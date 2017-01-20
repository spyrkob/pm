/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jboss.as.cli.CommandLineException;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.plugin.wildfly.config.FilePermission;
import org.jboss.provisioning.plugin.wildfly.config.WildFlyPostFeaturePackTasks;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfProvisioningPlugin implements ProvisioningPlugin {

    private ProvisioningContext ctx;
    private PropertyResolver versionResolver;
    private BuildPropertyHandler propertyHandler;
    private List<Path> standaloneCliList = Collections.emptyList();
    private List<Path> domainCliList = Collections.emptyList();
    private List<Path> hostCliList = Collections.emptyList();

    private boolean thinServer;

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void postInstall(ProvisioningContext ctx) throws ProvisioningException {

        System.out.println("WildFly-based configuration assembling plug-in");

        final String thinServerProp = System.getProperty("wfThinServer");
        if(thinServerProp != null) {
            if(thinServerProp.isEmpty()) {
                thinServer = true;
            } else {
                thinServer = Boolean.parseBoolean(thinServerProp);
            }
        }

        this.ctx = ctx;
        Properties tasksProps = null;
        final Map<String, String> artifactVersions = new HashMap<>();
        for(ArtifactCoords.Gav fpGav : ctx.getProvisionedState().getFeaturePackGavs()) {
            final Path wfRes = LayoutUtils.getFeaturePackDir(ctx.getLayoutDir(), fpGav).resolve(Constants.RESOURCES).resolve(WfConstants.WILDFLY);
            if(!Files.exists(wfRes)) {
                continue;
            }

            final Path artifactProps = wfRes.resolve(WfConstants.ARTIFACT_VERSIONS_PROPS);
            if(Files.exists(artifactProps)) {
                try (Stream<String> lines = Files.lines(artifactProps)) {
                    final Iterator<String> iterator = lines.iterator();
                    while (iterator.hasNext()) {
                        final String line = iterator.next();
                        final int i = line.indexOf('=');
                        if (i < 0) {
                            throw new ProvisioningException("Failed to locate '=' character in " + line);
                        }
                        artifactVersions.put(line.substring(0, i), line.substring(i + 1));
                    }
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readFile(artifactProps), e);
                }
            }

            final Path tasksPropsPath = wfRes.resolve(WfConstants.WILDFLY_TASKS_PROPS);
            if(Files.exists(tasksPropsPath)) {
                tasksProps = tasksProps == null ? new Properties() : new Properties(tasksProps);
                try(InputStream in = Files.newInputStream(tasksPropsPath)) {
                    tasksProps.load(in);
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readFile(tasksPropsPath), e);
                }
            }
        }
        versionResolver = new MapPropertyResolver(artifactVersions);
        propertyHandler = new BuildPropertyHandler(versionResolver);

        processPackages();

        if(!standaloneCliList.isEmpty()) {
            final ConfigGenerator configGen = ConfigGenerator.newStandaloneGenerator(ctx.getInstallDir());
            for(Path p : standaloneCliList) {
                try {
                    configGen.addCommandLines(p);
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to read " + p);
                }
            }
            try {
                System.out.println("Generating standalone configuration.");
                configGen.generate();
            } catch (CommandLineException e) {
                throw new ProvisioningException("Failed to generate configuration", e);
            }
        }

        if(!domainCliList.isEmpty() || !hostCliList.isEmpty()) {
            final ConfigGenerator configGen = ConfigGenerator.newDomainGenerator(ctx.getInstallDir());
            for(Path p : domainCliList) {
                try {
                    configGen.addCommandLines(p);
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to read " + p);
                }
            }
            for(Path p : hostCliList) {
                try {
                    configGen.addCommandLines(p);
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to read " + p);
                }
            }
            try {
                System.out.println("Generating domain configuration.");
                configGen.generate();
            } catch (CommandLineException e) {
                throw new ProvisioningException("Failed to generate configuration", e);
            }
        }

        final Path resources = ctx.getResourcesDir().resolve(WfConstants.WILDFLY);
        if(!Files.exists(resources)) {
            return;
        }

        // TODO merge the tasks
        final Path wfTasksXml = resources.resolve(WfConstants.WILDFLY_TASKS_XML);
        if(!Files.exists(wfTasksXml)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(wfTasksXml));
        }
        final WildFlyPostFeaturePackTasks tasks = WildFlyPostFeaturePackTasks.load(wfTasksXml, tasksProps);
        if (!PropertyUtils.isWindows()) {
            processFeaturePackFilePermissions(tasks, ctx.getInstallDir());
        }
        mkdirs(tasks, ctx.getInstallDir());
    }

    private static void mkdirs(final WildFlyPostFeaturePackTasks tasks, Path installDir) throws ProvisioningException {
        // make dirs
        for (String dirName : tasks.getMkDirs()) {
            final Path dir = installDir.resolve(dirName);
            if(!Files.exists(dir)) {
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
    }

    private void processPackages() throws ProvisioningException {
        try(DirectoryStream<Path> groupDtream = Files.newDirectoryStream(ctx.getLayoutDir())) {
            for(Path groupId : groupDtream) {
                try(DirectoryStream<Path> artifactStream = Files.newDirectoryStream(groupId)) {
                    for(Path artifactId : artifactStream) {
                        try(DirectoryStream<Path> versionStream = Files.newDirectoryStream(artifactId)) {
                            int count = 0;
                            for(Path version : versionStream) {
                                final ArtifactCoords.Gav fpGav = ArtifactCoords.newGav(groupId.getFileName().toString(), artifactId.getFileName().toString(), version.getFileName().toString());
                                if(++count > 1) {
                                    throw new ProvisioningException("There is more than one version of feature-pack " + fpGav.toGa());
                                }
                                processPackages(ctx.getProvisionedState().getFeaturePack(fpGav), version);
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

    private void processPackages(final ProvisionedFeaturePack provisionedFp, Path fpDir) throws ProvisioningException {
        final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
        if(!Files.exists(packagesDir)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(packagesDir));
        }
        noScriptsLogged = true;
        for(String pkgName : provisionedFp.getPackageNames()) {
            final Path pmWfDir = packagesDir.resolve(pkgName).resolve("pm/wildfly");
            if(!Files.exists(pmWfDir)) {
                continue;
            }
            final Path moduleDir = pmWfDir.resolve(WfConstants.MODULE);
            if(Files.exists(moduleDir)) {
                processModules(provisionedFp.getGav(), pkgName, moduleDir);
            }

            // collect cli scripts
            Path provisioningCli = pmWfDir.resolve("provisioning.cli");
            if (Files.exists(provisioningCli)) {
                logPackageScript(provisionedFp, pkgName, provisioningCli);
                if(standaloneCliList.isEmpty()) {
                    standaloneCliList = new ArrayList<>();
                }
                standaloneCliList.add(provisioningCli);
            }
            provisioningCli = pmWfDir.resolve("domain.cli");
            if (Files.exists(provisioningCli)) {
                logPackageScript(provisionedFp, pkgName, provisioningCli);
                if (domainCliList.isEmpty()) {
                    domainCliList = new ArrayList<>();
                }
                domainCliList.add(provisioningCli);
            }
            provisioningCli = pmWfDir.resolve("host.cli");
            if (Files.exists(provisioningCli)) {
                logPackageScript(provisionedFp, pkgName, provisioningCli);
                if (domainCliList.isEmpty()) {
                    domainCliList = new ArrayList<>();
                }
                domainCliList.add(provisioningCli);
            }
        }
    }

    private boolean noScriptsLogged;
    private void logPackageScript(final ProvisionedFeaturePack provisionedFp, String pkgName, Path script) {
        if(noScriptsLogged) {
            System.out.println("Collected CLI scripts from " + provisionedFp.getGav() + ":");
            noScriptsLogged = false;
        }
        System.out.println(" - " + pkgName + '/' + script.getFileName());
    }

    private void processModules(ArtifactCoords.Gav fp, String pkgName, Path fpModuleDir) throws ProvisioningException {
        try {
            final Path installDir = ctx.getInstallDir();
            Files.walkFileTree(fpModuleDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                    final Path targetDir = installDir.resolve(fpModuleDir.relativize(dir));
                    try {
                        Files.copy(dir, targetDir);
                    } catch (FileAlreadyExistsException e) {
                         if (!Files.isDirectory(targetDir)) {
                             throw e;
                         }
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                    if(file.getFileName().toString().equals(WfConstants.MODULE_XML)) {
                        final String originalContent = IoUtils.readFile(file);

                        final String resolvedContent;
                        if(thinServer) {
                            try {
                                resolvedContent = propertyHandler.replaceProperties(originalContent);
                            } catch (Throwable t) {
                                throw new IOException("Failed to replace properties for " + file, t);
                            }
                        } else {
                            final String[] finalContent = new String[]{originalContent};
                            propertyHandler.handlePoperties(originalContent, new PropertyResolver() {
                                @Override
                                public String resolveProperty(final String property) {
                                    final int optionsIndex = property.indexOf('?');
                                    final String artifactName;
                                    final String expr;
                                    if(optionsIndex > 0) {
                                        // TODO handle options
                                        artifactName = property.substring(0, optionsIndex);
                                        expr = artifactName + '\\' + property.substring(optionsIndex);
                                    } else {
                                        artifactName = property;
                                        expr = property;
                                    }
                                    final String resolved = versionResolver.resolveProperty(artifactName);
                                    if(resolved == null) {
                                        return null;
                                    }
                                    final Path moduleArtifact;
                                    try {
                                        moduleArtifact = ctx.resolveArtifact(fromJBossModules(resolved, "jar"));
                                    } catch (ArtifactResolutionException e) {
                                        throw new IllegalStateException(e);
                                    }
                                    final String artifactFileName = moduleArtifact.getFileName().toString();
                                    try {
                                        IoUtils.copy(moduleArtifact, installDir.resolve(fpModuleDir.relativize(file.getParent())).resolve(artifactFileName));
                                    } catch (IOException e) {
                                        throw new IllegalStateException(e);
                                    }
//                                  // update module xml content
                                    finalContent[0] = finalContent[0].replaceFirst("<artifact\\s+name=\"\\$\\{" + expr
                                          + "\\}\"\\s*/>", "<resource-root path=\"" + artifactFileName + "\"/>");
//                                  // it's also possible that this is an element with nested content
//                                  // this regex involves a good deal of backtracking but it seems to work
                                    finalContent[0] = Pattern.compile(
                                            "<artifact\\s+name=\"\\$\\{" + expr + "\\}\"\\s*>(.*)</artifact>",
                                            Pattern.DOTALL)
                                            .matcher(finalContent[0])
                                            .replaceFirst("<resource-root path=\"" + artifactFileName + "\">$1</resource-root>");

                                    return resolved;
                                }
                            });
                            resolvedContent = finalContent[0];
                        }
                        IoUtils.writeFile(installDir.resolve(fpModuleDir.relativize(file)), resolvedContent);
                    } else {
                        Files.copy(file, installDir.resolve(fpModuleDir.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ProvisioningException("Failed to process modules from package " + pkgName + " from feature-pack " + fp, e);
        }
    }

    private static ArtifactCoords fromJBossModules(String str, String extension) {
        final String[] parts = str.split(":");
        if(parts.length < 2) {
            throw new IllegalArgumentException("Unexpected artifact coordinates format: " + str);
        }
        final String groupId = parts[0];
        final String artifactId = parts[1];
        String version = null;
        String classifier = null;
        if(parts.length > 2) {
            if(!parts[2].isEmpty()) {
                version = parts[2];
            }
            if(parts.length > 3 && !parts[3].isEmpty()) {
                classifier = parts[3];
                if(parts.length > 4) {
                    throw new IllegalArgumentException("Unexpected artifact coordinates format: " + str);
                }
            }
        }
        return new ArtifactCoords(groupId, artifactId, version, classifier, extension);
    }
}
