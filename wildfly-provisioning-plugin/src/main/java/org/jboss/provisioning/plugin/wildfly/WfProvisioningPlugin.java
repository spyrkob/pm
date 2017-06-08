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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.plugin.wildfly.config.CopyArtifact;
import org.jboss.provisioning.plugin.wildfly.config.CopyPath;
import org.jboss.provisioning.plugin.wildfly.config.FilePermission;
import org.jboss.provisioning.plugin.wildfly.config.GeneratorConfig;
import org.jboss.provisioning.plugin.wildfly.config.WildFlyPackageTasks;
import org.jboss.provisioning.runtime.FeaturePackRuntime;
import org.jboss.provisioning.runtime.PackageRuntime;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PropertyUtils;
import org.jboss.provisioning.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfProvisioningPlugin implements ProvisioningPlugin {

    private ProvisioningRuntime runtime;
    private PropertyResolver versionResolver;
    private final Pattern moduleArtifactPattern = Pattern.compile("(\\s*)((<artifact)(\\s+name=\")(\\$\\{)(.*)(\\})(\".*>)|(</artifact>))");

    private PropertyResolver tasksProps;

    private boolean thinServer;
    private Set<String> schemaGroups = Collections.emptySet();

    private StandaloneConfigGenerator standaloneGenerator;
    private DomainConfigGenerator domainScriptCollector;


    private void testEmbedded(Path installDir) {
        System.out.println("testing embedded server");

        // JBoss Modules overrides the default providers
        final String origXmlOutFactory = System.getProperty("javax.xml.stream.XMLOutputFactory");
        CommandContext ctx = null;
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(
                    new CommandContextConfiguration.Builder()
                    .setSilent(true)
                    .setInitConsole(false)
                    .build());
            ctx.handle("embed-server --empty-config --remove-existing --server-config=test.xml --jboss-home=" + installDir.toAbsolutePath());
            ctx.handle(":read-attribute(name=name)");
            ctx.handle("stop-embedded-server");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if(origXmlOutFactory == null) {
                if(System.getProperty("javax.xml.stream.XMLOutputFactory") != null) {
                    System.clearProperty("javax.xml.stream.XMLOutputFactory");
                }
            } else if(!origXmlOutFactory.equals(System.getProperty("javax.xml.stream.XMLOutputFactory"))) {
                System.setProperty("javax.xml.stream.XMLOutputFactory", origXmlOutFactory);
            }
            if(ctx != null) {
                ctx.terminateSession();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void postInstall(ProvisioningRuntime runtime) throws ProvisioningException {

        System.out.println("WildFly provisioning plug-in");

        final String thinServerProp = System.getProperty("wfThinServer");
        if(thinServerProp != null) {
            if(thinServerProp.isEmpty()) {
                thinServer = true;
            } else {
                thinServer = Boolean.parseBoolean(thinServerProp);
            }
        }

        this.runtime = runtime;

        Properties provisioningProps = new Properties();
        final Map<String, String> artifactVersions = new HashMap<>();
        for(FeaturePackRuntime fp : runtime.getFeaturePacks()) {
            final Path wfRes = fp.getResource(WfConstants.WILDFLY);
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
                if(!provisioningProps.isEmpty()) {
                    provisioningProps = new Properties(provisioningProps);
                }
                try(InputStream in = Files.newInputStream(tasksPropsPath)) {
                    provisioningProps.load(in);
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readFile(tasksPropsPath), e);
                }
            }

            if(fp.containsPackage(WfConstants.DOCS_SCHEMA)) {
                final Path schemaGroupsTxt = fp.getPackage(WfConstants.DOCS_SCHEMA).getResource(
                        WfConstants.PM, WfConstants.WILDFLY, WfConstants.SCHEMA_GROUPS_TXT);
                try(BufferedReader reader = Files.newBufferedReader(schemaGroupsTxt)) {
                    String line = reader.readLine();
                    while(line != null) {
                        if(!schemaGroups.contains(line)) {
                            switch(schemaGroups.size()) {
                                case 0:
                                    schemaGroups = Collections.singleton(line);
                                    break;
                                case 1:
                                    schemaGroups = new HashSet<>(schemaGroups);
                                default:
                                    schemaGroups.add(line);
                            }
                        }
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readFile(schemaGroupsTxt), e);
                }
            }
        }
        tasksProps = new MapPropertyResolver(provisioningProps);
        versionResolver = new MapPropertyResolver(artifactVersions);

        for(FeaturePackRuntime fp : runtime.getFeaturePacks()) {
            processPackages(fp);
        }

        if(domainScriptCollector != null) {
            domainScriptCollector.run();
        }

        //testEmbedded(runtime.getInstallDir());
    }

    private void processPackages(final FeaturePackRuntime fp) throws ProvisioningException {
        for(PackageRuntime pkg : fp.getPackages()) {
            final Path pmWfDir = pkg.getResource(WfConstants.PM, WfConstants.WILDFLY);
            if(!Files.exists(pmWfDir)) {
                continue;
            }

            final Path moduleDir = pmWfDir.resolve(WfConstants.MODULE);
            if(Files.exists(moduleDir)) {
                processModules(fp.getGav(), pkg.getName(), moduleDir);
            }
            final Path tasksXml = pmWfDir.resolve(WfConstants.TASKS_XML);
            if(Files.exists(tasksXml)) {
                final WildFlyPackageTasks pkgTasks = WildFlyPackageTasks.load(tasksXml);
                if(pkgTasks.hasCopyArtifacts()) {
                    copyArtifacts(pkgTasks);
                }
                if(pkgTasks.hasCopyPaths()) {
                    copyPaths(pkgTasks, pmWfDir);
                }
                if(pkgTasks.hasMkDirs()) {
                    mkdirs(pkgTasks, this.runtime.getInstallDir());
                }
                if (pkgTasks.hasFilePermissions() && !PropertyUtils.isWindows()) {
                    processFeaturePackFilePermissions(pkgTasks, this.runtime.getInstallDir());
                }
                final GeneratorConfig genConfig = pkgTasks.getGeneratorConfig();
                if(genConfig != null) {
                    if(genConfig.hasStandaloneConfig()) {
                        if(standaloneGenerator == null) {
                            standaloneGenerator = new StandaloneConfigGenerator(this.runtime);
                        }
                        standaloneGenerator.init(genConfig.getStandaloneConfig().getServerConfig());
                        standaloneGenerator.collectScripts(fp, pkg, null);
                        standaloneGenerator.run();
                    }
                    if(genConfig.hasDomainProfile()) {
                        if(domainScriptCollector == null) {
                            domainScriptCollector = new DomainConfigGenerator(this.runtime);
                        }
                        domainScriptCollector.collectScripts(fp, pkg, genConfig.getDomainProfileConfig().getProfile());
                    }
                }
            }
        }
    }

    private void processModules(ArtifactCoords.Gav fp, String pkgName, Path fpModuleDir) throws ProvisioningException {
        try {
            final Path installDir = runtime.getInstallDir();
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
                        processModuleTemplate(fpModuleDir, installDir, file);
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

    private void processModuleTemplate(Path fpModuleDir, final Path installDir, Path moduleTemplate) throws IOException {
        final String content = IoUtils.readFile(moduleTemplate);
        final Matcher m = moduleArtifactPattern.matcher(content);
        int copiedUntil = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(installDir.resolve(fpModuleDir.relativize(moduleTemplate)))) {
            while (m.find()) {
                if (m.end(7) < 0) {
                    writer.append(content, copiedUntil, m.end(1));
                    if (thinServer) {
                        writer.append("</artifact>"); // which is equivalent to writer.append(m.group(2));
                    } else {
                        writer.append("</resource-root>");
                    }
                    copiedUntil = m.end(2);
                } else {
                    writer.append(content, copiedUntil, m.start(0));

                    final String artifactName;
                    final boolean jandex;
                    final String property = m.group(6);
                    final int optionsIndex = property.indexOf('?');
                    if (optionsIndex > 0) {
                        artifactName = property.substring(0, optionsIndex);
                        jandex = property.indexOf("jandex", optionsIndex) >= 0;
                    } else {
                        artifactName = property;
                        jandex = false;
                    }

                    final String resolved = versionResolver.resolveProperty(artifactName);
                    if (resolved == null) {
                        writer.append(content, m.start(1), m.end(8));
                    } else {
                        final Path targetDir = installDir.resolve(fpModuleDir.relativize(moduleTemplate.getParent()));
                        final ArtifactCoords coords = fromJBossModules(resolved, "jar");
                        Path moduleArtifact = null;

                        if (jandex) {
                            try {
                                moduleArtifact = runtime.resolveArtifact(coords);
                            } catch (ProvisioningException e) {
                                throw new IOException(e);
                            }
                            final String artifactFileName = moduleArtifact.getFileName().toString();

                            final int lastDot = artifactFileName.lastIndexOf(".");
                            final File target = new File(targetDir.toFile(),
                                    new StringBuilder().append(artifactFileName.substring(0, lastDot))
                                    .append("-jandex")
                                    .append(artifactFileName.substring(lastDot)).toString());
                            try {
                                JandexIndexer.createIndex(moduleArtifact.toFile(), new FileOutputStream(target));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                            writer.append(m.group(1));
                            writer.append("<resource-root path=\"");
                            writer.append(target.getName());
                            writer.append("\"/>");
                        }

                        if (thinServer) {
                            writer.append(content, m.start(1), m.end(4));
                            writer.append(resolved);
                            writer.append(m.group(8));
                        } else {
                            if(moduleArtifact == null) {
                                try {
                                    moduleArtifact = runtime.resolveArtifact(coords);
                                } catch (ProvisioningException e) {
                                    throw new IOException(e);
                                }
                            }
                            final String artifactFileName = moduleArtifact.getFileName().toString();
                            try {
                                IoUtils.copy(moduleArtifact, targetDir.resolve(artifactFileName));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                            writer.append(m.group(1));
                            writer.append("<resource-root path=\"");
                            writer.append(artifactFileName);
                            writer.append(m.group(8));
                        }

                        if (schemaGroups.contains(coords.getGroupId())) {
                            if(moduleArtifact == null) {
                                try {
                                    moduleArtifact = runtime.resolveArtifact(coords);
                                } catch (ProvisioningException e) {
                                    throw new IOException(e);
                                }
                            }
                            extractSchemas(moduleArtifact);
                        }
                    }
                    copiedUntil = m.end(8);
                }
            }
            writer.append(content, copiedUntil, content.length());
        }
    }

    private void extractSchemas(Path moduleArtifact) throws IOException {
        final Path targetSchemasDir = this.runtime.getInstallDir().resolve(WfConstants.DOCS).resolve(WfConstants.SCHEMA);
        Files.createDirectories(targetSchemasDir);
        try (final FileSystem jarFS = FileSystems.newFileSystem(moduleArtifact, null)) {
            final Path schemaSrc = jarFS.getPath(WfConstants.SCHEMA);
            if (Files.exists(schemaSrc)) {
                ZipUtils.copyFromZip(schemaSrc.toAbsolutePath(), targetSchemasDir);
            }
        }
    }

    private void copyArtifacts(final WildFlyPackageTasks tasks) throws ProvisioningException {
        for(CopyArtifact copyArtifact : tasks.getCopyArtifacts()) {
            final String gavString = versionResolver.resolveProperty(copyArtifact.getArtifact());
            try {
                final ArtifactCoords coords = fromJBossModules(gavString, "jar");
                final Path jarSrc = runtime.resolveArtifact(coords);
                String location = copyArtifact.getToLocation();
                if (!location.isEmpty() && location.charAt(location.length() - 1) == '/') {
                    // if the to location ends with a / then it is a directory
                    // so we need to append the artifact name
                    location += jarSrc.getFileName();
                }

                final Path jarTarget = runtime.getInstallDir().resolve(location);

                Files.createDirectories(jarTarget.getParent());
                if (copyArtifact.isExtract()) {
                    extractArtifact(jarSrc, jarTarget, copyArtifact);
                } else {
                    IoUtils.copy(jarSrc, jarTarget);
                }
                if(schemaGroups.contains(coords.getGroupId())) {
                    extractSchemas(jarSrc);
                }
            } catch (IOException e) {
                throw new ProvisioningException("Failed to copy artifact " + gavString, e);
            }
        }
    }

    private void copyPaths(final WildFlyPackageTasks tasks, final Path pmWfDir) throws ProvisioningException {
        for(CopyPath copyPath : tasks.getCopyPaths()) {
            final Path src = pmWfDir.resolve(copyPath.getSrc());
            if (!Files.exists(src)) {
                throw new ProvisioningException(Errors.pathDoesNotExist(src));
            }
            final Path target = copyPath.getTarget() == null ? runtime.getInstallDir() : runtime.getInstallDir().resolve(copyPath.getTarget());
            if (copyPath.isReplaceProperties()) {
                if (!Files.exists(target.getParent())) {
                    try {
                        Files.createDirectories(target.getParent());
                    } catch (IOException e) {
                        throw new ProvisioningException(Errors.mkdirs(target.getParent()), e);
                    }
                }
                try {
                    Files.walkFileTree(src, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    final Path targetDir = target.resolve(src.relativize(dir));
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
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    PropertyReplacer.copy(file, target.resolve(src.relativize(file)), tasksProps);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.copyFile(src, target), e);
                }
            } else {
                try {
                    IoUtils.copy(src, target);
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.copyFile(src, target));
                }
            }
        }
    }

    private static void extractArtifact(Path artifact, Path target, CopyArtifact copy) throws IOException {
        if(!Files.exists(target)) {
            Files.createDirectories(target);
        }
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

    private static void mkdirs(final WildFlyPackageTasks tasks, Path installDir) throws ProvisioningException {
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

    private static void processFeaturePackFilePermissions(WildFlyPackageTasks tasks, Path installDir) throws ProvisioningException {
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
