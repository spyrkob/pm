/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provisioning.plugin.wildfly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.plugin.FPMavenErrors;
import org.jboss.provisioning.plugin.wildfly.configassembly.ConfigurationAssembler;
import org.jboss.provisioning.plugin.wildfly.configassembly.FileInputStreamSource;
import org.jboss.provisioning.plugin.wildfly.configassembly.InputStreamSource;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemConfig;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemInputStreamSources;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemsParser;
import org.jboss.provisioning.plugin.wildfly.configassembly.ZipFileSubsystemInputStreamSources;
import org.jboss.provisioning.util.plugin.ProvisioningContext;
import org.jboss.provisioning.util.plugin.ProvisioningPlugin;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFProvisioningPlugin implements ProvisioningPlugin {

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void execute(ProvisioningContext ctx) throws ProvisioningException {

        final Path templatesDir = ctx.getResourcesDir().resolve("wildfly").resolve("configuration");
        if(!Files.exists(templatesDir)) {
            System.out.println("Configuration templates not found.");
            return;
        }

        System.out.println("Assembling configuration for " + ctx.getInstallDir());

        final SubsystemInputStreamSources subsystemsInput = getSubsystemSources(ctx);

        try(DirectoryStream<Path> stream = Files.newDirectoryStream(templatesDir, p -> Files.isDirectory(p))) {
            for(Path p : stream) {
                processConfigTemplate(p, subsystemsInput, ctx.getInstallDir());
            }
        } catch (IOException e) {
            throw new ProvisioningException(Errors.readDirectory(templatesDir));
        }
    }

    private void processConfigTemplate(Path baseDir, SubsystemInputStreamSources subsystemsInput, Path installDir) throws ProvisioningException {
        System.out.println("Processing " + baseDir.getFileName() + " templates");

        final Path subsystemsXml = baseDir.resolve("subsystems.xml");
        if(!Files.exists(subsystemsXml)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(subsystemsXml));
        }

        final Map<String, Map<String, SubsystemConfig>> subsystemConfigs = new LinkedHashMap<>();
        try {
            SubsystemsParser.parse(new InputStreamSource() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return Files.newInputStream(subsystemsXml);
                }
            }, new BuildPropertyReplacer(PropertyResolver.NO_OP), subsystemConfigs);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(subsystemsXml), e);
        }

        final String templateRoot;
        final Path configDir;
        if(baseDir.getFileName().toString().equals("standalone")) {
            configDir = installDir.resolve("standalone").resolve("configuration");
            templateRoot = "server";
        } else {
            configDir = installDir.resolve("domain").resolve("configuration");
            templateRoot = baseDir.getFileName().toString();
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, p -> !p.getFileName().toString().equals("subsystems.xml"))) {
            for(Path p : stream) {
                final Path outputFile = p.getFileName().toString().equals("template.xml") ? configDir.resolve(baseDir.getFileName().toString() + ".xml") :
                    configDir.resolve(p.getFileName().toString());
                System.out.println("  assembling " + outputFile + " using template " + p.getFileName());
                try {
                    new ConfigurationAssembler(subsystemsInput, new FileInputStreamSource(p.toFile()), templateRoot, subsystemConfigs, outputFile.toFile()).assemble();
                } catch (XMLStreamException | RuntimeException | Error t) {
                    throw new ProvisioningException("Failed to assemble configuration", t);
                }
            }
        } catch (IOException e) {
            throw new ProvisioningException(Errors.readDirectory(baseDir));
        }
    }

    private SubsystemInputStreamSources getSubsystemSources(ProvisioningContext ctx) throws ProvisioningException {
        final ZipFileSubsystemInputStreamSources subsystemsInput = new ZipFileSubsystemInputStreamSources();
        final Path layoutDir = ctx.getLayoutDir();
        for(FeaturePackDescription fpDescr : ctx.getInstallationDescription().getFeaturePacks()) {
            final PackageDescription modulesDescr = fpDescr.getPackageDescription("modules");
            if(modulesDescr == null) {
                continue;
            }

            final GAV fpGav = fpDescr.getGAV();
            final Path fpDir = layoutDir.resolve(fpGav.getGroupId()).resolve(fpGav.getArtifactId()).resolve(fpGav.getVersion());

            final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
            for(String pkgName : modulesDescr.getDependencies()) {
                final Path pkgContent = packagesDir.resolve(pkgName).resolve(Constants.CONTENT);
                if(!Files.exists(pkgContent)) {
                    continue;
                }
                try {
                    Files.walkFileTree(pkgContent, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if(!file.getFileName().toString().equals("module.xml")) {
                                return FileVisitResult.CONTINUE;
                            }

                            try {
                                final ModuleParseResult parsedModule = ModuleXmlParser.parse(file, ctx.getEncoding());
                                for(ModuleParseResult.ArtifactName artName : parsedModule.artifacts) {
                                    final Path artifactPath;
                                    try {
                                        artifactPath = ctx.resolveArtifact(GAV.fromString(artName.getArtifactCoords()), "jar");
                                    } catch(ProvisioningException e) {
                                        throw new IOException(FPMavenErrors.artifactResolution(GAV.fromString(artName.getArtifactCoords())));
                                    }
                                    final FileSystem jarFS = FileSystems.newFileSystem(artifactPath, null);
                                    final Path subsystemTemplates = jarFS.getPath("subsystem-templates");
                                    if(Files.exists(subsystemTemplates)) {
                                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(subsystemTemplates)) {
                                            for(Path path : stream) {
                                                subsystemsInput.addSubsystemFileSource(path.getFileName().toString(),
                                                        artifactPath.toFile(), new ZipEntry(path.toString().substring(1)));
                                            }
                                        }
                                    }
                                }
                            } catch (XMLStreamException e) {
                                throw new IOException(Errors.parseXml(file));
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.TERMINATE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to process package " + pkgName + " of " + fpGav, e);
                }
            }
        }
        return subsystemsInput;
    }
}
