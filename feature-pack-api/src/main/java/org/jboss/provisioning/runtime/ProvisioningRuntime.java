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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.FeaturePackSet;
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
            System.out.println("Installing " + fpGav);
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
        for(ProvisioningPlugin plugin : runtime.plugins) {
            plugin.postInstall(runtime);
        }

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

        System.out.println("Moving provisioned installation from staged directory to " + runtime.installDir);
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

    private final long startTime;
    private final ArtifactResolver artifactResolver;
    private final ProvisioningConfig config;
    private final Path installDir;
    private final Path stagedDir;
    private final Path workDir;
    private final Path tmpDir;
    private final List<ProvisioningPlugin> plugins;
    private final Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;

    ProvisioningRuntime(ProvisioningRuntimeBuilder builder) throws ProvisioningException {
        this.startTime = builder.startTime;
        this.artifactResolver = builder.artifactResolver;
        this.config = builder.config;
        this.plugins = builder.plugins;
        this.fpRuntimes = builder.fpRuntimes;

        this.workDir = builder.workDir;
        this.installDir = builder.installDir;
        this.stagedDir = workDir.resolve("staged");
        try {
            Files.createDirectories(stagedDir);
        } catch(IOException e) {
            throw new ProvisioningException(Errors.mkdirs(stagedDir), e);
        }

        this.tmpDir = workDir.resolve("tmp");
    }

    /**
     * The target installation location
     *
     * @return  installation location
     */
    public Path getInstallDir() {
        return stagedDir;
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
     * Returns feature-pack specification for the given GAV.
     *
     * @return  feature-pack specification
     */
    public FeaturePackSpec getFeaturePackSpec(ArtifactCoords.Gav fpGav) {
        final FeaturePackRuntime fp = fpRuntimes.get(fpGav);
        return fp == null ? null : fp.getSpec();
    }

    /**
     * Returns a resource path for the provisioning setup.
     *
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

    public boolean hasPlugins() {
        return !plugins.isEmpty();
    }

    public List<ProvisioningPlugin> getPlugins() {
        return plugins;
    }

    @Override
    public void close() throws IOException {
        IoUtils.recursiveDelete(workDir);
        final long time = System.currentTimeMillis() - startTime;
        final long seconds = time / 1000;
        System.out.println(new StringBuilder("Done in ").append(seconds).append('.').append(time - seconds*1000).append(" seconds").toString());
    }
}
