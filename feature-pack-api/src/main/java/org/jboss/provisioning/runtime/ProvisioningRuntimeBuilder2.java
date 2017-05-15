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

import java.io.BufferedReader;
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
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlParser;


/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntimeBuilder2 {

    public static ProvisioningRuntimeBuilder2 newInstance() {
        return new ProvisioningRuntimeBuilder2();
    }

    final long startTime;
    String encoding;
    ArtifactResolver artifactResolver;
    ProvisioningConfig config;
    PackageParameterResolver paramResolver;
    Path installDir;
    final Path workDir;
    final Path layoutDir;
    List<ProvisioningPlugin> plugins = Collections.emptyList();

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> layoutFps = new HashMap<>();
    private Map<ArtifactCoords.Gav, List<FeaturePackConfig>> fpBranch = new HashMap<>();
    private Path pluginsDir = null;

    Map<ArtifactCoords.Gav, FeaturePackRuntime.Builder> fpRuntimes;

    private ProvisioningRuntimeBuilder2() {
        startTime = System.currentTimeMillis();
        workDir = IoUtils.createRandomTmpDir();
        layoutDir = workDir.resolve("layout");
    }

    public ProvisioningRuntimeBuilder2 setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public ProvisioningRuntimeBuilder2 setArtifactResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder2 setConfig(ProvisioningConfig config) {
        this.config = config;
        return this;
    }

    public ProvisioningRuntimeBuilder2 setParameterResolver(PackageParameterResolver paramResolver) {
        this.paramResolver = paramResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder2 setInstallDir(Path installDir) {
        this.installDir = installDir;
        return this;
    }

    public ProvisioningRuntime build() throws ProvisioningException {

        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePacks();
        for (FeaturePackConfig fpConfig : fpConfigs) {
            fpBranch.put(fpConfig.getGav(), Collections.singletonList(fpConfig));
        }
        for (FeaturePackConfig fpConfig : fpConfigs) {
            layoutFeaturePack(fpConfig);
        }

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
                final java.net.URLClassLoader ucl = new java.net.URLClassLoader(urls.toArray(
                        new java.net.URL[urls.size()]), Thread.currentThread().getContextClassLoader());
                final ServiceLoader<ProvisioningPlugin> pluginLoader = ServiceLoader.load(ProvisioningPlugin.class, ucl);
                plugins = new ArrayList<>(urls.size());
                for (ProvisioningPlugin plugin : pluginLoader) {
                    plugins.add(plugin);
                }
            }
        }

        return null;
    }

    private void layoutFeaturePack(FeaturePackConfig fpConfig) throws ProvisioningException {
        FeaturePackRuntime.Builder fp = layoutFps.get(fpConfig.getGav().toGa());
        if(fp == null) {
            fp = FeaturePackRuntime.builder(fpConfig.getGav(), LayoutUtils.getFeaturePackDir(layoutDir, fpConfig.getGav(), false));
            mkdirs(fp.dir);
            layoutFps.put(fp.gav.toGa(), fp);

            final Path artifactPath = artifactResolver.resolve(fp.gav.toArtifactCoords());
            try {
                //System.out.println("Adding " + fpGav + " to the layout at " + fpWorkDir);
                ZipUtils.unzip(artifactPath, fp.dir);
            } catch (IOException e) {
                throw new ProvisioningException("Failed to unzip " + artifactPath + " to " + layoutDir, e);
            }

            final Path fpXml = fp.dir.resolve(Constants.FEATURE_PACK_XML);
            if(!Files.exists(fpXml)) {
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpXml));
            }

            try(BufferedReader reader = Files.newBufferedReader(fpXml)) {
                fp.spec = FeaturePackXmlParser.getInstance().parse(reader);
            } catch (IOException | XMLStreamException e) {
                throw new ProvisioningException(Errors.parseXml(fpXml), e);
            }
        } else if(!fp.gav.equals(fpConfig.getGav())) {
            throw new ProvisioningException(Errors.featurePackVersionConflict(fp.gav, fpConfig.getGav()));
        }

        if(fp.spec.hasDependencies()) {
            final Collection<FeaturePackDependencySpec> fpDeps = fp.spec.getDependencies();
            final List<FeaturePackDependencySpec> depsToProcess = new ArrayList<>(fpDeps.size());
            for (FeaturePackDependencySpec fpDep : fpDeps) {
                final FeaturePackConfig fpDepTarget = fpDep.getTarget();
                List<FeaturePackConfig> fpConfigStack = fpBranch.get(fpDepTarget.getGav());
                if(fpConfigStack == null) {
                    fpBranch.put(fp.gav, Collections.singletonList(fpDepTarget));
                    depsToProcess.add(fpDep);
                } else if(fpConfigStack.get(fpConfigStack.size() - 1).isInheritPackages()) {
                    boolean processDep = false;
                    if(fpDepTarget.hasExcludedPackages()) {
                        for(String excluded : fpDepTarget.getExcludedPackages()) {
                            if(!isPackageExcluded(fpConfigStack, excluded) && !isPackageIncluded(fpConfigStack, excluded)) {
                                processDep = true;
                                break;
                            }
                        }
                    }
                    if(!processDep && fpDepTarget.hasIncludedPackages()) {
                        for(PackageConfig included : fpDepTarget.getIncludedPackages()) {
                            if(!isPackageIncluded(fpConfigStack, included.getName()) && !isPackageExcluded(fpConfigStack, included.getName())) {
                                processDep = true;
                                break;
                            }
                        }
                    }
                    if(processDep) {
                        depsToProcess.add(fpDep);
                        if (fpConfigStack.size() == 1) {
                            fpConfigStack = new ArrayList<>(fpConfigStack);
                            fpBranch.put(fp.gav, fpConfigStack);
                        }
                        fpConfigStack.add(fpDepTarget);
                    }
                }
            }
            if (!depsToProcess.isEmpty()) {
                for (FeaturePackDependencySpec dep : depsToProcess) {
                    layoutFeaturePack(dep.getTarget());
                }
                for (FeaturePackDependencySpec dep : depsToProcess) {
                    final Gav depGav = dep.getTarget().getGav();
                    List<FeaturePackConfig> fpConfigStack = fpBranch.get(depGav);
                    if (fpConfigStack.size() == 1) {
                        fpBranch.remove(depGav);
                    } else {
                        fpConfigStack.remove(fpConfigStack.size() - 1);
                        if (fpConfigStack.size() == 1) {
                            fpBranch.put(depGav, Collections.singletonList(fpConfigStack.get(0)));
                        }
                    }
                }
            }
        }

        // TODO process the packages

        // resources should be copied last overriding the dependency resources
        final Path fpResources = fp.dir.resolve(Constants.RESOURCES);
        if(Files.exists(fpResources)) {
            try {
                IoUtils.copy(fpResources, workDir.resolve(Constants.RESOURCES));
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpResources, workDir.resolve(Constants.RESOURCES)), e);
            }
        }

        final Path fpPlugins = fp.dir.resolve(Constants.PLUGINS);
        if(Files.exists(fpPlugins)) {
            if(pluginsDir == null) {
                pluginsDir = workDir.resolve(Constants.PLUGINS);
            }
            try {
                IoUtils.copy(fpPlugins, pluginsDir);
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpPlugins, workDir.resolve(Constants.PLUGINS)), e);
            }
        }
    }

    private boolean isPackageIncluded(List<FeaturePackConfig> stack, String packageName) {
        int i = stack.size() - 1;
        while(i >= 0) {
            if(stack.get(i--).isIncluded(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackageExcluded(List<FeaturePackConfig> stack, String packageName) {
        int i = stack.size() - 1;
        while(i >= 0) {
            if(stack.get(i--).isExcluded(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }
}
