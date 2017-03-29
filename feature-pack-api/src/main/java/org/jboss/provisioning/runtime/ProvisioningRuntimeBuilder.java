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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;


/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntimeBuilder {

    public static ProvisioningRuntimeBuilder newInstance() {
        return new ProvisioningRuntimeBuilder();
    }

    String encoding;
    ArtifactResolver artifactResolver;
    ProvisioningConfig config;
    PackageParameterResolver paramResolver;
    Path installDir;
    final Path workDir;
    final Path layoutDir;
    List<ProvisioningPlugin> plugins = Collections.emptyList();
    private Map<ArtifactCoords, URL> pluginUrls = Collections.emptyMap();

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> layoutFps = new HashMap<>();
    private Set<ArtifactCoords.Gav> dependencyResolution;

    Map<ArtifactCoords.Gav, FeaturePackRuntime.Builder> fpRuntimes;

    private ProvisioningRuntimeBuilder() {
        workDir = IoUtils.createRandomTmpDir();
        layoutDir = workDir.resolve("layout");
    }

    public ProvisioningRuntimeBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public ProvisioningRuntimeBuilder setArtifactResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder setConfig(ProvisioningConfig config) {
        this.config = config;
        return this;
    }

    public ProvisioningRuntimeBuilder setParameterResolver(PackageParameterResolver paramResolver) {
        this.paramResolver = paramResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder setInstallDir(Path installDir) {
        this.installDir = installDir;
        return this;
    }

    public ProvisioningRuntime build() throws ProvisioningException {

        Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> fpConfigBuilders = Collections.emptyMap();
        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePacks();
        for (FeaturePackConfig provisionedFp : fpConfigs) {
            fpConfigBuilders = merge(fpConfigBuilders, layoutFeaturePack(provisionedFp));
        }
        for (FeaturePackConfig fpConfig : fpConfigs) {
            fpConfigBuilders = enforce(layoutFps.get(fpConfig.getGav().toGa()), fpConfig, fpConfigBuilders);
        }
        fpRuntimes = new LinkedHashMap<>(layoutFps.size());
        for(Map.Entry<ArtifactCoords.Gav, FeaturePackConfig.Builder> entry : fpConfigBuilders.entrySet()) {
            final FeaturePackRuntime.Builder fp = layoutFps.get(entry.getKey().toGa());
            fp.config = entry.getValue().build();
            fpRuntimes.put(fp.gav, fp);
        }

        for(FeaturePackRuntime.Builder fp : fpRuntimes.values()) {
            resolvePackages(fp);
        }

        // set parameters set by the user in feature-pack configs
        for (FeaturePackConfig userFpConfig : config.getFeaturePacks()) {
            if (userFpConfig.hasIncludedPackages()) {
                FeaturePackRuntime.Builder fp = null;
                for (PackageConfig userPkg : userFpConfig.getIncludedPackages()) {
                    if (userPkg.hasParams()) {
                        if (fp == null) {
                            fp = layoutFps.get(userFpConfig.getGav().toGa());
                        }
                        final PackageRuntime.Builder pkgBuilder = fp.pkgBuilders.get(userPkg.getName());
                        for (PackageParameter param : userPkg.getParameters()) {
                            pkgBuilder.configBuilder.addParameter(param);
                        }
                    }
                }
            }
        }

        if(!pluginUrls.isEmpty()) {
            final java.net.URLClassLoader ucl = new java.net.URLClassLoader(pluginUrls.values().toArray(
                    new java.net.URL[pluginUrls.size()]), Thread.currentThread().getContextClassLoader());
            final ServiceLoader<ProvisioningPlugin> pluginLoader = ServiceLoader.load(ProvisioningPlugin.class, ucl);
            plugins = new ArrayList<>(pluginUrls.size());
            for(ProvisioningPlugin plugin : pluginLoader) {
                plugins.add(plugin);
            }
        }
        return new ProvisioningRuntime(this);
    }

    private void resolvePackages(FeaturePackRuntime.Builder fp) throws ProvisioningException {

        if(fp.config.isInheritPackages()) {
            for (String name : fp.spec.getDefaultPackageNames()) {
                if(!fp.config.isIncluded(name)) {
                    // spec parameters are set first
                    resolvePackage(fp, name, Collections.emptyList());
                }
            }
        }
        if(fp.config.hasIncludedPackages()) {
            for(PackageConfig packageConfig : fp.config.getIncludedPackages()) {
                // user parameters are set last
                if(!resolvePackage(fp, packageConfig.getName(), Collections.emptyList())) {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, packageConfig.getName()));
                }
            }
        }
        // set parameters set in feature-pack dependencies
        if(fp.spec.hasDependencies()) {
            for(FeaturePackDependencySpec depSpec : fp.spec.getDependencies()) {
                final FeaturePackConfig depConfig = depSpec.getTarget();
                FeaturePackRuntime.Builder dep = null;
                if(depConfig.hasIncludedPackages()) {
                    for(PackageConfig pkgConfig : depConfig.getIncludedPackages()) {
                        if(pkgConfig.hasParams()) {
                            if(dep == null) {
                                dep = layoutFps.get(depConfig.getGav().toGa());
                            }
                            final PackageConfig.Builder pkgBuilder = dep.pkgBuilders.get(pkgConfig.getName()).configBuilder;
                            for(PackageParameter param : pkgConfig.getParameters()) {
                                pkgBuilder.addParameter(param);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean resolvePackage(FeaturePackRuntime.Builder fp, final String pkgName, Collection<PackageParameter> params) throws ProvisioningException {

        if(fp.config.isExcluded(pkgName)) {
            return false;
        }

        final PackageRuntime.Builder pkgRt = fp.pkgBuilders.get(pkgName);
        if(pkgRt != null) {
            if(!params.isEmpty()) {
                for(PackageParameter param : params) {
                    pkgRt.configBuilder.addParameter(param);
                }
            }
            return true;
        }

        final PackageSpec pkgSpec = fp.spec.getPackage(pkgName);
        if (pkgSpec == null) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fp.gav, pkgName));
        }
        final PackageConfig.Builder pkgConfig = fp.newPackage(pkgSpec, LayoutUtils.getPackageDir(fp.dir, pkgName)).configBuilder;
        // set parameters set in the package spec first
        if(pkgSpec.hasParameters()) {
            for(PackageParameter param : pkgSpec.getParameters()) {
                pkgConfig.addParameter(param);
            }
        }

        if (pkgSpec.hasLocalDependencies()) {
            for (PackageDependencySpec dep : pkgSpec.getLocalDependencies().getDescriptions()) {
                final boolean resolved;
                try {
                    resolved = resolvePackage(fp, dep.getName(), dep.getParameters());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
                if(!resolved && !dep.isOptional()) {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgName, dep.getName()));
                }
            }
        }
        if(pkgSpec.hasExternalDependencies()) {
            for(String depName : pkgSpec.getExternalDependencyNames()) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final FeaturePackRuntime.Builder targetFp = layoutFps.get(depSpec.getTarget().getGav().toGa());

                final PackageDependencyGroupSpec pkgDeps = pkgSpec.getExternalDependencies(depName);
                for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                    final boolean resolved;
                    try {
                        resolved = resolvePackage(targetFp, pkgDep.getName(), pkgDep.getParameters());
                    } catch(ProvisioningDescriptionException e) {
                        if(pkgDep.isOptional()) {
                            continue;
                        } else {
                            throw e;
                        }
                    }
                    if(!resolved && !pkgDep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedExternalPackageDependency(fp.gav, pkgName, targetFp.gav, pkgDep.getName()));
                    }
                }
            }
        }

        if(!params.isEmpty()) {
            for(PackageParameter param : params) {
                pkgConfig.addParameter(param);
            }
        }
        fp.addPackage(pkgName);
        return true;
    }

    private Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> layoutFeaturePack(FeaturePackConfig fpConfig) throws ProvisioningException {
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

            try {
                fp.spec = FeaturePackLayoutDescriber.describeFeaturePack(fp.dir, encoding);
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningException("Failed to describe feature-pack " + fp.gav, e);
            }

            if(fp.spec.hasProvisioningPlugins()) {
                for(ArtifactCoords coords : fp.spec.getProvisioningPlugins()) {
                    addProvisioningPlugin(coords);
                }
            }
        } else {
            if(!fp.gav.equals(fpConfig.getGav())) {
                throw new ProvisioningException(Errors.featurePackVersionConflict(fp.gav, fpConfig.getGav()));
            }
        }

        Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> fpBuilders = Collections.emptyMap();
        if(fp.spec.hasDependencies()) {
            if(dependencyResolution == null) {
                dependencyResolution = new HashSet<>();
            }
            if(dependencyResolution.contains(fp.gav)) {
                return fpBuilders;
            } else {
                dependencyResolution.add(fp.gav);
                for (FeaturePackDependencySpec dep : fp.spec.getDependencies()) {
                    fpBuilders = merge(fpBuilders, layoutFeaturePack(dep.getTarget()));
                }
                for (FeaturePackDependencySpec dep : fp.spec.getDependencies()) {
                    fpBuilders = enforce(layoutFps.get(dep.getTarget().getGav().toGa()), dep.getTarget(), fpBuilders);
                }
                dependencyResolution.remove(fp.gav);
            }
        }

        // resources should be copied last overriding the dependency resources
        final Path fpResources = fp.dir.resolve(Constants.RESOURCES);
        if(Files.exists(fpResources)) {
            try {
                IoUtils.copy(fpResources, workDir.resolve(Constants.RESOURCES));
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpResources, workDir.resolve(Constants.RESOURCES)), e);
            }
        }
        return fpBuilders;
    }

    private Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> enforce(
            FeaturePackRuntime.Builder fp,
            FeaturePackConfig fpConfig,
            Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> fpBuilders) throws ProvisioningDescriptionException {
        final ArtifactCoords.Gav fpGav = fpConfig.getGav();
        switch(fpBuilders.size()) {
            case 0:
                fpBuilders = Collections.singletonMap(fpGav, FeaturePackConfig.builder(fp.spec, fpConfig));
                break;
            case 1:
                if(fpBuilders.containsKey(fpGav)) {
                    fpBuilders.get(fpGav).enforce(fpConfig);
                    break;
                }
                fpBuilders = new LinkedHashMap<>(fpBuilders);
            default:
                if(fpBuilders.containsKey(fpGav)) {
                    fpBuilders.get(fpGav).enforce(fpConfig);
                } else {
                    fpBuilders.put(fpGav, FeaturePackConfig.builder(fp.spec, fpConfig));
                }
        }
        return fpBuilders;
    }

    private Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> merge(
            Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> allBuilders,
            final Map<ArtifactCoords.Gav, FeaturePackConfig.Builder> depBuilders)
            throws ProvisioningDescriptionException {
        switch(allBuilders.size()) {
            case 0:
                allBuilders = depBuilders;
                break;
            case 1:
                final ArtifactCoords.Gav provisionedGav = allBuilders.keySet().iterator().next();
                if(depBuilders.size() == 1 && depBuilders.containsKey(provisionedGav)) {
                    allBuilders.get(provisionedGav).merge(depBuilders.get(provisionedGav).build());
                    break;
                }
                allBuilders = new LinkedHashMap<>(allBuilders);
            default:
                for(Map.Entry<ArtifactCoords.Gav, FeaturePackConfig.Builder> depEntry : depBuilders.entrySet()) {
                    final FeaturePackConfig.Builder fpBuilder = allBuilders.get(depEntry.getKey());
                    if(fpBuilder == null) {
                        allBuilders.put(depEntry.getKey(), depEntry.getValue());
                    } else {
                        fpBuilder.merge(depEntry.getValue().build());
                    }
                }
        }
        return allBuilders;
    }

    private void addProvisioningPlugin(ArtifactCoords coords) throws ArtifactResolutionException {
        if(pluginUrls.isEmpty()) {
            pluginUrls = Collections.singletonMap(coords, resolveUrl(coords));
        } else if(pluginUrls.containsKey(coords)) {
            return;
        } else {
            if(pluginUrls.size() == 1) {
                pluginUrls = new LinkedHashMap<>(pluginUrls);
            }
            pluginUrls.put(coords, resolveUrl(coords));
        }
    }

    private URL resolveUrl(ArtifactCoords coords) throws ArtifactResolutionException {
        try {
            return artifactResolver.resolve(coords).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new ArtifactResolutionException("Failed to resolve " + coords, e);
        }
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }
}
