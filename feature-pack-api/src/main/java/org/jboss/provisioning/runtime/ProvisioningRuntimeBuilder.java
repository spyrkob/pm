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
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.feature.AbstractFeatureGroup;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureGroupConfig;
import org.jboss.provisioning.feature.FeatureGroupSpec;
import org.jboss.provisioning.feature.FeatureId;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.SpecId;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.PackageDependencies;
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
    Path pluginsDir = null;

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> fpRtBuilders = new HashMap<>();
    private List<FeaturePackRuntime.Builder> fpRtBuildersOrdered = new ArrayList<>();
    List<ConfigModelBuilder> anonymousConfigs = Collections.emptyList();
    Map<String, ConfigModelBuilder> noModelNamedConfigs = Collections.emptyMap();
    Map<String, ConfigModelBuilder> noNameModelConfigs = Collections.emptyMap();
    Map<String, Map<String, ConfigModelBuilder>> modelConfigs = Collections.emptyMap();
    Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;

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

        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePacks();
        for (FeaturePackConfig fpConfig : fpConfigs) {
            getRtBuilder(fpConfig.getGav()).push(fpConfig);
        }
        for (FeaturePackConfig fpConfig : fpConfigs) {
            processFpConfig(fpConfig);
        }
        buildConfigs();

        switch(fpRtBuildersOrdered.size()) {
            case 0: {
                fpRuntimes = Collections.emptyMap();
                break;
            }
            case 1: {
                final FeaturePackRuntime.Builder builder = fpRtBuildersOrdered.get(0);
                copyResources(builder);
                fpRuntimes = Collections.singletonMap(builder.gav, builder.build(paramResolver));
                break;
            }
            default: {
                fpRuntimes = new LinkedHashMap<>(fpRtBuildersOrdered.size());
                for(FeaturePackRuntime.Builder builder : fpRtBuildersOrdered) {
                    copyResources(builder);
                    fpRuntimes.put(builder.gav, builder.build(paramResolver));
                }
                fpRuntimes = Collections.unmodifiableMap(fpRuntimes);
            }
        }

        return new ProvisioningRuntime(this);
    }

    private void buildConfigs() throws ProvisioningException {
        if(!anonymousConfigs.isEmpty()) {
            for(ConfigModelBuilder config : anonymousConfigs) {
                config.build();
            }
        }
        if(!noModelNamedConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelBuilder> entry : noModelNamedConfigs.entrySet()) {
                entry.getValue().build();
            }
        }

        if(!noNameModelConfigs.isEmpty()) {
            final Iterator<Map.Entry<String, ConfigModelBuilder>> i = noNameModelConfigs.entrySet().iterator();
            if(noNameModelConfigs.size() == 1) {
                final Map.Entry<String, ConfigModelBuilder> entry = i.next();
                final Map<String, ConfigModelBuilder> targetConfigs = modelConfigs.get(entry.getKey());
                if (targetConfigs == null) {
                    entry.getValue().build();
                } else {
                    noNameModelConfigs = Collections.emptyMap();
                    for (Map.Entry<String, ConfigModelBuilder> targetConfig : targetConfigs.entrySet()) {
                        targetConfig.getValue().merge(entry.getValue());
                    }
                }
            } else {
                while (i.hasNext()) {
                    final Map.Entry<String, ConfigModelBuilder> entry = i.next();
                    final Map<String, ConfigModelBuilder> targetConfigs = modelConfigs.get(entry.getKey());
                    if (targetConfigs == null) {
                        entry.getValue().build();
                        continue;
                    }
                    i.remove();
                    for (Map.Entry<String, ConfigModelBuilder> targetConfig : targetConfigs.entrySet()) {
                        targetConfig.getValue().merge(entry.getValue());
                    }
                }
            }
        }

        for(Map<String, ConfigModelBuilder> configMap : modelConfigs.values()) {
            for(Map.Entry<String, ConfigModelBuilder> configEntry : configMap.entrySet()) {
                configEntry.getValue().build();
            }
        }
    }

    private void processFpConfig(FeaturePackConfig fpConfig) throws ProvisioningException {
        final FeaturePackRuntime.Builder fp = getRtBuilder(fpConfig.getGav());

        List<FeaturePackConfig> pushedDepConfigs = Collections.emptyList();
        if(fp.spec.hasDependencies()) {
            final Collection<FeaturePackDependencySpec> fpDeps = fp.spec.getDependencies();
            pushedDepConfigs = new ArrayList<>(fpDeps.size());
            for (FeaturePackDependencySpec fpDep : fpDeps) {
                pushFpConfig(pushedDepConfigs, fpDep.getTarget());
            }
            if (!pushedDepConfigs.isEmpty()) {
                for (FeaturePackConfig depConfig : pushedDepConfigs) {
                    processFpConfig(depConfig);
                }
            }
        }

        boolean contributed = false;

        if(fpConfig.isInheritConfigs()) {
            for(Config config : fp.spec.getConfigs()) {
                if(fp.isConfigExcluded(config)) {
                    continue;
                }
                contributed |= includeConfig(fp, config);
            }
        } else {
            for(Config config : fp.spec.getConfigs()) {
                if(fp.isConfigIncluded(config)) {
                    contributed |= includeConfig(fp, config);
                }
            }
        }

        if(fpConfig.isInheritPackages()) {
            for(String packageName : fp.spec.getDefaultPackageNames()) {
                if(!fp.isPackageExcluded(packageName)) {
                    resolvePackage(fp, packageName, Collections.emptyList());
                    contributed = true;
                }
            }
        }
        if (fpConfig.hasIncludedPackages()) {
            for (PackageConfig pkgConfig : fpConfig.getIncludedPackages()) {
                if (!fp.isPackageExcluded(pkgConfig.getName())) {
                    resolvePackage(fp, pkgConfig.getName(), pkgConfig.getParameters());
                    contributed = true;
                } else {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgConfig.getName()));
                }
            }
        }

        if (!pushedDepConfigs.isEmpty()) {
            popFpConfigs(pushedDepConfigs);
        }

        if (fpConfig.hasDefinedConfigs()) {
            for (String modelName : fpConfig.getDefinedConfigModels()) {
                for(Config config : fpConfig.getDefinedConfigs(modelName)) {
                    if (fp.isConfigExcluded(config)) {
                        continue;
                    }
                    contributed |= includeConfig(fp, config);
                }
            }
        }

        if(!fp.ordered && contributed) {
            orderFpRtBuilder(fp);
        }
    }

    private boolean includeConfig(FeaturePackRuntime.Builder fp, Config config) throws ProvisioningException {
        return processFeatureGroupSpec(getConfigModelBuilder(config), fp, config);
    }

    private ConfigModelBuilder getConfigModelBuilder(Config config) {
        if(config.getModel() == null) {
            if(config.getName() == null) {
                final ConfigModelBuilder modelBuilder = ConfigModelBuilder.anonymous();
                modelBuilder.overwriteProps(config.getProperties());
                switch(anonymousConfigs.size()) {
                    case 0:
                        anonymousConfigs = Collections.singletonList(modelBuilder);
                        break;
                    case 1:
                        anonymousConfigs = new ArrayList<>(anonymousConfigs);
                    default:
                        anonymousConfigs.add(modelBuilder);
                }
                return modelBuilder;
            }
            if (noModelNamedConfigs.isEmpty()) {
                final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forName(config.getName());
                modelBuilder.overwriteProps(config.getProperties());
                noModelNamedConfigs = Collections.singletonMap(config.getName(), modelBuilder);
                return modelBuilder;
            }
            ConfigModelBuilder modelBuilder = noModelNamedConfigs.get(config.getName());
            if (modelBuilder == null) {
                modelBuilder = ConfigModelBuilder.forName(config.getName());
                if (noModelNamedConfigs.size() == 1) {
                    noModelNamedConfigs = new LinkedHashMap<>(noModelNamedConfigs);
                }
                noModelNamedConfigs.put(config.getName(), modelBuilder);
            }
            modelBuilder.overwriteProps(config.getProperties());
            return modelBuilder;
        }
        if(config.getName() == null) {
            if(noNameModelConfigs.isEmpty()) {
                final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forModel(config.getModel());
                modelBuilder.overwriteProps(config.getProperties());
                noNameModelConfigs = Collections.singletonMap(config.getModel(), modelBuilder);
                return modelBuilder;
            }
            ConfigModelBuilder modelBuilder = noNameModelConfigs.get(config.getModel());
            if (modelBuilder == null) {
                modelBuilder = ConfigModelBuilder.forModel(config.getModel());
                if (noNameModelConfigs.size() == 1) {
                    noNameModelConfigs = new LinkedHashMap<>(noNameModelConfigs);
                }
                noNameModelConfigs.put(config.getModel(), modelBuilder);
            }
            modelBuilder.overwriteProps(config.getProperties());
            return modelBuilder;
        }
        if (modelConfigs.isEmpty()) {
            final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forConfig(config.getModel(), config.getName());
            modelBuilder.overwriteProps(config.getProperties());
            modelConfigs = Collections.singletonMap(config.getModel(), Collections.singletonMap(config.getName(), modelBuilder));
            return modelBuilder;
        }
        Map<String, ConfigModelBuilder> namedConfigs = modelConfigs.get(config.getModel());
        if (namedConfigs == null) {
            final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forConfig(config.getModel(), config.getName());
            if (modelConfigs.size() == 1) {
                modelConfigs = new LinkedHashMap<>(modelConfigs);
            }
            modelConfigs.put(config.getModel(), Collections.singletonMap(config.getName(), modelBuilder));
            modelBuilder.overwriteProps(config.getProperties());
            return modelBuilder;
        }
        final ConfigModelBuilder modelBuilder = namedConfigs.get(config.getName());
        if (modelBuilder == null) {
            if (namedConfigs.size() == 1) {
                namedConfigs = new HashMap<>(namedConfigs);
                if (modelConfigs.size() == 1) {
                    modelConfigs = new LinkedHashMap<>(modelConfigs);
                }
                modelConfigs.put(config.getModel(), namedConfigs);
            }
            namedConfigs.put(config.getName(), modelBuilder);
        }
        modelBuilder.overwriteProps(config.getProperties());
        return modelBuilder;
    }

    private boolean processFeatureGroupConfig(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, FeatureGroupConfig fgConfig) throws ProvisioningException {
        if(!modelBuilder.pushConfig(fp.gav, resolveFeatureGroupConfig(fp, fgConfig))) {
            return false;
        }
        boolean resolvedFeatures = processFeatureGroupSpec(modelBuilder, fp, fp.getFeatureGroupSpec(fgConfig.getName()));
        final ResolvedFeatureGroupConfig popped = modelBuilder.popConfig(fp.gav);
        if(!popped.includedFeatures.isEmpty()) {
            for(Map.Entry<ResolvedFeatureId, FeatureConfig> feature : popped.includedFeatures.entrySet()) {
                if(feature.getValue() != null) {
                    resolvedFeatures |= resolveFeature(modelBuilder, fp, feature.getValue());
                }
            }
        }
        return resolvedFeatures;
    }

    private ResolvedFeatureGroupConfig resolveFeatureGroupConfig(FeaturePackRuntime.Builder fp, FeatureGroupConfig fpConfig) throws ProvisioningException {
        final ResolvedFeatureGroupConfig resolvedFgc = new ResolvedFeatureGroupConfig(fpConfig.getName());
        resolvedFgc.inheritFeatures = fpConfig.isInheritFeatures();
        if(fpConfig.hasExcludedSpecs()) {
            resolvedFgc.excludedSpecs = resolveSpecs(fp, fpConfig.getExcludedSpecs());
        }
        if(fpConfig.hasIncludedSpecs()) {
            resolvedFgc.includedSpecs = resolveSpecs(fp, fpConfig.getIncludedSpecs());
        }
        if(fpConfig.hasExcludedFeatures()) {
            resolvedFgc.excludedFeatures = resolveFeatureSet(fp, fpConfig.getExcludedFeatures());
        }
        if(fpConfig.hasIncludedFeatures()) {
            resolvedFgc.includedFeatures = resolveFeatureMap(fp, fpConfig.getIncludedFeatures());
        }
        return resolvedFgc;
    }

    private Map<ResolvedFeatureId, FeatureConfig> resolveFeatureMap(FeaturePackRuntime.Builder fp, Map<FeatureId, FeatureConfig> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, FeatureConfig> excluded = features.entrySet().iterator().next();
            return Collections.singletonMap(resolveFeatureId(fp, excluded.getKey()), excluded.getValue());
        }
        final Map<ResolvedFeatureId, FeatureConfig> tmp = new HashMap<>(features.size());
        for (Map.Entry<FeatureId, FeatureConfig> excluded : features.entrySet()) {
            tmp.put(resolveFeatureId(fp, excluded.getKey()), excluded.getValue());
        }
        return tmp;
    }

    private Set<ResolvedFeatureId> resolveFeatureSet(FeaturePackRuntime.Builder fp, Set<FeatureId> features) throws ProvisioningException {
        if (features.size() == 1) {
            final FeatureId excludedId = features.iterator().next();
            return Collections.singleton(resolveFeatureId(fp, excludedId));
        }
        final Set<ResolvedFeatureId> tmp = new HashSet<>(features.size());
        for (FeatureId excludedId : features) {
            tmp.add(resolveFeatureId(fp, excludedId));
        }
        return tmp;
    }

    private ResolvedFeatureId resolveFeatureId(FeaturePackRuntime.Builder fp, final FeatureId featureId)
            throws ProvisioningException {
        final SpecId specId = featureId.getSpec();
        return new ResolvedFeatureId(resolveSpecId(specId, fp), featureId.getParams());
    }

    private Set<ResolvedSpecId> resolveSpecs(FeaturePackRuntime.Builder fp, Set<SpecId> specs) throws ProvisioningException {
        if(specs.size() == 1) {
            final SpecId excludedSpecId = specs.iterator().next();
            return Collections.singleton(resolveSpecId(excludedSpecId, fp));
        }
        final Set<ResolvedSpecId> tmp = new HashSet<>(specs.size());
        for (SpecId excludedSpecId : specs) {
            tmp.add(resolveSpecId(excludedSpecId, fp));
        }
        return tmp;
    }

    private boolean processFeatureGroupSpec(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, AbstractFeatureGroup fgSpec) throws ProvisioningException {
        boolean resolvedFeatures = false;
        if(fgSpec.hasExternalDependencies()) {
            for(Map.Entry<String, FeatureGroupSpec> entry : fgSpec.getExternalDependencies().entrySet()) {
                final FeaturePackDependencySpec fpDep = fp.spec.getDependency(entry.getKey());
                if(fpDep == null) {
                    throw new ProvisioningDescriptionException("Unknown feature-pack dependency " + entry.getKey());
                }
                resolvedFeatures |= processFeatureGroupSpec(modelBuilder, getRtBuilder(fpDep.getTarget().getGav()), entry.getValue());
            }
        }
        if(fgSpec.hasLocalDependencies()) {
            for(FeatureGroupConfig nestedFg : fgSpec.getLocalDependencies()) {
                resolvedFeatures |= processFeatureGroupConfig(modelBuilder, fp, nestedFg);
            }
        }
        if(fgSpec.hasFeatures()) {
            for(FeatureConfig fc : fgSpec.getFeatures()) {
                resolvedFeatures |= resolveFeature(modelBuilder, fp, fc);
            }
        }
        return resolvedFeatures;
    }

    private boolean resolveFeature(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, FeatureConfig fc) throws ProvisioningException {

        final SpecId specId = fc.getSpecId();
        final FeaturePackRuntime.Builder targetFp = getRtBuilder(specId, fp);
        final ResolvedFeatureSpec spec = targetFp.getFeatureSpec(specId.getName());

        final ResolvedFeatureId resolvedId = modelBuilder.resolveFeatureId(spec, fc);
        if(modelBuilder.isFilteredOut(spec.id, resolvedId)) {
            return false;
        }

        if(spec.xmlSpec.dependsOnPackages()) {
            try {
                processPackageDeps(targetFp, specId.toString(), spec.xmlSpec);
            } catch(ProvisioningException e) {
                throw new ProvisioningDescriptionException(Errors.resolveFeature(spec.id), e);
            }
        }

        final Set<ResolvedFeatureId> resolvedDeps;
        if(fc.hasDependencies()) {
            final Set<FeatureId> userDeps = fc.getDependencies();
            if(userDeps.size() == 1) {
                resolvedDeps = Collections.singleton(resolveFeatureId(targetFp, userDeps.iterator().next()));
            } else {
                resolvedDeps = new HashSet<>(userDeps.size());
                for(FeatureId featureId : userDeps) {
                    resolvedDeps.add(resolveFeatureId(targetFp, featureId));
                }
            }
        } else {
            resolvedDeps = Collections.emptySet();
        }
        modelBuilder.includeFeature(resolvedId, spec, fc, resolvedDeps);
        if (fc.hasNested()) {
            for (FeatureConfig nested : fc.getNested()) {
                final FeaturePackRuntime.Builder nestedFp = getRtBuilder(nested.getSpecId(), targetFp);
                final String parentRef = nested.getParentRef() == null ? specId.toString() : nested.getParentRef();
                final ResolvedFeatureSpec nestedSpec = nestedFp.getFeatureSpec(nested.getSpecId().getName());
                final FeatureReferenceSpec refSpec = nestedSpec.xmlSpec.getRef(parentRef);
                if (refSpec == null) {
                    throw new ProvisioningDescriptionException("Parent reference " + parentRef + " not found in "
                            + nestedSpec.id);
                }
                for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                    final String paramValue = fc.getParam(refSpec.getTargetParam(i));
                    if (paramValue == null) {
                        throw new ProvisioningDescriptionException(fc + " is missing ID parameter " + refSpec.getTargetParam(i)
                                + " for " + nestedSpec.id);
                    }
                    final String prevValue = nested.putParam(refSpec.getLocalParam(i), paramValue);
                    if (prevValue != null && !prevValue.equals(paramValue)) {
                        throw new ProvisioningDescriptionException("Value " + prevValue + " of ID parameter "
                                + refSpec.getLocalParam(i) + " of " + nestedSpec.id
                                + " conflicts with the corresponding parent ID value " + paramValue);
                    }
                }

                resolveFeature(modelBuilder, nestedFp, nested);
            }
        }
        return true;
    }

    private void popFpConfigs(List<FeaturePackConfig> fpConfigs) throws ProvisioningException {
        for (FeaturePackConfig fpConfig : fpConfigs) {
            final Gav fpGav = fpConfig.getGav();
            final FeaturePackRuntime.Builder fp = getRtBuilder(fpGav);
            final FeaturePackConfig popped = fp.pop();
            if (popped.hasIncludedPackages()) {
                for (PackageConfig pkgConfig : popped.getIncludedPackages()) {
                    if (!fp.isPackageExcluded(pkgConfig.getName())) {
                        resolvePackage(fp, pkgConfig.getName(), pkgConfig.getParameters());
                    } else {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgConfig.getName()));
                    }
                }
            }
        }
    }

    private void pushFpConfig(List<FeaturePackConfig> pushed, FeaturePackConfig fpConfig)
            throws ProvisioningDescriptionException, ProvisioningException, ArtifactResolutionException {
        final FeaturePackRuntime.Builder fp = getRtBuilder(fpConfig.getGav());

        if(fp.isStackEmpty()) {
            fp.push(fpConfig);
            pushed.add(fpConfig);
            return;
        }

        boolean pushDep = false;
        if(fp.isInheritPackages()) {
            if(fpConfig.hasExcludedPackages()) {
                for(String excluded : fpConfig.getExcludedPackages()) {
                    if(!fp.isPackageExcluded(excluded) && !fp.isPackageIncluded(excluded, Collections.emptyList())) {
                        pushDep = true;
                        break;
                    }
                }
            }
            if(!pushDep && fpConfig.hasIncludedPackages()) {
                for(PackageConfig included : fpConfig.getIncludedPackages()) {
                    if(!fp.isPackageIncluded(included.getName(), included.getParameters()) && !fp.isPackageExcluded(included.getName())) {
                        pushDep = true;
                        break;
                    }
                }
            }
        }

        if(!pushDep && fpConfig.hasDefinedConfigs() && fp.isInheritConfigs()) {
            pushDep = true;
        }

        if(pushDep) {
            pushed.add(fpConfig);
            fp.push(fpConfig);
        }
    }

    private ResolvedSpecId resolveSpecId(final SpecId specId, FeaturePackRuntime.Builder defaultFp)
            throws ProvisioningException, ProvisioningDescriptionException, ArtifactResolutionException {
        return new ResolvedSpecId(getRtBuilder(specId, defaultFp).gav, specId.getName());
    }

    private FeaturePackRuntime.Builder getRtBuilder(final SpecId specId, FeaturePackRuntime.Builder originFp)
            throws ProvisioningException, ProvisioningDescriptionException, ArtifactResolutionException {
        FeaturePackRuntime.Builder targetFp = originFp;
        if (specId.getFpDepName() != null) {
            final FeaturePackDependencySpec fpDep = originFp.spec.getDependency(specId.getFpDepName());
            if (fpDep == null) {
                throw new ProvisioningException(Errors.unknownDependencyName(originFp.gav, specId.getFpDepName()));
            }
            targetFp = getRtBuilder(fpDep.getTarget().getGav());
        }
        return targetFp;
    }

    private FeaturePackRuntime.Builder getRtBuilder(ArtifactCoords.Gav gav) throws ProvisioningDescriptionException,
            ProvisioningException, ArtifactResolutionException {
        FeaturePackRuntime.Builder fp = fpRtBuilders.get(gav.toGa());
        if(fp == null) {
            final Path fpDir = LayoutUtils.getFeaturePackDir(layoutDir, gav, false);
            mkdirs(fpDir);

            final Path artifactPath = artifactResolver.resolve(gav.toArtifactCoords());
            try {
                ZipUtils.unzip(artifactPath, fpDir);
            } catch (IOException e) {
                throw new ProvisioningException("Failed to unzip " + artifactPath + " to " + layoutDir, e);
            }

            final Path fpXml = fpDir.resolve(Constants.FEATURE_PACK_XML);
            if(!Files.exists(fpXml)) {
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpXml));
            }

            try(BufferedReader reader = Files.newBufferedReader(fpXml)) {
                fp = FeaturePackRuntime.builder(gav, FeaturePackXmlParser.getInstance().parse(reader), fpDir);
            } catch (IOException | XMLStreamException e) {
                throw new ProvisioningException(Errors.parseXml(fpXml), e);
            }
            fpRtBuilders.put(gav.toGa(), fp);
        } else if(!fp.gav.equals(gav)) {
            throw new ProvisioningException(Errors.featurePackVersionConflict(fp.gav, gav));
        }
        return fp;
    }

    private void resolvePackage(FeaturePackRuntime.Builder fp, final String pkgName, Collection<PackageParameter> params)
            throws ProvisioningException {
        final PackageRuntime.Builder pkgRt = fp.pkgBuilders.get(pkgName);
        if(pkgRt != null) {
            if(!params.isEmpty()) {
                for(PackageParameter param : params) {
                    pkgRt.configBuilder.addParameter(param);
                }
            }
            return;
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

        if(pkg.spec.dependsOnPackages()) {
            try {
                processPackageDeps(fp, pkgName, pkg.spec);
            } catch(ProvisioningException e) {
                throw new ProvisioningDescriptionException(Errors.resolvePackage(fp.gav, pkg.spec.getName()), e);
            }
        }

        if(!params.isEmpty()) {
            for(PackageParameter param : params) {
                pkgConfig.addParameter(param);
            }
        }
        fp.addPackage(pkgName);
    }

    private void processPackageDeps(FeaturePackRuntime.Builder fp, final String pkgName, final PackageDependencies pkg)
            throws ProvisioningException {
        if (pkg.dependsOnLocalPackages()) {
            PackageDependencyGroupSpec localDeps = pkg.getLocalPackageDependencies();
            for (PackageDependencySpec dep : localDeps.getDescriptions()) {
                if(fp.isPackageExcluded(dep.getName())) {
                    if(!dep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, dep.getName()));
                    }
                    continue;
                }
                try {
                    resolvePackage(fp, dep.getName(), dep.getParameters());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        if(pkg.dependsOnExternalPackages()) {
            final Collection<String> depNames = pkg.getPackageDependencySources();
            final List<FeaturePackConfig> pushedConfigs = new ArrayList<>(depNames.size());
            for(String depName : depNames) {
                pushFpConfig(pushedConfigs, fp.spec.getDependency(depName).getTarget());
            }
            for(String depName : depNames) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final FeaturePackRuntime.Builder targetFp = getRtBuilder(depSpec.getTarget().getGav());
                if(targetFp == null) {
                    throw new IllegalStateException(depSpec.getName() + " " + depSpec.getTarget().getGav() + " has not been layed out yet");
                }
                final PackageDependencyGroupSpec pkgDeps = pkg.getExternalPackageDependencies(depName);
                boolean resolvedPackages = false;
                for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                    if(targetFp.isPackageExcluded(pkgDep.getName())) {
                        if(!pkgDep.isOptional()) {
                            throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(targetFp.gav, pkgDep.getName()));
                        }
                        continue;
                    }
                    try {
                        resolvePackage(targetFp, pkgDep.getName(), pkgDep.getParameters());
                        resolvedPackages = true;
                    } catch(ProvisioningDescriptionException e) {
                        if(pkgDep.isOptional()) {
                            continue;
                        } else {
                            throw e;
                        }
                    }
                }
                if(!targetFp.ordered && resolvedPackages) {
                    orderFpRtBuilder(targetFp);
                }
            }
            if (!pushedConfigs.isEmpty()) {
                popFpConfigs(pushedConfigs);
            }
        }
    }

    private void orderFpRtBuilder(final FeaturePackRuntime.Builder fpRtBuilder) {
        this.fpRtBuildersOrdered.add(fpRtBuilder);
        fpRtBuilder.ordered = true;
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }

    private void copyResources(FeaturePackRuntime.Builder fpRtBuilder) throws ProvisioningException {
        // resources should be copied last overriding the dependency resources
        final Path fpResources = fpRtBuilder.dir.resolve(Constants.RESOURCES);
        if(Files.exists(fpResources)) {
            try {
                IoUtils.copy(fpResources, workDir.resolve(Constants.RESOURCES));
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpResources, workDir.resolve(Constants.RESOURCES)), e);
            }
        }

        final Path fpPlugins = fpRtBuilder.dir.resolve(Constants.PLUGINS);
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
}
