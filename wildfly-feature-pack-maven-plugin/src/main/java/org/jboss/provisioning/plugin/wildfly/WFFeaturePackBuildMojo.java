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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallationException;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackDescription.Builder;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.plugin.FPMavenErrors;
import org.jboss.provisioning.plugin.util.MavenPluginUtil;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PropertyUtils;
import org.jboss.provisioning.xml.FeaturePackXMLWriter;
import org.jboss.provisioning.xml.PackageXMLWriter;

/**
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "wf-build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class WFFeaturePackBuildMojo extends AbstractMojo {

    private static final boolean OS_WINDOWS = PropertyUtils.isWindows();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The directory the configuration file is located in.
     */
    @Parameter(alias = "config-dir", defaultValue = "${basedir}", property = "wildfly.feature.pack.configDir")
    private File configDir;

    /**
     * A path relative to {@link #configDir} that represents the directory under which of resources such as
     * {@code configuration/standalone/subsystems.xml}, {modules}, {subsystem-templates}, etc.
     */
    @Parameter(alias = "resources-dir", defaultValue = "src/main/resources", property = "wildfly.feature.pack.resourcesDir", required = true)
    private String resourcesDir;

    /**
     * The name of the server.
     */
    @Parameter(alias = "server-name", defaultValue = "${project.build.finalName}", property = "wildfly.feature.pack.serverName")
    private String serverName;

    /**
     * The directory for the built artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "wildfly.feature.pack.buildName")
    private String buildName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        /* normalize resourcesDir */
        if (!resourcesDir.isEmpty()) {
            switch (resourcesDir.charAt(0)) {
            case '/':
            case '\\':
                break;
            default:
                resourcesDir = "/" + resourcesDir;
                break;
            }
        }

        final Path workDir = Paths.get(buildName, "layout");
        IoUtils.recursiveDelete(workDir);
        //final Path fpDir = workDir.resolve(project.getGroupId()).resolve(project.getArtifactId()).resolve(project.getVersion());
        final Path fpDir = workDir.resolve(project.getGroupId()).resolve(project.getArtifactId() + "-new").resolve(project.getVersion());
        final Path fpPackagesDir = fpDir.resolve(Constants.PACKAGES);

        final Path resourcesPath = Paths.get(configDir.getAbsolutePath() + resourcesDir);
        final Path srcModulesDir = resourcesPath.resolve("modules").resolve("system").resolve("layers").resolve("base");
        if(!Files.exists(srcModulesDir)) {
            throw new MojoExecutionException(Errors.pathDoesNotExist(srcModulesDir));
        }

        final Builder fpBuilder = FeaturePackDescription.builder(new GAV(project.getGroupId(), project.getArtifactId() + "-new", project.getVersion()));
        try {
            processContent(fpBuilder, resourcesPath.resolve(Constants.CONTENT), fpPackagesDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process content", e);
        }

        final PackageDescription.Builder modulesBuilder = PackageDescription.builder("modules");

        try {
            processModules(fpBuilder, modulesBuilder, resourcesPath, srcModulesDir, fpPackagesDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process modules content", e);
        }

        final PackageDescription modulesPkg = modulesBuilder.build();
        writeXml(modulesPkg, fpDir.resolve(Constants.PACKAGES).resolve(modulesPkg.getName()));

        try {
            processConfiguration(resourcesPath.resolve("configuration"), fpDir.resolve("resources"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy configuration", e);
        }

        fpBuilder.addProvisioningPlugin(new GAV("org.jboss.pm", "wildfly-feature-pack-maven-plugin", "1.0.0.Alpha-SNAPSHOT"));

        final FeaturePackDescription fpDescr = fpBuilder.addTopPackage(modulesPkg).build();
        try {
            FeaturePackXMLWriter.INSTANCE.write(fpDescr, fpDir.resolve(Constants.FEATURE_PACK_XML));
        } catch (XMLStreamException | IOException e) {
            throw new MojoExecutionException(Errors.writeXml(fpDir.resolve(Constants.FEATURE_PACK_XML)));
        }

        try {
            repoSystem.install(repoSession, MavenPluginUtil.getInstallLayoutRequest(workDir));
        } catch (InstallationException e) {
            throw new MojoExecutionException(FPMavenErrors.featurePackInstallation(), e);
        }
    }

    private void processConfiguration(Path configurationDir, Path resourcesDir) throws IOException {
        IoUtils.copy(configurationDir, resourcesDir.resolve("wildfly").resolve("configuration"));
    }

    private void processContent(FeaturePackDescription.Builder fpBuilder, Path contentDir, Path packagesDir) throws IOException, MojoExecutionException {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(contentDir)) {
            for(Path p : stream) {
                final String pkgName = p.getFileName().toString();
                final Path pkgDir = packagesDir.resolve(pkgName);
                IoUtils.copy(p, pkgDir.resolve(Constants.CONTENT).resolve(p.getFileName()));
                final PackageDescription pkgDescr = PackageDescription.builder(pkgName).build();
                writeXml(pkgDescr, pkgDir);
                fpBuilder.addTopPackage(pkgDescr);
            }
        }
    }

    private void processModules(FeaturePackDescription.Builder fpBuilder, PackageDescription.Builder modulesBuilder,
            Path resourcesDir, Path modulesDir, Path packagesDir) throws IOException {
        Files.walkFileTree(modulesDir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
/*                String relative = modulesDir.relativize(dir).toString();
                boolean include = true;
                if (include) {
                    Path rel = target.resolve(relative);
                    if (!Files.isDirectory(rel)) {
                        if (!rel.toFile().mkdirs()) {
                            throw new IOException("Could not create directory " + rel.toString());
                        }
                    }*/
                    return FileVisitResult.CONTINUE;
//                }
//                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!file.getFileName().toString().equals("module.xml")) {
                    return FileVisitResult.CONTINUE;
                }
                final String packageName = modulesDir.relativize(file.getParent()).toString().replace(File.separatorChar, '.');
                final Path packageDir = packagesDir.resolve(packageName);
                final Path targetXml = packageDir.resolve(Constants.CONTENT).resolve(resourcesDir.relativize(file));
                Files.createDirectories(targetXml.getParent());

                final PackageDescription pkgDescr = PackageDescription.builder(packageName).build();
                try {
                    PackageXMLWriter.INSTANCE.write(pkgDescr, packageDir.resolve(Constants.PACKAGE_XML));
                } catch (XMLStreamException e) {
                    throw new IOException(Errors.writeXml(packageDir.resolve(Constants.PACKAGE_XML)), e);
                }
                modulesBuilder.addDependency(packageName);
                fpBuilder.addPackage(pkgDescr);


                IoUtils.copy(file, targetXml);
                if (!OS_WINDOWS) {
                    Files.setPosixFilePermissions(targetXml, Files.getPosixFilePermissions(file));
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
    }

    private static void writeXml(PackageDescription pkgDescr, Path dir) throws MojoExecutionException {
        try {
            Files.createDirectories(dir);
            PackageXMLWriter.INSTANCE.write(pkgDescr, dir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new MojoExecutionException(Errors.writeXml(dir.resolve(Constants.PACKAGE_XML)));
        }
    }
}
