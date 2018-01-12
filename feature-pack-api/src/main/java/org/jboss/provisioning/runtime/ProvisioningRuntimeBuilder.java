/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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
import org.jboss.provisioning.ArtifactRepositoryManager;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.DefaultMessageWriter;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ConfigId;
import org.jboss.provisioning.config.ConfigItem;
import org.jboss.provisioning.config.ConfigItemContainer;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.config.FeatureGroupSupport;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageDepsSpec;
import org.jboss.provisioning.spec.SpecId;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlParser;
import org.jboss.provisioning.xml.PackageXmlParser;


/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntimeBuilder {

    public static ProvisioningRuntimeBuilder newInstance() {
        return newInstance(DefaultMessageWriter.getDefaultInstance());
    }

    public static ProvisioningRuntimeBuilder newInstance(final MessageWriter messageWriter) {
        return new ProvisioningRuntimeBuilder(messageWriter);
    }

    private static void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }

    final long startTime;
    String encoding;
    String operation;
    ArtifactRepositoryManager artifactResolver;
    ProvisioningConfig config;
    Path installDir;
    final Path workDir;
    final Path layoutDir;
    Path pluginsDir = null;

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> fpRtBuilders = new HashMap<>();
    private final MessageWriter messageWriter;
    private List<FeaturePackRuntime.Builder> fpRtBuildersOrdered = new ArrayList<>();
    List<ConfigModelResolver> anonymousConfigs = Collections.emptyList();
    Map<String, ConfigModelResolver> nameOnlyConfigs = Collections.emptyMap();
    Map<String, ConfigModelResolver> modelOnlyConfigs = Collections.emptyMap();
    Map<String, Map<String, ConfigModelResolver>> namedModelConfigs = Collections.emptyMap();
    Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;
    Map<String, String> rtParams = Collections.emptyMap();

    private ResolvedFeature parentFeature;

    // this is a stack of model only configs that are resolved and merged after all
    // the named model configs have been resolved. This is done to:
    // 1) avoid resolving model only configs that are not going to get merged;
    // 2) to avoid adding package dependencies of the model only configs that are not merged.
    private List<ConfigModel> modelOnlyConfigSpecs = Collections.emptyList();
    private List<ArtifactCoords.Gav> modelOnlyGavs = Collections.emptyList();

    private FeaturePackRuntime.Builder thisFpOrigin;
    private FeaturePackRuntime.Builder currentFp;
    private ConfigModelResolver configResolver;

    private FpStack fpConfigStack;

    private ProvisioningRuntimeBuilder(final MessageWriter messageWriter) {
        startTime = System.currentTimeMillis();
        workDir = IoUtils.createRandomTmpDir();
        layoutDir = workDir.resolve("layout");
        this.messageWriter = messageWriter;
    }

    public ProvisioningRuntimeBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public ProvisioningRuntimeBuilder setArtifactResolver(ArtifactRepositoryManager artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder setConfig(ProvisioningConfig config) {
        this.config = config;
        return this;
    }

    public ProvisioningRuntimeBuilder setInstallDir(Path installDir) {
        this.installDir = installDir;
        return this;
    }

    public ProvisioningRuntimeBuilder setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    List<ProvisionedConfig> getConfigList() throws ProvisioningDescriptionException {
        final int configsTotal = anonymousConfigs.size() + nameOnlyConfigs.size() + namedModelConfigs.size();
        if(configsTotal == 0) {
            return Collections.emptyList();
        }

        List<ProvisionedConfig> configList = new ArrayList<>(configsTotal);
        if(!anonymousConfigs.isEmpty()) {
            for (ConfigModelResolver config : anonymousConfigs) {
                orderConfig(config, configList, Collections.emptySet());
            }
        }
        if(!nameOnlyConfigs.isEmpty()) {
            for(ConfigModelResolver config : nameOnlyConfigs.values()) {
                if(contains(configList, config.id)) {
                    continue;
                }
                orderConfig(config, configList, Collections.emptySet());
            }
        }
        if(!namedModelConfigs.isEmpty()) {
            for(Map.Entry<String, Map<String, ConfigModelResolver>> entry : namedModelConfigs.entrySet()) {
                for(ConfigModelResolver config : entry.getValue().values()) {
                    if(contains(configList, config.id)) {
                        continue;
                    }
                    orderConfig(config, configList, Collections.emptySet());
                }
            }
        }
        return configList;
    }

    private void orderConfig(ConfigModelResolver config, List<ProvisionedConfig> configList, Set<ConfigId> scheduledIds) throws ProvisioningDescriptionException {
        if(config.configDeps.isEmpty()) {
            configList.add(config);
            return;
        }
        if(!config.id.isAnonymous()) {
            scheduledIds = PmCollections.add(scheduledIds, config.id);
        }
        for(ConfigId depId : config.configDeps.values()) {
            if(scheduledIds.contains(depId) || contains(configList, depId)) {
                continue;
            }

            if(depId.isModelOnly()) {
                final Map<String, ConfigModelResolver> configs = namedModelConfigs.get(depId.getModel());
                if(configs == null) {
                    throw new ProvisioningDescriptionException("Config " + config.id + " has unsatisfied dependency on config " + depId);
                }
                for(ConfigModelResolver dep : configs.values()) {
                    if(contains(configList, dep.id)) {
                        continue;
                    }
                    orderConfig(dep, configList, scheduledIds);
                }
            } else {
                final ConfigModelResolver configMr;
                if (depId.getModel() == null) {
                    configMr = nameOnlyConfigs.get(depId.getName());
                } else {
                    final Map<String, ConfigModelResolver> configs = namedModelConfigs.get(depId.getModel());
                    if(configs == null) {
                        throw new ProvisioningDescriptionException("Config " + config.id + " has unsatisfied dependency on config " + depId);
                    }
                    configMr = configs.get(depId.getName());
                }
                if(configMr == null) {
                    throw new ProvisioningDescriptionException("Config " + config.id + " has unsatisfied dependency on config " + depId);
                }
                if(contains(configList, configMr.id)) {
                    continue;
                }
                orderConfig(configMr, configList, scheduledIds);
            }
        }
        scheduledIds = PmCollections.remove(scheduledIds, config.id);
        configList.add(config);
    }

    private boolean contains(List<ProvisionedConfig> configList, ConfigId depId) {
        int i = 0;
        while(i < configList.size()) {
            if(((ConfigModelResolver)configList.get(i++)).id.equals(depId)) {
                return true;
            }
        }
        return false;
    }

    public ProvisioningRuntime build() throws ProvisioningException {

        fpConfigStack = new FpStack(config);

        // the configs are processed in the reverse order to correctly implement config overwrites

        List<ConfigModelResolver> fpConfigResolvers = Collections.emptyList();
        for(int i = config.getDefinedConfigs().size() - 1; i >= 0; --i) {
            final ConfigModel config = this.config.getDefinedConfigs().get(i);
            if(fpConfigStack.isFilteredOut(config.getId(), true)) {
                continue;
            }
            configResolver = getConfigResolver(config.getId());
            configResolver.pushConfig(config);
            fpConfigResolvers = PmCollections.add(fpConfigResolvers, configResolver);
        }

        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePackDeps();
        boolean extendedStackLevel = false;
        for (FeaturePackConfig fpConfig : fpConfigs) {
            extendedStackLevel |= fpConfigStack.push(fpConfig, extendedStackLevel);
        }
        while(fpConfigStack.hasNext()) {
            processFpConfig(fpConfigStack.next());
        }

        if(extendedStackLevel) {
            fpConfigStack.popLevel();
        }

        for(int i = fpConfigResolvers.size() - 1; i>= 0; --i) {
            final ConfigModelResolver configResolver = fpConfigResolvers.get(i);
            final ConfigModel config = configResolver.popConfig();
            if(config.getId().isModelOnly()) {
                recordModelOnlyConfig(null, config);
                continue;
            }
            processConfig(configResolver, config);
        }

        resolveConfigs();

        switch(fpRtBuildersOrdered.size()) {
            case 0: {
                fpRuntimes = Collections.emptyMap();
                break;
            }
            case 1: {
                final FeaturePackRuntime.Builder builder = fpRtBuildersOrdered.get(0);
                copyResources(builder);
                fpRuntimes = Collections.singletonMap(builder.gav, builder.build());
                break;
            }
            default: {
                fpRuntimes = new LinkedHashMap<>(fpRtBuildersOrdered.size());
                for(FeaturePackRuntime.Builder builder : fpRtBuildersOrdered) {
                    copyResources(builder);
                    fpRuntimes.put(builder.gav, builder.build());
                }
                fpRuntimes = Collections.unmodifiableMap(fpRuntimes);
            }
        }

        return new ProvisioningRuntime(this, messageWriter);
    }

    private void resolveConfigs() throws ProvisioningException {
        if(!anonymousConfigs.isEmpty()) {
            for(ConfigModelResolver config : anonymousConfigs) {
                config.resolve();
            }
        }
        if(!nameOnlyConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelResolver> entry : nameOnlyConfigs.entrySet()) {
                entry.getValue().resolve();
            }
        }

        if(!modelOnlyConfigSpecs.isEmpty()) {
            for(int i = 0; i < modelOnlyConfigSpecs.size(); ++i) {
                final ConfigModel modelOnlySpec = modelOnlyConfigSpecs.get(i);
                if(!namedModelConfigs.containsKey(modelOnlySpec.getModel())) {
                    continue;
                }
                fpConfigStack.activateConfigStack(i);
                final ArtifactCoords.Gav fpGav = modelOnlyGavs.get(i);
                thisFpOrigin = fpGav == null ? null : loadFpBuilder(modelOnlyGavs.get(i));
                currentFp = thisFpOrigin;
                if(processConfig(getConfigResolver(modelOnlySpec.getId()), modelOnlySpec) && currentFp != null && !currentFp.ordered) {
                    orderFpRtBuilder(currentFp);
                }
            }
        }
        if(!modelOnlyConfigs.isEmpty()) {
            final Iterator<Map.Entry<String, ConfigModelResolver>> i = modelOnlyConfigs.entrySet().iterator();
            if(modelOnlyConfigs.size() == 1) {
                final Map.Entry<String, ConfigModelResolver> entry = i.next();
                final Map<String, ConfigModelResolver> targetConfigs = namedModelConfigs.get(entry.getKey());
                if (targetConfigs != null) {
                    for (Map.Entry<String, ConfigModelResolver> targetConfig : targetConfigs.entrySet()) {
                        targetConfig.getValue().merge(entry.getValue());
                    }
                }
            } else {
                while (i.hasNext()) {
                    final Map.Entry<String, ConfigModelResolver> entry = i.next();
                    final Map<String, ConfigModelResolver> targetConfigs = namedModelConfigs.get(entry.getKey());
                    if (targetConfigs != null) {
                        for (Map.Entry<String, ConfigModelResolver> targetConfig : targetConfigs.entrySet()) {
                            targetConfig.getValue().merge(entry.getValue());
                        }
                    }
                }
            }
            modelOnlyConfigs = Collections.emptyMap();
        }

        for(Map<String, ConfigModelResolver> configMap : namedModelConfigs.values()) {
            for(Map.Entry<String, ConfigModelResolver> configEntry : configMap.entrySet()) {
                configEntry.getValue().resolve();
            }
        }
    }

    private void processFpConfig(FeaturePackConfig fpConfig) throws ProvisioningException {
        final FeaturePackRuntime.Builder parentFp = currentFp;
        thisFpOrigin = loadFpBuilder(fpConfig.getGav());
        currentFp = thisFpOrigin;

        List<ConfigModelResolver> fpConfigResolvers = Collections.emptyList();
        for(int i = fpConfig.getDefinedConfigs().size() - 1; i >= 0; --i) {
            final ConfigModel config = fpConfig.getDefinedConfigs().get(i);
            if(fpConfigStack.isFilteredOut(config.getId(), true)) {
                continue;
            }
            configResolver = getConfigResolver(config.getId());
            configResolver.pushConfig(config);
            fpConfigResolvers = PmCollections.add(fpConfigResolvers, configResolver);
        }

        List<ConfigModelResolver> specResolvers = Collections.emptyList();
        for(int i = currentFp.spec.getDefinedConfigs().size() - 1; i >= 0; --i) {
            final ConfigModel config = currentFp.spec.getDefinedConfigs().get(i);
            if(fpConfigStack.isFilteredOut(config.getId(), false)) {
                continue;
            }
            configResolver = getConfigResolver(config.getId());
            configResolver.pushConfig(config);
            specResolvers = PmCollections.add(specResolvers, configResolver);
        }

        configResolver = null;

        boolean extendedStackLevel = false;
        if(currentFp.spec.hasFeaturePackDeps()) {
            final Collection<FeaturePackConfig> fpDeps = currentFp.spec.getFeaturePackDeps();
            for (FeaturePackConfig fpDep : fpDeps) {
                extendedStackLevel |= fpConfigStack.push(fpDep, extendedStackLevel);
            }
            if (extendedStackLevel) {
                while(fpConfigStack.hasNext()) {
                    processFpConfig(fpConfigStack.next());
                }
            }
        }

        boolean contributed = false;

        for(int i = specResolvers.size() - 1; i >= 0; --i) {
            final ConfigModelResolver configResolver = specResolvers.get(i);
            final ConfigModel config = configResolver.popConfig();
            if(config.getId().isModelOnly()) {
                recordModelOnlyConfig(fpConfig.getGav(), config);
                continue;
            }
            contributed |= processConfig(configResolver, config);
        }

        if(fpConfig.isInheritPackages()) {
            for(String packageName : currentFp.spec.getDefaultPackageNames()) {
                if(!fpConfigStack.isPackageExcluded(currentFp.gav, packageName)) {
                    resolvePackage(packageName);
                    contributed = true;
                }
            }
        }
        if (fpConfig.hasIncludedPackages()) {
            for (PackageConfig pkgConfig : fpConfig.getIncludedPackages()) {
                if (!fpConfigStack.isPackageExcluded(currentFp.gav, pkgConfig.getName())) {
                    resolvePackage(pkgConfig.getName());
                    contributed = true;
                } else {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(currentFp.gav, pkgConfig.getName()));
                }
            }
        }

        for(int i = fpConfigResolvers.size() - 1; i>= 0; --i) {
            final ConfigModelResolver configResolver = fpConfigResolvers.get(i);
            final ConfigModel config = configResolver.popConfig();
            if(config.getId().isModelOnly()) {
                recordModelOnlyConfig(fpConfig.getGav(), config);
                continue;
            }
            contributed |= processConfig(configResolver, config);
        }

        if (extendedStackLevel) {
            fpConfigStack.popLevel();
        }

        if(!currentFp.ordered && contributed) {
            orderFpRtBuilder(currentFp);
        }

        this.thisFpOrigin = parentFp;
        this.currentFp = parentFp;
    }

    private void recordModelOnlyConfig(ArtifactCoords.Gav gav, ConfigModel config) {
        modelOnlyConfigSpecs = PmCollections.add(modelOnlyConfigSpecs, config);
        modelOnlyGavs = PmCollections.add(modelOnlyGavs, gav);
        fpConfigStack.recordStack();
    }

    private boolean processConfig(ConfigModelResolver configResolver, ConfigModel config) throws ProvisioningException {
        if(this.configResolver != null) {
            throw new IllegalStateException();
        }
        this.configResolver = configResolver;
        configResolver.overwriteProps(config.getProperties());
        configResolver.overwriteConfigDeps(config.getConfigDeps());
        try {
            if(config.hasPackageDeps()) {
                processPackageDeps(config);
            }
            processConfigItemContainer(config);
            this.configResolver = null;
            return true; // the config may be empty but it may tigger model-only merge into it
        } catch (ProvisioningException e) {
            throw new ProvisioningException(Errors.failedToResolveConfigSpec(config.getModel(), config.getName()), e);
        }
    }

    private ConfigModelResolver getConfigResolver(ConfigId config) throws ProvisioningException {
        if(config.getModel() == null) {
            if(config.getName() == null) {
                final ConfigModelResolver modelBuilder = ConfigModelResolver.forId(this, config);
                anonymousConfigs = PmCollections.add(anonymousConfigs, modelBuilder);
                return modelBuilder;
            }
            ConfigModelResolver modelBuilder = nameOnlyConfigs.get(config.getName());
            if(modelBuilder == null) {
                modelBuilder = ConfigModelResolver.forId(this, config);
                nameOnlyConfigs = PmCollections.putLinked(nameOnlyConfigs, config.getName(), modelBuilder);
            }
            return modelBuilder;
        }
        if(config.getName() == null) {
            ConfigModelResolver modelBuilder = modelOnlyConfigs.get(config.getModel());
            if(modelBuilder == null) {
                modelBuilder = ConfigModelResolver.forId(this, config);
                modelOnlyConfigs = PmCollections.putLinked(modelOnlyConfigs, config.getModel(), modelBuilder);
            }
            return modelBuilder;
        }

        Map<String, ConfigModelResolver> namedConfigs = namedModelConfigs.get(config.getModel());
        if(namedConfigs == null) {
            final ConfigModelResolver modelBuilder = ConfigModelResolver.forId(this, config);
            namedConfigs = Collections.singletonMap(config.getName(), modelBuilder);
            namedModelConfigs = PmCollections.putLinked(namedModelConfigs, config.getModel(), namedConfigs);
            return modelBuilder;
        }

        ConfigModelResolver modelBuilder = namedConfigs.get(config.getName());
        if (modelBuilder == null) {
            if (namedConfigs.size() == 1) {
                namedConfigs = new LinkedHashMap<>(namedConfigs);
                if (namedModelConfigs.size() == 1) {
                    namedModelConfigs = new LinkedHashMap<>(namedModelConfigs);
                }
                namedModelConfigs.put(config.getModel(), namedConfigs);
            }
            modelBuilder = ConfigModelResolver.forId(this, config);
            namedConfigs.put(config.getName(), modelBuilder);
        }
        return modelBuilder;
    }

    private boolean processFeatureGroup(FeatureGroupSupport includedFg)
            throws ProvisioningException {

        final boolean pushed = configResolver.pushGroup(includedFg);

        final FeatureGroupSupport originalFg = currentFp.getFeatureGroupSpec(includedFg.getName());
        if(originalFg.hasPackageDeps()) {
            processPackageDeps(originalFg);
        }

        if (!pushed) {
            return false;
        }

        configResolver.startGroup();
        boolean resolvedFeatures = processConfigItemContainer(originalFg);
        resolvedFeatures |= configResolver.popGroup();
        configResolver.endGroup();

        if(includedFg.hasItems()) {
            resolvedFeatures |= processConfigItemContainer(includedFg);
        }
        return resolvedFeatures;
    }

    ResolvedFeatureGroupConfig resolveFg(String origin, FeatureGroupSupport fg) throws ProvisioningException {
        final FeaturePackRuntime.Builder originalFp = currentFp;
        if(origin != null) {
            currentFp = getFpDep(origin);
        } else if(currentFp == null) {
            return null;
        }
        final ResolvedFeatureGroupConfig resolvedFgConfig = resolveFeatureGroupConfig(fg);
        currentFp = originalFp;
        return resolvedFgConfig;
    }

    boolean processIncludedFeatures(final List<ResolvedFeatureGroupConfig> pushedConfigs)
            throws ProvisioningException {
        boolean resolvedFeatures = false;
        final FeaturePackRuntime.Builder originalFp = currentFp;
        for(ResolvedFeatureGroupConfig pushedFgConfig : pushedConfigs) {
            currentFp = loadFpBuilder(pushedFgConfig.gav);
            if (pushedFgConfig.includedFeatures.isEmpty()) {
                continue;
            }
            for (Map.Entry<ResolvedFeatureId, FeatureConfig> feature : pushedFgConfig.includedFeatures.entrySet()) {
                final FeatureConfig includedFc = feature.getValue();
                if (includedFc != null && includedFc.hasParams()) {
                    final ResolvedFeatureId includedId = feature.getKey();
                    if (pushedFgConfig.configResolver.isFilteredOut(includedId.specId, includedId)) {
                        continue;
                    }
                    // make sure the included ID is in fact present on the feature group branch
                    if (!pushedFgConfig.configResolver.includes(includedId)) {
                        throw new ProvisioningException(Errors.featureNotInScope(includedId,
                                pushedFgConfig.fg.getId() == null ? "'anonymous'" : pushedFgConfig.fg.getId().toString(), currentFp.gav));
                    }
                    resolvedFeatures |= resolveFeature(pushedFgConfig.configResolver, includedFc);
                }
            }
        }
        currentFp = originalFp;
        return resolvedFeatures;
    }

    private ResolvedFeatureGroupConfig resolveFeatureGroupConfig(FeatureGroupSupport fg)
            throws ProvisioningException {
        final ResolvedFeatureGroupConfig resolvedFgc = new ResolvedFeatureGroupConfig(configResolver, fg, currentFp.gav);
        resolvedFgc.inheritFeatures = fg.isInheritFeatures();
        if(fg.hasExcludedSpecs()) {
            resolvedFgc.excludedSpecs = resolveSpecIds(currentFp.gav, fg.getExcludedSpecs());
        }
        if(fg.hasIncludedSpecs()) {
            resolvedFgc.includedSpecs = resolveSpecIds(currentFp.gav, fg.getIncludedSpecs());
        }
        if(fg.hasExcludedFeatures()) {
            resolvedFgc.excludedFeatures = resolveExcludedIds(fg.getExcludedFeatures());
        }
        if(fg.hasIncludedFeatures()) {
            resolvedFgc.includedFeatures = resolveIncludedIds(fg.getIncludedFeatures());
        }
        return resolvedFgc;
    }

    private Map<ResolvedFeatureId, FeatureConfig> resolveIncludedIds(Map<FeatureId, FeatureConfig> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, FeatureConfig> included = features.entrySet().iterator().next();
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = currentFp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                return Collections.singletonMap(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams()), fc);
            }
            return Collections.singletonMap(resolvedSpec.resolveFeatureId(fc.getParams()), fc);
        }
        final Map<ResolvedFeatureId, FeatureConfig> tmp = new HashMap<>(features.size());
        for (Map.Entry<FeatureId, FeatureConfig> included : features.entrySet()) {
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = currentFp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                tmp.put(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams()), fc);
            } else {
                tmp.put(resolvedSpec.resolveFeatureId(fc.getParams()), fc);
            }
        }
        return tmp;
    }

    private Set<ResolvedFeatureId> resolveExcludedIds(Map<FeatureId, String> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, String> excluded = features.entrySet().iterator().next();
            final FeatureId excludedId = excluded.getKey();
            final ResolvedFeatureSpec resolvedSpec = currentFp.getFeatureSpec(excludedId.getSpec().getName());
            if(parentFeature != null) {
                return Collections.singleton(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, excluded.getValue(), excludedId.getParams()));
            }
            return Collections.singleton(resolvedSpec.resolveFeatureId(excludedId.getParams()));
        }
        final Set<ResolvedFeatureId> tmp = new HashSet<>(features.size());
        for (Map.Entry<FeatureId, String> excluded : features.entrySet()) {
            final FeatureId excludedId = excluded.getKey();
            final ResolvedFeatureSpec resolvedSpec = currentFp.getFeatureSpec(excludedId.getSpec().getName());
            if(parentFeature != null) {
                tmp.add(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, excluded.getValue(), excludedId.getParams()));
            } else {
                tmp.add(resolvedSpec.resolveFeatureId(excludedId.getParams()));
            }
        }
        return tmp;
    }

    private static Set<ResolvedSpecId> resolveSpecIds(ArtifactCoords.Gav gav, Set<SpecId> specs) throws ProvisioningException {
        if(specs.size() == 1) {
            final SpecId specId = specs.iterator().next();
            return Collections.singleton(new ResolvedSpecId(gav, specId.getName()));
        }
        final Set<ResolvedSpecId> tmp = new HashSet<>(specs.size());
        for (SpecId specId : specs) {
            tmp.add(new ResolvedSpecId(gav, specId.getName()));
        }
        return tmp;
    }

    private boolean processConfigItemContainer(ConfigItemContainer ciContainer) throws ProvisioningException {
        boolean resolvedFeatures = false;
        final FeaturePackRuntime.Builder prevFpOrigin = thisFpOrigin;
        if(ciContainer.isResetFeaturePackOrigin()) {
            thisFpOrigin = currentFp;
        }
        if(ciContainer.hasItems()) {
            for(ConfigItem item : ciContainer.getItems()) {
                final FeaturePackRuntime.Builder originalFp = currentFp;
                currentFp = item.getFpDep() == null ? currentFp : getFpDep(item.getFpDep());
                try {
                    if (item.isGroup()) {
                        final FeatureGroup nestedFg = (FeatureGroup) item;
                        resolvedFeatures |= processFeatureGroup(nestedFg);
                    } else if(currentFp != null) {
                        resolvedFeatures |= resolveFeature(configResolver, (FeatureConfig) item);
                    } else {
                        throw new ProvisioningDescriptionException(Errors.featureOriginNotSpecified(configResolver.id, (FeatureConfig) item));
                    }
                } catch (ProvisioningException e) {
                    if(currentFp == null) {
                        throw e;
                    }
                    throw new ProvisioningException(item.isGroup() ?
                            Errors.failedToProcess(currentFp.gav, ((FeatureGroup)item).getName()) : Errors.failedToProcess(currentFp.gav, (FeatureConfig)item),
                            e);
                }
                currentFp = originalFp;
            }
        }
        thisFpOrigin = prevFpOrigin;
        return resolvedFeatures;
    }

    FeaturePackRuntime.Builder getFpDep(final String depName) throws ProvisioningException {
        if(Constants.THIS.equals(depName)) {
            if(thisFpOrigin == null) {
                throw new ProvisioningException("Feature-pack reference 'this' cannot be used in the current context.");
            }
            return thisFpOrigin;
        }
        final ArtifactCoords.Gav depGav = currentFp == null ? config.getFeaturePackDep(depName).getGav() : currentFp.spec.getFeaturePackDep(depName).getGav();
        return loadFpBuilder(depGav);
    }

    private boolean resolveFeature(ConfigModelResolver modelBuilder, FeatureConfig fc) throws ProvisioningException {
        final ResolvedFeatureSpec spec = currentFp.getFeatureSpec(fc.getSpecId().getName());
        final ResolvedFeatureId resolvedId = parentFeature == null ? spec.resolveFeatureId(fc.getParams()) : spec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams());
        if(modelBuilder.isFilteredOut(spec.id, resolvedId)) {
            return false;
        }

        final ResolvedFeature myParent = parentFeature;
        parentFeature = resolveFeatureDepsAndRefs(modelBuilder, spec, resolvedId,
                spec.resolveNonIdParams(parentFeature == null ? null : parentFeature.id, fc.getParentRef(), fc.getParams()), fc.getFeatureDeps());
        if(fc.hasUnsetParams()) {
            parentFeature.unsetAllParams(fc.getUnsetParams(), true);
        }
        if(fc.hasResetParams()) {
            parentFeature.resetAllParams(fc.getResetParams());
        }
        processConfigItemContainer(fc);
        parentFeature = myParent;
        return true;
    }

    private ResolvedFeature resolveFeatureDepsAndRefs(ConfigModelResolver modelBuilder,
            final ResolvedFeatureSpec spec, final ResolvedFeatureId resolvedId, Map<String, Object> resolvedParams,
            Collection<FeatureDependencySpec> featureDeps)
            throws ProvisioningException {

        if(spec.xmlSpec.hasPackageDeps()) {
            processPackageDeps(spec.xmlSpec);
        }

        final ResolvedFeature resolvedFeature = modelBuilder.includeFeature(resolvedId, spec, resolvedParams, resolveFeatureDeps(modelBuilder, featureDeps, spec));

        if(spec.xmlSpec.hasFeatureRefs()) {
            final ResolvedFeature myParent = parentFeature;
            parentFeature = resolvedFeature;
            for(FeatureReferenceSpec refSpec : spec.xmlSpec.getFeatureRefs()) {
                if(!refSpec.isInclude()) {
                    continue;
                }
                final FeaturePackRuntime.Builder originalFp = currentFp;
                currentFp = refSpec.getDependency() == null ? currentFp : getFpDep(refSpec.getDependency());
                final ResolvedFeatureSpec refResolvedSpec = currentFp.getFeatureSpec(refSpec.getFeature().getName());
                final List<ResolvedFeatureId> refIds = spec.resolveRefId(parentFeature, refSpec, refResolvedSpec);
                if(!refIds.isEmpty()) {
                    for (ResolvedFeatureId refId : refIds) {
                        if (modelBuilder.includes(refId) || modelBuilder.isFilteredOut(refId.specId, refId)) {
                            continue;
                        }
                        resolveFeatureDepsAndRefs(modelBuilder, refResolvedSpec, refId, Collections.emptyMap(), Collections.emptyList());
                    }
                }
                currentFp = originalFp;
            }
            parentFeature = myParent;
        }
        return resolvedFeature;
    }

    private Map<ResolvedFeatureId, FeatureDependencySpec> resolveFeatureDeps(ConfigModelResolver modelBuilder,
            Collection<FeatureDependencySpec> featureDeps, final ResolvedFeatureSpec spec)
            throws ProvisioningException {
        Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps = spec.resolveFeatureDeps(this, featureDeps);
        if(!resolvedDeps.isEmpty()) {
            for(Map.Entry<ResolvedFeatureId, FeatureDependencySpec> dep : resolvedDeps.entrySet()) {
                if(!dep.getValue().isInclude()) {
                    continue;
                }
                final ResolvedFeatureId depId = dep.getKey();
                if(modelBuilder.includes(depId) || modelBuilder.isFilteredOut(depId.specId, depId)) {
                    continue;
                }
                final FeatureDependencySpec depSpec = dep.getValue();
                final FeaturePackRuntime.Builder originalFp = currentFp;
                currentFp = depSpec.getDependency() == null ? currentFp : getFpDep(depSpec.getDependency());
                resolveFeatureDepsAndRefs(modelBuilder, currentFp.getFeatureSpec(depId.getSpecId().getName()), depId, Collections.emptyMap(), Collections.emptyList());
                currentFp = originalFp;
            }
        }
        return resolvedDeps;
    }

    FeaturePackRuntime.Builder getFpBuilder(ArtifactCoords.Gav gav) throws ProvisioningDescriptionException {
        final FeaturePackRuntime.Builder fpRtBuilder = fpRtBuilders.get(gav.toGa());
        if(fpRtBuilder == null) {
            throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(gav));
        }
        return fpRtBuilder;
    }

    private FeaturePackRuntime.Builder loadFpBuilder(ArtifactCoords.Gav gav) throws ProvisioningException {
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

    private void resolvePackage(final String pkgName)
            throws ProvisioningException {
        final PackageRuntime.Builder pkgRt = currentFp.pkgBuilders.get(pkgName);
        if(pkgRt != null) {
            return;
        }

        final PackageRuntime.Builder pkg = currentFp.newPackage(pkgName, LayoutUtils.getPackageDir(currentFp.dir, pkgName, false));
        if(!Files.exists(pkg.dir)) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(currentFp.gav, pkgName));
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

        if(pkg.spec.hasPackageDeps()) {
            try {
                processPackageDeps(pkg.spec);
            } catch(ProvisioningException e) {
                throw new ProvisioningDescriptionException(Errors.resolvePackage(currentFp.gav, pkg.spec.getName()), e);
            }
        }
        currentFp.addPackage(pkgName);
    }

    private void processPackageDeps(final PackageDepsSpec pkgDeps)
            throws ProvisioningException {
        if (pkgDeps.hasLocalPackageDeps()) {
            for (PackageDependencySpec dep : pkgDeps.getLocalPackageDeps()) {
                if(fpConfigStack.isPackageExcluded(currentFp.gav, dep.getName())) {
                    if(!dep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(currentFp.gav, dep.getName()));
                    }
                    continue;
                }
                try {
                    resolvePackage(dep.getName());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        if(!pkgDeps.hasExternalPackageDeps()) {
            return;
        }
        for (String depName : pkgDeps.getExternalPackageSources()) {
            final FeaturePackRuntime.Builder originalFp = currentFp;
            currentFp = this.getFpDep(depName);
            boolean resolvedPackages = false;
            for (PackageDependencySpec pkgDep : pkgDeps.getExternalPackageDeps(depName)) {
                if (fpConfigStack.isPackageExcluded(currentFp.gav, pkgDep.getName())) {
                    if (!pkgDep.isOptional()) {
                        final ArtifactCoords.Gav originGav = currentFp.gav;
                        currentFp = originalFp;
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(originGav, pkgDep.getName()));
                    }
                    continue;
                }
                try {
                    resolvePackage(pkgDep.getName());
                    resolvedPackages = true;
                } catch (ProvisioningDescriptionException e) {
                    if (pkgDep.isOptional()) {
                        continue;
                    } else {
                        currentFp = originalFp;
                        throw e;
                    }
                }
            }
            if (!currentFp.ordered && resolvedPackages) {
                orderFpRtBuilder(currentFp);
            }
            currentFp = originalFp;
        }
    }

    private void orderFpRtBuilder(final FeaturePackRuntime.Builder fpRtBuilder) {
        this.fpRtBuildersOrdered.add(fpRtBuilder);
        fpRtBuilder.ordered = true;
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

    public ProvisioningRuntimeBuilder addParameter(String name, String param) {
        rtParams = PmCollections.put(rtParams, name, param);
        return this;
    }

    public ProvisioningRuntimeBuilder addAllParameters(Map<String, String> params) {
        for(Map.Entry<String, String> param : params.entrySet()) {
            addParameter(param.getKey(), param.getValue());
        }
        return this;
    }
}
