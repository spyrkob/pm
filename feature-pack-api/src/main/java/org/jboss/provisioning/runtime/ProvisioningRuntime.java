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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.FeaturePackSet;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.util.FeaturePackInstallException;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PathsUtils;
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
        runtime.messageWriter.verbose("Moving provisioned installation from staged directory to %s", runtime.installDir);
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

    public static void exportDiff(ProvisioningRuntime runtime, Path target, Path customizedInstallation) throws ProvisioningException {
        // execute the plug-ins
        runtime.executeDiffPlugins(target, customizedInstallation);
    }

    private final long startTime;
    private final ArtifactResolver artifactResolver;
    private final ProvisioningConfig config;
    private final Path installDir;
    private final Path stagedDir;
    private final Path workDir;
    private final Path tmpDir;
    private final Path pluginsDir;
    private final Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;
    private final Map<String, String> parameters = new HashMap<>();
    private final MessageWriter messageWriter;
    private List<ProvisionedConfig> configs = Collections.emptyList();

    ProvisioningRuntime(ProvisioningRuntimeBuilder builder, final MessageWriter messageWriter) throws ProvisioningException {
        this.startTime = builder.startTime;
        this.artifactResolver = builder.artifactResolver;
        this.config = builder.config;
        this.pluginsDir = builder.pluginsDir;
        this.fpRuntimes = builder.fpRuntimes;

        if(!builder.anonymousConfigs.isEmpty()) {
            for(ProvisionedConfig config : builder.anonymousConfigs) {
                addConfig(config);
            }
        }
        if(!builder.noModelNamedConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelBuilder> config : builder.noModelNamedConfigs.entrySet()) {
                addConfig(config.getValue());
            }
        }
        if(!builder.noNameModelConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelBuilder> config : builder.noNameModelConfigs.entrySet()) {
                addConfig(config.getValue());
            }
        }
        if(!builder.modelConfigs.isEmpty()) {
            for(Map.Entry<String, Map<String, ConfigModelBuilder>> namedConfigs : builder.modelConfigs.entrySet()) {
                for(Map.Entry<String, ConfigModelBuilder> config : namedConfigs.getValue().entrySet()) {
                    addConfig(config.getValue());
                }
            }
        }
        if(!builder.parameters.isEmpty()) {
            this.parameters.putAll(builder.parameters);
        }
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

    private void addConfig(ProvisionedConfig config) {
        switch(configs.size()) {
            case 0:
                configs = Collections.singletonList(config);
                break;
            case 1:
                final ProvisionedConfig tmp = configs.get(0);
                configs = new ArrayList<>(2);
                configs.add(tmp);
            default:
                configs.add(config);
        }
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
     * @throws ArtifactResolutionException  in case the artifact could not be
     * resolved for any reason
     */
    public Path resolveArtifact(ArtifactCoords coords) throws ArtifactResolutionException {
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
        if(pluginsDir != null) {
            List<java.net.URL> urls = Collections.emptyList();
            try(Stream<Path> stream = Files.list(pluginsDir)) {
                final Iterator<Path> i = stream.iterator();
                while(i.hasNext()) {
                    switch(urls.size()) {
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
            if(!urls.isEmpty()) {
                final Thread thread = Thread.currentThread();
                final java.net.URLClassLoader ucl = new java.net.URLClassLoader(urls.toArray(
                        new java.net.URL[urls.size()]), thread.getContextClassLoader());
                final ServiceLoader<ProvisioningPlugin> pluginLoader = ServiceLoader.load(ProvisioningPlugin.class, ucl);
                final Iterator<ProvisioningPlugin> pluginIterator = pluginLoader.iterator();
                if(pluginIterator.hasNext()) {
                    final ClassLoader ocl = thread.getContextClassLoader();
                    try {
                        thread.setContextClassLoader(ucl);
                        final ProvisioningPlugin plugin = pluginIterator.next();
                        plugin.postInstall(this);
                        while(pluginIterator.hasNext()) {
                            pluginIterator.next().postInstall(this);
                        }
                    } finally {
                        thread.setContextClassLoader(ocl);
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        IoUtils.recursiveDelete(workDir);
        final long time = System.currentTimeMillis() - startTime;
        final long seconds = time / 1000;
        if(messageWriter.isVerboseEnabled()) {
            messageWriter.print("Done in %d.%d seconds", seconds, (time - seconds*1000));
        }
    }

    private void executeDiffPlugins(Path target, Path customizedInstallation) throws ProvisioningException {
        if(pluginsDir != null) {
            List<java.net.URL> urls = Collections.emptyList();
            try(Stream<Path> stream = Files.list(pluginsDir)) {
                final Iterator<Path> i = stream.iterator();
                while(i.hasNext()) {
                    switch(urls.size()) {
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
            if(!urls.isEmpty()) {
                final Thread thread = Thread.currentThread();
                final java.net.URLClassLoader ucl = new java.net.URLClassLoader(urls.toArray(
                        new java.net.URL[urls.size()]), thread.getContextClassLoader());
                final ServiceLoader<DiffPlugin> pluginLoader = ServiceLoader.load(DiffPlugin.class, ucl);
                final Iterator<DiffPlugin> pluginIterator = pluginLoader.iterator();
                if(pluginIterator.hasNext()) {
                    final ClassLoader ocl = thread.getContextClassLoader();
                    try {
                        thread.setContextClassLoader(ucl);
                        final DiffPlugin plugin = pluginIterator.next();
                        plugin.calculateConfiguationChanges(this, customizedInstallation, target);
                        while(pluginIterator.hasNext()) {
                            pluginIterator.next().calculateConfiguationChanges(this, customizedInstallation, target);
                        }
                    } finally {
                        thread.setContextClassLoader(ocl);
                    }
                }
            }
        }
    }
}
