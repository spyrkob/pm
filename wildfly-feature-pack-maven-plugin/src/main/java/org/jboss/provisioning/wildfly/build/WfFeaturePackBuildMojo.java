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
package org.jboss.provisioning.wildfly.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.plugin.FpMavenErrors;
import org.jboss.provisioning.plugin.util.MavenPluginUtil;
import org.jboss.provisioning.plugin.wildfly.BuildPropertyReplacer;
import org.jboss.provisioning.plugin.wildfly.config.WildFlyPostFeaturePackTasks;
import org.jboss.provisioning.plugin.wildfly.config.WildFlyPostFeaturePackTasksWriter20;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.spec.FeaturePackSpec.Builder;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PropertyUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.wildfly.build.ModuleParseResult.ModuleDependency;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;
import org.jboss.provisioning.xml.PackageXmlParser;
import org.jboss.provisioning.xml.PackageXmlWriter;

/**
 * This plug-in builds a WildFly feature-pack arranging the content by packages.
 * The artifact versions are resolved here. The configuration pieces are copied into
 * the feature-pack resources directory and will be assembled at the provisioning time.
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "wf-build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class WfFeaturePackBuildMojo extends AbstractMojo {

    private static final String MODULE_XML = "module.xml";

    private static final boolean OS_WINDOWS = PropertyUtils.isWindows();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * The configuration file used for feature pack.
     */
    @Parameter(alias = "config-file", defaultValue = "wildfly-feature-pack-build.xml", property = "wildfly.feature.pack.configFile")
    private String configFile;

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

    private MavenProjectArtifactVersions artifactVersions;

    private WildFlyFeaturePackBuild wfFpConfig;
    private Map<String, FeaturePackSpec> fpDependencies = Collections.emptyMap();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        } catch(RuntimeException | Error | MojoExecutionException | MojoFailureException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void doExecute() throws MojoExecutionException, MojoFailureException {
        artifactVersions = MavenProjectArtifactVersions.getInstance(project);

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
        final Path targetResources = Paths.get(buildName, Constants.RESOURCES);
        try {
            IoUtils.copy(Paths.get(configDir.getAbsolutePath() + resourcesDir), targetResources);
        } catch (IOException e1) {
            throw new MojoExecutionException(Errors.copyFile(Paths.get(configDir.getAbsolutePath()).resolve(resourcesDir), targetResources), e1);
        }

        final Path workDir = Paths.get(buildName, "layout");
        System.out.println("WfFeaturePackBuildMojo.execute " + workDir);
        IoUtils.recursiveDelete(workDir);
        final String fpArtifactId = project.getArtifactId() + "-new";
        final Path fpDir = workDir.resolve(project.getGroupId()).resolve(fpArtifactId).resolve(project.getVersion());
        final Path fpPackagesDir = fpDir.resolve(Constants.PACKAGES);

        // feature-pack builder
        final FeaturePackSpec.Builder fpBuilder = FeaturePackSpec.builder(ArtifactCoords.newGav(project.getGroupId(), fpArtifactId, project.getVersion()));

        // feature-pack build config
        try {
            wfFpConfig = Util.loadFeaturePackBuildConfig(getFPConfigFile(), getFPConfigProperties());
        } catch (ProvisioningException e) {
            throw new MojoExecutionException("Failed to load feature-pack config file", e);
        }

        try {
            processFeaturePackDependencies(fpBuilder);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to process dependencies", e);
        }
        copyArtifacts(targetResources);

        final Path srcModulesDir = targetResources.resolve("modules").resolve("system").resolve("layers").resolve("base");
        if(!Files.exists(srcModulesDir)) {
            throw new MojoExecutionException(Errors.pathDoesNotExist(srcModulesDir));
        }

        try {
            final Map<String, Path> moduleXmlByPkgName = findModules(srcModulesDir);
            if(moduleXmlByPkgName.isEmpty()) {
                throw new MojoExecutionException("Modules not found in " + srcModulesDir);
            }
            packageModules(fpBuilder, targetResources, moduleXmlByPkgName, fpPackagesDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process modules content", e);
        }

        try {
            packageContent(fpBuilder, targetResources.resolve(Constants.CONTENT), fpPackagesDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process content", e);
        }

        fpBuilder.addProvisioningPlugin(ArtifactCoords.newGav("org.jboss.pm", "wildfly-provisioning-plugin", "1.0.0.Alpha-SNAPSHOT").toArtifactCoords());

        addConfigPackages(targetResources.resolve("config").resolve("packages"), fpDir.resolve(Constants.PACKAGES), fpBuilder);

        for(String defaultPackage : wfFpConfig.getDefaultPackages()) {
            if(!fpBuilder.hasPackage(defaultPackage)) {
                throw new MojoExecutionException(Errors.unknownPackage(defaultPackage));
            }
            fpBuilder.markAsDefaultPackage(defaultPackage);
        }

        final FeaturePackSpec fpSpec;
        try {
            fpSpec = fpBuilder.build();
            FeaturePackXmlWriter.getInstance().write(fpSpec, fpDir.resolve(Constants.FEATURE_PACK_XML));
        } catch (XMLStreamException | IOException | ProvisioningDescriptionException e) {
            throw new MojoExecutionException(Errors.writeXml(fpDir.resolve(Constants.FEATURE_PACK_XML)), e);
        }

        // collect feature-pack resources
        final Path resourcesWildFly = fpDir.resolve(Constants.RESOURCES).resolve("wildfly");
        try {
            IoUtils.copy(targetResources.resolve("configuration"), resourcesWildFly.resolve("configuration"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy configuration files to feature-pack resources", e);
        }

        // properties
        try(OutputStream out = Files.newOutputStream(resourcesWildFly.resolve("wildfly-tasks.properties"))) {
                getFPConfigProperties().store(out, "WildFly feature-pack properties");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to store feature-pack properties", e);
        }

        // post feature-pack tasks config
        final WildFlyPostFeaturePackTasks tasks = WildFlyPostFeaturePackTasks.builder()
                .addFilePermissions(wfFpConfig.getFilePermissions())
                .addMkDirs(wfFpConfig.getMkDirs())
                .addUnixLineEndFilters(wfFpConfig.getUnixLineEndFilters())
                .addWindowsLineEndFilters(wfFpConfig.getWindowsLineEndFilters())
                .build();
        try {
            WildFlyPostFeaturePackTasksWriter20.INSTANCE.write(tasks, resourcesWildFly.resolve("wildfly-tasks.xml"));
        } catch (XMLStreamException | IOException e) {
            throw new MojoExecutionException(Errors.writeXml(resourcesWildFly.resolve("wildfly-tasks.xml")), e);
        }

        try {
            repoSystem.install(repoSession, MavenPluginUtil.getInstallLayoutRequest(workDir));
        } catch (InstallationException | IOException e) {
            throw new MojoExecutionException(FpMavenErrors.featurePackInstallation(), e);
        }
    }

    private void addConfigPackages(final Path configDir, final Path packagesDir, final Builder fpBuilder) throws MojoExecutionException {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(configDir)) {
            for(Path configPackage : stream) {
                final Path provisioningCli = configPackage.resolve("pm/wildfly/provisioning.cli");
                if(!Files.exists(provisioningCli)) {
                    throw new MojoExecutionException("Config package is missing provisioning.cli: " + provisioningCli);
                }
                final Path packageDir = packagesDir.resolve(configPackage.getFileName());
                if (!Files.exists(packageDir)) {
                    Files.createDirectories(packageDir);
                }
                IoUtils.copy(configPackage, packageDir);

                final Path packageXml = configPackage.resolve(Constants.PACKAGE_XML);
                if (Files.exists(packageXml)) {
                    final PackageSpec pkgSpec;
                    try (BufferedReader reader = Files.newBufferedReader(packageXml)) {
                        try {
                            pkgSpec = new PackageXmlParser().parse(reader);
                        } catch (XMLStreamException e) {
                            throw new MojoExecutionException("Failed to parse " + packageXml, e);
                        }
                    }
                    IoUtils.copy(packageXml, packageDir.resolve(Constants.PACKAGE_XML));
                    fpBuilder.addPackage(pkgSpec);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process config packages", e);
        }
    }

    private void copyArtifacts(final Path targetResources) throws MojoExecutionException {
        for(CopyArtifact copyArtifact : wfFpConfig.getCopyArtifacts()) {
            final String gavString = artifactVersions.getVersion(copyArtifact.getArtifact());
            try {
                final ArtifactCoords coords = ArtifactCoordsUtil.fromJBossModules(gavString, "jar");
                final Path jarSrc = resolveArtifact(coords);
                String location = copyArtifact.getToLocation();
                if (location.endsWith("/")) {
                    // if the to location ends with a / then it is a directory
                    // so we need to append the artifact name
                    location += jarSrc.getFileName();
                }

                Path jarTarget = targetResources;
                if(!location.startsWith("modules/")) {
                    jarTarget = jarTarget.resolve("content");
                }
                jarTarget = jarTarget.resolve(location);

                Files.createDirectories(jarTarget.getParent());
                if (copyArtifact.isExtract()) {
                    extractArtifact(jarSrc, jarTarget, copyArtifact);
                } else {
                    IoUtils.copy(jarSrc, jarTarget);
                }

                if(wfFpConfig.isPackageSchemas() && wfFpConfig.isSchemaGroup(coords.getGroupId())) {
                    extractSchemas(targetResources, jarSrc);
                }
            } catch (ProvisioningException | IOException e) {
                throw new MojoExecutionException("Failed to copy artifact " + gavString, e);
            }
        }
    }

    private void extractArtifact(Path artifact, Path target, CopyArtifact copy) throws IOException {
        try (FileSystem zipFS = FileSystems.newFileSystem(artifact, null)) {
            for(Path zipRoot : zipFS.getRootDirectories()) {
                Files.walkFileTree(zipRoot, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                                final String entry = dir.toString().substring(1);
                                if(entry.isEmpty()) {
                                    return FileVisitResult.CONTINUE;
                                }
                                if(!copy.includeFile(entry)) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                final Path targetDir = target.resolve(zipRoot.relativize(dir).toString());
                                try {
                                    Files.copy(dir, targetDir);
                                } catch (FileAlreadyExistsException e) {
                                     if (!Files.isDirectory(targetDir))
                                         throw e;
                                }
                                return FileVisitResult.CONTINUE;
                            }
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                                if(copy.includeFile(file.toString().substring(1))) {
                                    Files.copy(file, target.resolve(zipRoot.relativize(file).toString()));
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
            }
        }
    }

    private void processFeaturePackDependencies(final Builder fpBuilder)
            throws Exception {
        if (!wfFpConfig.getDependencies().isEmpty()) {
            fpDependencies = new HashMap<>(wfFpConfig.getDependencies().size());
            for (FeaturePackDependencySpec depSpec : wfFpConfig.getDependencies()) {
                FeaturePackConfig depConfig = depSpec.getTarget();
                final String depStr = depConfig.getGav().toString();
                String gavStr = artifactVersions.getVersion(depStr);
                if(gavStr == null) {
                    throw new MojoExecutionException("Failed resolve artifact version for " + depStr);
                }
                gavStr = gavStr.replace(depStr, depStr + "-new");
                final ArtifactCoords.Gav depGav = ArtifactCoords.newGav(gavStr);
                final FeaturePackConfig.Builder depBuilder = FeaturePackConfig.builder(depGav);
                if (depConfig.hasExcludedPackages()) {
                    try {
                        depBuilder.excludeAllPackages(depConfig.getExcludedPackages()).build();
                    } catch (ProvisioningException e) {
                        throw new MojoExecutionException("Failed to process dependencies", e);
                    }
                }
                fpBuilder.addDependency(depSpec.getName(), depBuilder.build());
                final Path depZip = resolveArtifact(depGav.toArtifactCoords());
                fpDependencies.put(depSpec.getName(), FeaturePackLayoutDescriber.describeFeaturePackZip(depZip));
            }
        }
    }

    private void packageContent(FeaturePackSpec.Builder fpBuilder, Path contentDir, Path packagesDir) throws IOException, MojoExecutionException {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(contentDir)) {
            for(Path p : stream) {
                final String pkgName = p.getFileName().toString();
                if(pkgName.equals("docs")) {
                    final PackageSpec.Builder docsBuilder = PackageSpec.builder(pkgName);
                    try(DirectoryStream<Path> docsStream = Files.newDirectoryStream(p)) {
                        for(Path docPath : docsStream) {
                            final String docName = docPath.getFileName().toString();
                            final Path docDir = packagesDir.resolve(docName);
                            IoUtils.copy(docPath, docDir.resolve(Constants.CONTENT).resolve("docs").resolve(docName));
                            final PackageSpec docSpec = PackageSpec.builder(docName).build();
                            fpBuilder.addPackage(docSpec);
                            writeXml(docSpec, docDir);
                            docsBuilder.addDependency(docName, true);
                        }
                    }
                    PackageSpec docsSpec = docsBuilder.build();
                    writeXml(docsSpec, packagesDir.resolve(pkgName));
                    fpBuilder.addPackage(docsSpec);
                } else {
                    final Path pkgDir = packagesDir.resolve(pkgName);
                    IoUtils.copy(p, pkgDir.resolve(Constants.CONTENT).resolve(pkgName));
                    final PackageSpec pkgSpec = PackageSpec.builder(pkgName).build();
                    writeXml(pkgSpec, pkgDir);
                    fpBuilder.addPackage(pkgSpec);
                }
            }
        }
    }

    private Map<String, Path> findModules(Path modulesDir) throws IOException {
        final Map<String, Path> moduleXmlByPkgName = new HashMap<>();
        Files.walkFileTree(modulesDir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!file.getFileName().toString().equals(MODULE_XML)) {
                    return FileVisitResult.CONTINUE;
                }
                final String packageName = modulesDir.relativize(file.getParent()).toString().replace(File.separatorChar, '.');
                moduleXmlByPkgName.put(packageName, file);
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
        return moduleXmlByPkgName;
    }

    private void packageModules(FeaturePackSpec.Builder fpBuilder,
            Path resourcesDir, Map<String, Path> moduleXmlByPkgName, Path packagesDir)
            throws IOException, MojoExecutionException {
        final BuildPropertyReplacer buildPropertyReplacer = new BuildPropertyReplacer(
                new ModuleArtifactPropertyResolver(artifactVersions));

        for (Map.Entry<String, Path> module : moduleXmlByPkgName.entrySet()) {
            final String packageName = module.getKey();
            final Path moduleXml = module.getValue();

            final Path packageDir = packagesDir.resolve(packageName);
            final Path targetXml = packageDir.resolve(Constants.CONTENT).resolve(resourcesDir.relativize(moduleXml));
            Files.createDirectories(targetXml.getParent());

            IoUtils.copy(moduleXml.getParent(), targetXml.getParent());

            final PackageSpec.Builder pkgSpecBuilder = PackageSpec.builder(packageName);
            final ModuleParseResult parsedModule;
            try {
                parsedModule = ModuleXmlParser.parse(targetXml, "UTF-8");
                if (!parsedModule.dependencies.isEmpty()) {
                    for (ModuleDependency moduleDep : parsedModule.dependencies) {
                        final StringBuilder buf = new StringBuilder();
                        buf.append(moduleDep.getModuleId().getName()).append('.').append(moduleDep.getModuleId().getSlot());
                        final String depName = buf.toString();
                        if (moduleXmlByPkgName.containsKey(depName)) {
                            pkgSpecBuilder.addDependency(depName, moduleDep.isOptional());
                        } else {
                            Map.Entry<String, FeaturePackSpec> depSrc = null;
                            if (!fpDependencies.isEmpty()) {
                                for (Map.Entry<String, FeaturePackSpec> depEntry : fpDependencies.entrySet()) {
                                    if (depEntry.getValue().getPackageNames().contains(depName)) {
                                        if (depSrc != null) {
                                            throw new MojoExecutionException("Package " + depName
                                                    + " found in more than one feature-pack dependency: " + depSrc.getKey()
                                                    + " and " + depEntry.getKey());
                                        }
                                        depSrc = depEntry;
                                    }
                                }
                            }
                            if(depSrc != null) {
                                pkgSpecBuilder.addDependency(depSrc.getKey(), depName, moduleDep.isOptional());
                            } else if(moduleDep.isOptional()){
                                //System.out.println("UNSATISFIED EXTERNAL OPTIONAL DEPENDENCY " + packageName + " -> " + depName);
                            } else {
                                throw new MojoExecutionException("Package " + packageName + " has unsatisifed external dependency on package " + depName);
                            }
                        }
                    }
                }
            } catch (XMLStreamException e) {
                throw new IOException(Errors.parseXml(targetXml), e);
            }

            final PackageSpec pkgSpec = pkgSpecBuilder.build();
            try {
                PackageXmlWriter.getInstance().write(pkgSpec, packageDir.resolve(Constants.PACKAGE_XML));
            } catch (XMLStreamException e) {
                throw new IOException(Errors.writeXml(packageDir.resolve(Constants.PACKAGE_XML)), e);
            }
            fpBuilder.addPackage(pkgSpec);

            final String moduleXmlContents = IoUtils.readFile(moduleXml);
            String targetContent;
            try {
                targetContent = buildPropertyReplacer.replaceProperties(moduleXmlContents);
            } catch (Throwable t) {
                throw new IOException("Failed to replace properties for " + moduleXml, t);
            }
            IoUtils.writeFile(targetXml, targetContent);

            // extract schemas
            if (wfFpConfig.isPackageSchemas()) {
                Util.processModuleArtifacts(parsedModule, coords -> {
                    if (wfFpConfig.isSchemaGroup(coords.getGroupId())) {
                        final Path artifactFile;
                        try {
                            artifactFile = resolveArtifact(coords);
                        } catch (ProvisioningException e) {
                            throw new IOException(FpMavenErrors.artifactResolution(coords), e);
                        }
                        extractSchemas(resourcesDir, artifactFile);
                    }
                });
            }

            if (!OS_WINDOWS) {
                Files.setPosixFilePermissions(targetXml, Files.getPosixFilePermissions(moduleXml));
            }
        }
    }

    private void extractSchemas(Path resourcesDir, final Path artifactFile) throws IOException {
        final FileSystem jarFS = FileSystems.newFileSystem(artifactFile, null);
        final Path schemaSrc = jarFS.getPath("schema");
        if (Files.exists(schemaSrc)) {
            final Path targetSchemaDir = resourcesDir.resolve("content").resolve("docs").resolve("schema");
            if(!Files.exists(targetSchemaDir)) {
                Files.createDirectories(targetSchemaDir);
            }
            ZipUtils.copyFromZip(schemaSrc.toAbsolutePath(), targetSchemaDir);
        }
    }

    private Properties getFPConfigProperties() {
        final Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.put("project.version", project.getVersion()); //TODO: figure out the correct way to do this
        return properties;
    }

    private Path getFPConfigFile() throws ProvisioningException {
        final Path path = Paths.get(configDir.getAbsolutePath(), configFile);
        if(!Files.exists(path)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(path));
        }
        return path;
    }

    private static void writeXml(PackageSpec pkgSpec, Path dir) throws MojoExecutionException {
        try {
            Files.createDirectories(dir);
            PackageXmlWriter.getInstance().write(pkgSpec, dir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new MojoExecutionException(Errors.writeXml(dir.resolve(Constants.PACKAGE_XML)), e);
        }
    }

    private Path resolveArtifact(ArtifactCoords coords) throws ProvisioningException {
        final ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, getArtifactRequest(coords));
        } catch (ArtifactResolutionException e) {
            throw new ProvisioningException(FpMavenErrors.artifactResolution(coords), e);
        }
        if(!result.isResolved()) {
            throw new ProvisioningException(FpMavenErrors.artifactResolution(coords));
        }
        if(result.isMissing()) {
            throw new ProvisioningException(FpMavenErrors.artifactMissing(coords));
        }
        return Paths.get(result.getArtifact().getFile().toURI());
    }

    private ArtifactRequest getArtifactRequest(ArtifactCoords coords) {
        final ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getExtension(), coords.getVersion()));
        req.setRepositories(remoteRepos);
        return req;
    }
}
