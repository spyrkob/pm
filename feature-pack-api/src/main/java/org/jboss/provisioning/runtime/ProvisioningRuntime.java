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

package org.jboss.provisioning.runtime;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactException;
import org.jboss.provisioning.ArtifactRepositoryManager;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.diff.FileSystemDiffResult;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.plugin.UpgradePlugin;
import org.jboss.provisioning.repomanager.FeaturePackBuilder;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.FeaturePackSet;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.util.FeaturePackInstallException;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PathsUtils;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.xml.ProvisionedStateXmlWriter;
import org.jboss.provisioning.xml.ProvisioningXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntime implements FeaturePackSet<FeaturePackRuntime>, java.io.Closeable {

    public static void install(ProvisioningRuntime runtime) throws ProvisioningException {
        // copy package content
        for(FeaturePackRuntime fp : runtime.fpRuntimes.values()) {
            final ArtifactCoords.Gav fpGav = fp.getGav();
            runtime.messageWriter.verbose("Installing %s", fpGav);
            for(PackageRuntime pkg : fp.getPackages()) {
                final Path pkgSrcDir = pkg.getContentDir();
                if (Files.exists(pkgSrcDir)) {
                    try {
                        IoUtils.copy(pkgSrcDir, runtime.stagedDir);
                    } catch (IOException e) {
                        throw new FeaturePackInstallException(Errors.packageContentCopyFailed(pkg.getName()), e);
                    }
                }
            }
        }

        // execute the plug-ins
        runtime.executePlugins();

        // save the config
        try {
            ProvisioningXmlWriter.getInstance().write(runtime.config, PathsUtils.getProvisioningXml(runtime.stagedDir));
        } catch (XMLStreamException | IOException e) {
            throw new FeaturePackInstallException(Errors.writeFile(PathsUtils.getProvisioningXml(runtime.stagedDir)), e);
        }

        // save the provisioned state
        try {
            ProvisionedStateXmlWriter.getInstance().write(runtime, PathsUtils.getProvisionedStateXml(runtime.stagedDir));
        } catch (XMLStreamException | IOException e) {
            throw new FeaturePackInstallException(Errors.writeFile(PathsUtils.getProvisionedStateXml(runtime.stagedDir)), e);
        }
        runtime.messageWriter.verbose("Moving the provisioned installation from the staged directory to %s", runtime.installDir);
        // copy from the staged to the target installation directory
        if (Files.exists(runtime.installDir)) {
            IoUtils.recursiveDelete(runtime.installDir);
        }
        try {
            IoUtils.copy(runtime.stagedDir, runtime.installDir);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.copyFile(runtime.stagedDir, runtime.installDir));
        }
    }

    public static void exportToFeaturePack(ProvisioningRuntime runtime, Path location, Path installationHome) throws ProvisioningDescriptionException, ProvisioningException, IOException {
        diff(runtime, location, installationHome);
        FeaturePackRepositoryManager fpRepoManager = FeaturePackRepositoryManager.newInstance(location);
        Gav gav = ArtifactCoords.newGav(runtime.getParameter("gav"));
        FeaturePackBuilder fpBuilder = fpRepoManager.installer().newFeaturePack(gav);
        for (FeaturePackConfig fpConfig : runtime.getProvisioningConfig().getFeaturePacks()) {
            fpBuilder.addDependency(fpConfig);
        }
        runtime.exportDiffResultToFeaturePack(fpBuilder, installationHome);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(runtime.pluginsDir)) {
            for(Path file : stream) {
                if((Files.isRegularFile(file))) {
                    fpBuilder.addPlugin(file);
                }
            }
        } catch(IOException ioex) {
            throw new ProvisioningException(ioex);
        }
        fpBuilder.getInstaller().install();
        runtime.artifactResolver.install(gav.toArtifactCoords(), fpRepoManager.resolve(gav.toArtifactCoords()));
    }

    public static void diff(ProvisioningRuntime runtime, Path target, Path customizedInstallation) throws ProvisioningException, IOException {
        // execute the plug-ins
        runtime.executeDiffPlugins(target, customizedInstallation);
    }

    public static void upgrade(ProvisioningRuntime runtime, Path customizedInstallation) throws ProvisioningException {
        // execute the plug-ins
        runtime.executeUpgradePlugins(customizedInstallation);
         if (Files.exists(customizedInstallation)) {
            IoUtils.recursiveDelete(customizedInstallation);
        }
        try {
            IoUtils.copy(runtime.installDir, customizedInstallation);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.copyFile(runtime.installDir, customizedInstallation));
        }
    }

    private final long startTime;
    private final ArtifactRepositoryManager artifactResolver;
    private ProvisioningConfig config;
    private Path installDir;
    private final Path stagedDir;
    private final Path workDir;
    private final Path tmpDir;
    private final Path pluginsDir;
    private final Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;
    private final Map<String, String> parameters;
    private final MessageWriter messageWriter;
    private List<ProvisionedConfig> configs = Collections.emptyList();
    private FileSystemDiffResult diff = FileSystemDiffResult.empty();
    private ClassLoader pluginsClassLoader;
    private final String operation;

    ProvisioningRuntime(ProvisioningRuntimeBuilder builder, final MessageWriter messageWriter) throws ProvisioningException {
        this.startTime = builder.startTime;
        this.artifactResolver = builder.artifactResolver;
        this.config = builder.config;
        this.pluginsDir = builder.pluginsDir;
        this.fpRuntimes = builder.fpRuntimes;
        this.operation = builder.operation;

        if(!builder.anonymousConfigs.isEmpty()) {
            for(ProvisionedConfig config : builder.anonymousConfigs) {
                addConfig(config);
            }
        }
        if(!builder.nameOnlyConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelResolver> config : builder.nameOnlyConfigs.entrySet()) {
                addConfig(config.getValue());
            }
        }
        if(!builder.modelOnlyConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelResolver> config : builder.modelOnlyConfigs.entrySet()) {
                addConfig(config.getValue());
            }
        }
        if(!builder.namedModelConfigs.isEmpty()) {
            for(Map.Entry<String, Map<String, ConfigModelResolver>> namedConfigs : builder.namedModelConfigs.entrySet()) {
                for(Map.Entry<String, ConfigModelResolver> config : namedConfigs.getValue().entrySet()) {
                    addConfig(config.getValue());
                }
            }
        }
        parameters = builder.rtParams;

        if(configs.size() > 1) {
            configs = Collections.unmodifiableList(configs);
        }

        this.workDir = builder.workDir;
        this.installDir = builder.installDir;
        this.stagedDir = workDir.resolve("staged");
        try {
            Files.createDirectories(stagedDir);
        } catch(IOException e) {
            throw new ProvisioningException(Errors.mkdirs(stagedDir), e);
        }

        this.tmpDir = workDir.resolve("tmp");
        this.messageWriter = messageWriter;
    }

    private ClassLoader getPluginClassloader() throws ProvisioningException {
        if(pluginsClassLoader != null) {
            return pluginsClassLoader;
        }
        if (pluginsDir != null) {
            List<java.net.URL> urls = Collections.emptyList();
            try (Stream<Path> stream = Files.list(pluginsDir)) {
                final Iterator<Path> i = stream.iterator();
                while (i.hasNext()) {
                    switch (urls.size()) {
                        case 0:
                            urls = Collections.singletonList(i.next().toUri().toURL());
                            break;
                        case 1:
                            urls = new ArrayList<>(urls);
                        default:
                            urls.add(i.next().toUri().toURL());
                    }
                }
            } catch (IOException e) {
                throw new ProvisioningException(Errors.readDirectory(pluginsDir), e);
            }
            if (!urls.isEmpty()) {
                final Thread thread = Thread.currentThread();
                pluginsClassLoader = new java.net.URLClassLoader(urls.toArray(
                        new java.net.URL[urls.size()]), thread.getContextClassLoader());
            } else {
                pluginsClassLoader = Thread.currentThread().getContextClassLoader();
            }
        } else {
            pluginsClassLoader = Thread.currentThread().getContextClassLoader();
        }
        return pluginsClassLoader;
    }

    private void addConfig(ProvisionedConfig config) {
        configs = PmCollections.add(configs, config);
    }

    /**
     * The target staged location
     *
     * @return the staged location
     */
    public Path getStagedDir() {
        return stagedDir;
    }

    /**
     * The target installation location
     *
     * @return the installation location
     */
    public Path getInstallDir() {
        return installDir;
    }

    public void setInstallDir(Path installDir) {
        this.installDir = installDir;
    }

    /**
     * Configuration of the installation to be provisioned.
     *
     * @return  installation configuration
     */
    public ProvisioningConfig getProvisioningConfig() {
        return config;
    }

    @Override
    public boolean hasFeaturePacks() {
        return !fpRuntimes.isEmpty();
    }

    @Override
    public Set<Gav> getFeaturePackGavs() {
        return fpRuntimes.keySet();
    }

    @Override
    public Collection<FeaturePackRuntime> getFeaturePacks() {
        return fpRuntimes.values();
    }

    @Override
    public FeaturePackRuntime getFeaturePack(Gav gav) {
        return fpRuntimes.get(gav);
    }

    /**
     * Returns a writer for messages to be reported.
     *
     * @return the message writer
     */
    public MessageWriter getMessageWriter() {
        return messageWriter;
    }

    public void setDiff(FileSystemDiffResult diff) {
        this.diff = diff;
    }

    /**
     * Returns the result of a diff if such an operation was called previously.
     *
     * @return the result of a diff
     */
    public FileSystemDiffResult getDiff() {
        return diff;
    }

    public void exportDiffResultToFeaturePack(FeaturePackBuilder fpBuilder, Path installationHome) throws ProvisioningException {
        ClassLoader pluginClassLoader = getPluginClassloader();
        if (pluginClassLoader != null) {
            final Thread thread = Thread.currentThread();
            final ClassLoader ocl = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(pluginClassLoader);
                diff.toFeaturePack(fpBuilder, this, installationHome);
            } finally {
                thread.setContextClassLoader(ocl);
            }
        }
    }

    /**
     * Returns the current operation being executed.
     *
     * @return the current operation being executed.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns feature-pack specification for the given GAV.
     *
     * @param fpGav the GAV of which the specification is returned.
     * @return  feature-pack specification
     */
    public FeaturePackSpec getFeaturePackSpec(ArtifactCoords.Gav fpGav) {
        final FeaturePackRuntime fp = fpRuntimes.get(fpGav);
        return fp == null ? null : fp.getSpec();
    }

    /**
     * Returns a resource path for the provisioning setup.
     *
     * @param path
     * @return  file-system path for the resource
     */
    public Path getResource(String... path) {
        if(path.length == 0) {
            throw new IllegalArgumentException("Resource path is null");
        }
        if(path.length == 1) {
            return workDir.resolve(Constants.RESOURCES).resolve(path[0]);
        }
        Path p = workDir.resolve(Constants.RESOURCES);
        for(String name : path) {
            p = p.resolve(name);
        }
        return p;
    }

    /**
     * Returns a path for a temporary file-system resource.
     *
     * @return  temporary file-system path
     */
    public Path getTmpPath(String... path) {
        if(path.length == 0) {
            return tmpDir;
        }
        if(path.length == 1) {
            return tmpDir.resolve(path[0]);
        }
        Path p = tmpDir;
        for(String name : path) {
            p = p.resolve(name);
        }
        return p;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Resolves the location of the artifact given its coordinates.
     *
     * @param coords  artifact coordinates
     * @return  location of the artifact
     * @throws ArtifactException  in case the artifact could not be
     * resolved for any reason
     */
    public Path resolveArtifact(ArtifactCoords coords) throws ArtifactException {
        return artifactResolver.resolve(coords);
    }

    @Override
    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    @Override
    public List<ProvisionedConfig> getConfigs() {
        return configs;
    }

    private void executePlugins() throws ProvisioningException {
        ClassLoader pluginClassLoader = getPluginClassloader();
        if (pluginClassLoader != null) {
            final Thread thread = Thread.currentThread();
            final ServiceLoader<ProvisioningPlugin> pluginLoader = ServiceLoader.load(ProvisioningPlugin.class, pluginClassLoader);
            final Iterator<ProvisioningPlugin> pluginIterator = pluginLoader.iterator();
            if (pluginIterator.hasNext()) {
                final ClassLoader ocl = thread.getContextClassLoader();
                try {
                    thread.setContextClassLoader(pluginClassLoader);
                    final ProvisioningPlugin plugin = pluginIterator.next();
                    plugin.postInstall(this);
                    while (pluginIterator.hasNext()) {
                        pluginIterator.next().postInstall(this);
                    }
                } finally {
                    thread.setContextClassLoader(ocl);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        IoUtils.recursiveDelete(workDir);
        if (messageWriter.isVerboseEnabled()) {
            final long time = System.currentTimeMillis() - startTime;
            final long seconds = time / 1000;
            messageWriter.verbose("Done in %d.%d seconds", seconds, (time - seconds * 1000));
        }
    }

    private void executeDiffPlugins(Path target, Path customizedInstallation) throws ProvisioningException, IOException {
        ClassLoader pluginClassLoader = getPluginClassloader();
        if (pluginClassLoader != null) {
            final Thread thread = Thread.currentThread();
            final ServiceLoader<DiffPlugin> pluginLoader = ServiceLoader.load(DiffPlugin.class, pluginClassLoader);
            final Iterator<DiffPlugin> pluginIterator = pluginLoader.iterator();
            if (pluginIterator.hasNext()) {
                final ClassLoader ocl = thread.getContextClassLoader();
                try {
                    thread.setContextClassLoader(pluginClassLoader);
                    final DiffPlugin plugin = pluginIterator.next();
                    plugin.computeDiff(this, customizedInstallation, target);
                    while (pluginIterator.hasNext()) {
                        pluginIterator.next().computeDiff(this, customizedInstallation, target);
                    }
                } finally {
                    thread.setContextClassLoader(ocl);
                }
            }
        }
    }

    private void executeUpgradePlugins(Path customizedInstallation) throws ProvisioningException {
        ClassLoader pluginClassLoader = getPluginClassloader();
        if (pluginClassLoader != null) {
            final Thread thread = Thread.currentThread();
            final ServiceLoader<UpgradePlugin> pluginLoader = ServiceLoader.load(UpgradePlugin.class, pluginClassLoader);
            final Iterator<UpgradePlugin> pluginIterator = pluginLoader.iterator();
            if (pluginIterator.hasNext()) {
                final ClassLoader ocl = thread.getContextClassLoader();
                try {
                    thread.setContextClassLoader(pluginClassLoader);
                    final UpgradePlugin plugin = pluginIterator.next();
                    plugin.upgrade(this, customizedInstallation);
                    while (pluginIterator.hasNext()) {
                        pluginIterator.next().upgrade(this, customizedInstallation);
                    }
                } finally {
                    thread.setContextClassLoader(ocl);
                }
            }
        }
    }
}
