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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
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
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlParser;
import org.jboss.provisioning.xml.PackageXmlParser;


/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntimeBuilder {

    public static ProvisioningRuntimeBuilder newInstance() {
        return new ProvisioningRuntimeBuilder();
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
    private Set<ArtifactCoords.Gav> dependencyResolution;
    private Path pluginsDir = null;

    Map<ArtifactCoords.Gav, FeaturePackRuntime.Builder> fpRuntimes;

    private ProvisioningRuntimeBuilder() {
        startTime = System.currentTimeMillis();
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

        final PackageRuntime.Builder pkg = fp.newPackage(pkgName, LayoutUtils.getPackageDir(fp.dir, pkgName, false));
        if(!Files.exists(pkg.dir)) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fp.gav, pkgName));
        }
        final Path pkgXml = pkg.dir.resolve(Constants.PACKAGE_XML);
        if(!Files.exists(pkgXml)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(pkgXml));
        }
        try(BufferedReader reader = Files.newBufferedReader(pkgXml)) {
            pkg.spec = PackageXmlParser.getInstance().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(pkgXml), e);
        }

        final PackageConfig.Builder pkgConfig = pkg.configBuilder;
        // set parameters set in the package spec first
        if(pkg.spec.hasParameters()) {
            for(PackageParameter param : pkg.spec.getParameters()) {
                pkgConfig.addParameter(param);
            }
        }

        if (pkg.spec.hasLocalDependencies()) {
            for (PackageDependencySpec dep : pkg.spec.getLocalDependencies().getDescriptions()) {
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
        if(pkg.spec.hasExternalDependencies()) {
            for(String depName : pkg.spec.getExternalDependencyNames()) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final FeaturePackRuntime.Builder targetFp = layoutFps.get(depSpec.getTarget().getGav().toGa());

                final PackageDependencyGroupSpec pkgDeps = pkg.spec.getExternalDependencies(depName);
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

            final Path fpXml = fp.dir.resolve(Constants.FEATURE_PACK_XML);
            if(!Files.exists(fpXml)) {
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpXml));
            }

            try(BufferedReader reader = Files.newBufferedReader(fpXml)) {
                fp.spec = FeaturePackXmlParser.getInstance().parse(reader);
            } catch (IOException | XMLStreamException e) {
                throw new ProvisioningException(Errors.parseXml(fpXml), e);
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

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }
}
