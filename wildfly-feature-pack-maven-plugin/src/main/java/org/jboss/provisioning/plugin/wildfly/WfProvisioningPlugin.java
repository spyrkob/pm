/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.provisioning.plugin.wildfly;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.plugin.FpMavenErrors;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.plugin.wildfly.configassembly.ConfigurationAssembler;
import org.jboss.provisioning.plugin.wildfly.configassembly.InputStreamSource;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemConfig;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemsParser;
import org.jboss.provisioning.plugin.wildfly.configassembly.ZipFileSubsystemInputStreamSources;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.ConfigFileDescription;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.FilePermission;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.WildFlyPostFeaturePackTasks;
import org.jboss.provisioning.util.PropertyUtils;
import org.jboss.provisioning.xml.PackageXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfProvisioningPlugin implements ProvisioningPlugin {

    private final ZipFileSubsystemInputStreamSources subsystemsInput = new ZipFileSubsystemInputStreamSources();

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void execute(ProvisioningContext ctx) throws ProvisioningException {

        System.out.println("WildFly configuration assembling plugin for " + ctx.getInstallDir());

        final Path resources = ctx.getResourcesDir().resolve("wildfly");
        if(!Files.exists(resources)) {
            return;
        }

        final Properties props = new Properties();
        try(InputStream in = Files.newInputStream(resources.resolve("wildfly-tasks.properties"))) {
            props.load(in);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.readFile(resources.resolve("wildfly-feature-pack-build.properties")), e);
        }

        final Path wfTasksXml = resources.resolve("wildfly-tasks.xml");
        if(!Files.exists(wfTasksXml)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(wfTasksXml));
        }
        final WildFlyPostFeaturePackTasks tasks = Util.loadWildFlyTasks(wfTasksXml, props);

        collectLayoutSubsystemsInput(ctx);
        assembleConfigs(resources, tasks, ctx.getInstallDir());

        if (!PropertyUtils.isWindows()) {
            processFeaturePackFilePermissions(tasks, ctx.getInstallDir());
        }

        mkdirs(tasks, ctx.getInstallDir());
    }

    private static void mkdirs(final WildFlyPostFeaturePackTasks tasks, Path installDir) throws ProvisioningException {
        // make dirs
        for (String dirName : tasks.getMkDirs()) {
            final Path dir = installDir.resolve(dirName);
            if(!Files.isDirectory(dir)) {
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.mkdirs(dir));
                }
            }
        }
    }

    private void processFeaturePackFilePermissions(WildFlyPostFeaturePackTasks tasks, Path installDir) throws ProvisioningException {
        final List<FilePermission> filePermissions = tasks.getFilePermissions();
        try {
            Files.walkFileTree(installDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    final String relative = installDir.relativize(dir).toString();
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(dir, perm.getPermission());
                            continue;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final String relative = installDir.relativize(file).toString();
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(file, perm.getPermission());
                            continue;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ProvisioningException("Failed to set file permissions", e);
        }
//        if(!excludeDependencies) {
//            for (FeaturePack dependency : featurePack.getDependencies()) {
//                processFeaturePackFilePermissions(dependency, outputDirectory, excludeDependencies);
//            }
//        }
    }

    private void collectLayoutSubsystemsInput(ProvisioningContext ctx) throws ProvisioningException {
        try(DirectoryStream<Path> groupDtream = Files.newDirectoryStream(ctx.getLayoutDir())) {
            for(Path groupId : groupDtream) {
                try(DirectoryStream<Path> artifactStream = Files.newDirectoryStream(groupId)) {
                    for(Path artifactId : artifactStream) {
                        try(DirectoryStream<Path> versionStream = Files.newDirectoryStream(artifactId)) {
                            int count = 0;
                            for(Path version : versionStream) {
                                if(++count > 1) {
                                    throw new ProvisioningException("There is more than one version of feature-pack " +
                                            ArtifactCoords.getGaPart(groupId.getFileName().toString(), artifactId.getFileName().toString()));
                                }
                                collectFeaturePackSubsystemsInput(ctx, version);
                            }
                        } catch (IOException e) {
                            throw new ProvisioningException(Errors.readDirectory(artifactId), e);
                        }
                    }
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readDirectory(groupId), e);
                }
            }
        } catch (IOException e) {
            throw new ProvisioningException(Errors.readDirectory(ctx.getLayoutDir()), e);
        }
    }

    private void collectFeaturePackSubsystemsInput(final ProvisioningContext ctx, Path fpDir) throws ProvisioningException {
        final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
        final Path modulesPackageXml = packagesDir.resolve("modules").resolve(Constants.PACKAGE_XML);
        if (!Files.exists(modulesPackageXml)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(modulesPackageXml));
        }

        final PackageDescription modulesDescr;
        try (Reader reader = Files.newBufferedReader(modulesPackageXml)) {
            modulesDescr = new PackageXmlParser().parse(reader);
        } catch (XMLStreamException | IOException e) {
            throw new ProvisioningException(Errors.parseXml(modulesPackageXml), e);
        }

        for (String modulePkg : modulesDescr.getDependencies()) {
            final Path moduleDir = packagesDir.resolve(modulePkg).resolve(Constants.CONTENT);
            if (!Files.exists(moduleDir)) {
                throw new ProvisioningException(Errors.pathDoesNotExist(moduleDir));
            }
            try {
                Files.walkFileTree(moduleDir, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
                        if (!file.getFileName().toString().equals("module.xml")) {
                            return FileVisitResult.CONTINUE;
                        }
                        Util.processModuleArtifacts(file, (coords) -> {
                            final Path artifactFile;
                            try {
                                artifactFile = ctx.resolveArtifact(coords);
                            } catch(ProvisioningException e) {
                                throw new IOException(FpMavenErrors.artifactResolution(coords), e);
                            }

                            final FileSystem jarFS = FileSystems.newFileSystem(artifactFile, null);
                            final Path subsystemTemplates = jarFS.getPath("subsystem-templates");
                            if (Files.exists(subsystemTemplates)) {
                                try (DirectoryStream<Path> stream = Files.newDirectoryStream(subsystemTemplates)) {
                                    for (Path path : stream) {
                                        subsystemsInput.addSubsystemFileSource(path.getFileName().toString(),
                                                artifactFile.toFile(), new ZipEntry(path.toString().substring(1)));
                                    }
                                }
                            }
                        });
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
                throw new ProvisioningException(Errors.readDirectory(moduleDir), e);
            }
        }
    }

    private void assembleConfigs(final Path resources, WildFlyPostFeaturePackTasks tasks, Path installDir) throws ProvisioningException {
        assembleConfigs(resources, "domain", tasks.getConfig().getDomainConfigFiles(), installDir);
        assembleConfigs(resources, "server", tasks.getConfig().getStandaloneConfigFiles(), installDir);
        assembleConfigs(resources, "host", tasks.getConfig().getHostConfigFiles(), installDir);
    }

    private void assembleConfigs(final Path resources, String rootElement, List<ConfigFileDescription> configFiles, Path installDir)
            throws ProvisioningException {
        for (ConfigFileDescription provisioningConfigFile : configFiles) {
            final Path template = resources.resolve(provisioningConfigFile.getTemplate());
            if(!Files.exists(template)) {
                continue;
            }
            final Path subsystems = resources.resolve(provisioningConfigFile.getSubsystems());
            if(!Files.exists(subsystems)) {
                throw new ProvisioningException(Errors.pathDoesNotExist(subsystems));
            }
            final Path output = installDir.resolve(provisioningConfigFile.getOutputFile());
            if(Files.exists(output)) {
                throw new ProvisioningException(Errors.pathAlreadyExists(output));
            }
            try {
                new ConfigurationAssembler(subsystemsInput, getInputStreamSource(template), rootElement,
                        parseSubsystems(subsystems, provisioningConfigFile.getProperties()), output.toFile()).assemble();
            } catch (IOException | XMLStreamException e) {
                throw new ProvisioningException("Failed to assemble config file " + output + " using template " + template, e);
            }
            if(!Files.exists(output)) {
                throw new ProvisioningException(Errors.pathDoesNotExist(output));
            }
        }
    }

    private static Map<String, Map<String, SubsystemConfig>> parseSubsystems(Path subsystemsXml, Map<String, String> props) throws ProvisioningException {
        final Map<String, Map<String, SubsystemConfig>> subsystemConfigs = new LinkedHashMap<>();
        try {
            SubsystemsParser.parse(new InputStreamSource() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return Files.newInputStream(subsystemsXml);
                }
            }, props, subsystemConfigs);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(subsystemsXml), e);
        }
        return subsystemConfigs;
    }

    private static InputStreamSource getInputStreamSource(Path p) {
        return new InputStreamSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(p);
            }
        };
    }
}
