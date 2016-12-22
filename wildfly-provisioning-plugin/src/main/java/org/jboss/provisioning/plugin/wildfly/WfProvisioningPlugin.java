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
import java.util.stream.Stream;

import org.jboss.as.cli.CommandLineException;
import org.jboss.provisioning.ArtifactCoords;
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

    private BuildPropertyReplacer versionResolver;
    private List<Path> cliList = Collections.emptyList();

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void postInstall(ProvisioningContext ctx) throws ProvisioningException {

        System.out.println("WildFly-based configuration assembling plug-in");

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
        versionResolver = new BuildPropertyReplacer(new MapPropertyResolver(artifactVersions));

        processPackages(ctx);

        if(!cliList.isEmpty()) {
            final ConfigGenerator configGen = ConfigGenerator.newInstance(ctx.getInstallDir());
            for(Path p : cliList) {
                try {
                    configGen.addCommandLines(p);
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to read " + p);
                }
            }
            try {
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

    private void processPackages(ProvisioningContext ctx) throws ProvisioningException {
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
                                processPackages(ctx.getProvisionedState().getFeaturePack(fpGav), version, ctx.getInstallDir());
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

    private void processPackages(final ProvisionedFeaturePack provisionedFp, Path fpDir, Path installDir) throws ProvisioningException {
        final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
        if(!Files.exists(packagesDir)) {
            throw new ProvisioningException(Errors.pathDoesNotExist(packagesDir));
        }
        boolean foundScript = false;
        for(String pkgName : provisionedFp.getPackageNames()) {
            final Path pmWfDir = packagesDir.resolve(pkgName).resolve("pm/wildfly");
            final Path moduleDir = pmWfDir.resolve(WfConstants.MODULE);
            if(Files.exists(moduleDir)) {
                // look for and process modules
                processModules(provisionedFp.getGav(), pkgName, moduleDir, installDir);
            }
            // collect cli scripts
            final Path provisioningCli = pmWfDir.resolve("provisioning.cli");
            if (Files.exists(provisioningCli)) {
                if(!foundScript) {
                    System.out.println("Collected CLI scripts from " + provisionedFp.getGav() + ":");
                    foundScript = true;
                }
                System.out.println(" - " + pkgName);
                if(cliList.isEmpty()) {
                    cliList = new ArrayList<>();
                }
                cliList.add(provisioningCli);
            }
        }
    }

    private void processModules(ArtifactCoords.Gav fp, String pkgName, Path fpModuleDir, Path installDir) throws ProvisioningException {
        try {
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
                        String resolvedContent;
                        try {
                            resolvedContent = versionResolver.replaceProperties(originalContent);
                        } catch (Throwable t) {
                            throw new IOException("Failed to replace properties for " + file, t);
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
}
