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
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageDepsSpec;
import org.jboss.provisioning.spec.SpecId;
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
    private FeaturePackRuntime.Builder fpOrigin;
    Map<String, String> rtParams = Collections.emptyMap();

    private ResolvedFeature parentFeature;

    // this is a stack of model only configs that are resolved and merged after all
    // the named model configs have been resolved. This is done to:
    // 1) avoid resolving model only configs that are not going to get merged;
    // 2) to avoid adding package dependencies of the model only configs that are not merged.
    private List<ConfigModel> modelOnlyConfigSpecs = Collections.emptyList();
    private List<FeaturePackConfig> modelOnlyFpConfigs = Collections.emptyList();

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

    public ProvisioningRuntime build() throws ProvisioningException {

        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePacks();
        for (FeaturePackConfig fpConfig : fpConfigs) {
            loadFpBuilder(fpConfig.getGav()).push(fpConfig);
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

    private void buildConfigs() throws ProvisioningException {
        if(!anonymousConfigs.isEmpty()) {
            for(ConfigModelResolver config : anonymousConfigs) {
                config.build(this);
            }
        }
        if(!nameOnlyConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelResolver> entry : nameOnlyConfigs.entrySet()) {
                entry.getValue().build(this);
            }
        }

        if(!modelOnlyConfigSpecs.isEmpty()) {
            for(int i = 0; i < modelOnlyConfigSpecs.size(); ++i) {
                final ConfigModel modelOnlySpec = modelOnlyConfigSpecs.get(i);
                if(!namedModelConfigs.containsKey(modelOnlySpec.getModel())) {
                    continue;
                }
                for(Map.Entry<ArtifactCoords.Ga, FeaturePackRuntime.Builder> entry : fpRtBuilders.entrySet()) {
                    entry.getValue().activateConfigStack(i);
                }
                final FeaturePackConfig fpConfig = this.modelOnlyFpConfigs.get(i);
                final FeaturePackRuntime.Builder fp = loadFpBuilder(fpConfig.getGav());
                if(includeConfig(fp, fpConfig, modelOnlySpec) && !fp.ordered) {
                    orderFpRtBuilder(fp);
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
                configEntry.getValue().build(this);
            }
        }
    }

    private void processFpConfig(FeaturePackConfig fpConfig) throws ProvisioningException {
        final FeaturePackRuntime.Builder fp = loadFpBuilder(fpConfig.getGav());

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

        this.fpOrigin = fp;

        boolean contributed = false;

        for(ConfigModel config : fp.spec.getConfigs()) {
            final ConfigId configId = config.getId();
            if(configId.isAnonymous()) {
                if(fpConfig.isInheritConfigs()) {
                    contributed |= includeConfig(fp, fpConfig, config);
                }
            } else if(configId.isModelOnly()) {
                if(fpConfig.isInheritModelOnlyConfigs()) {
                    if(!fp.isModelOnlyConfigExcluded(configId)) {
                        recordModelOnlyConfig(fpConfig, config);
                    }
                } else if(fp.isModelOnlyConfigIncluded(configId)) {
                    recordModelOnlyConfig(fpConfig, config);
                }
            } else if (fpConfig.isInheritConfigs()) {
                if (!fp.isConfigExcluded(configId)) {
                    contributed |= includeConfig(fp, fpConfig, config);
                }
            } else if (fp.isConfigIncluded(configId)) {
                contributed |= includeConfig(fp, fpConfig, config);
            }
        }

        if(fpConfig.isInheritPackages()) {
            for(String packageName : fp.spec.getDefaultPackageNames()) {
                if(!fp.isPackageExcluded(packageName)) {
                    resolvePackage(fp, packageName);
                    contributed = true;
                }
            }
        }
        if (fpConfig.hasIncludedPackages()) {
            for (PackageConfig pkgConfig : fpConfig.getIncludedPackages()) {
                if (!fp.isPackageExcluded(pkgConfig.getName())) {
                    resolvePackage(fp, pkgConfig.getName());
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
            for (ConfigModel config : fpConfig.getDefinedConfigs()) {
                contributed |= processConfigSpec(fp, config);
            }
        }

        if(!fp.ordered && contributed) {
            orderFpRtBuilder(fp);
        }
    }

    private void recordModelOnlyConfig(FeaturePackConfig fpConfig, ConfigModel configSpec) {
        modelOnlyConfigSpecs = PmCollections.add(modelOnlyConfigSpecs, configSpec);
        modelOnlyFpConfigs = PmCollections.add(modelOnlyFpConfigs, fpConfig);
        for(Map.Entry<ArtifactCoords.Ga, FeaturePackRuntime.Builder> entry : this.fpRtBuilders.entrySet()) {
            entry.getValue().recordConfigStack();
        }
    }

    private boolean includeConfig(FeaturePackRuntime.Builder fp, FeaturePackConfig fpConfig, ConfigModel config) throws ProvisioningException {
        final ConfigModel includedConfig = fpConfig.getIncludedConfig(config.getId());
        if(includedConfig != null) {
            return includeConfig(fp, includedConfig, config);
        }
        return processConfigSpec(fp, config);
    }

    private boolean includeConfig(FeaturePackRuntime.Builder fp, ConfigModel includedConfig, ConfigModel config) throws ProvisioningException {
        final ConfigModelResolver configBuilder = getConfigModelBuilder(config.getId());
        configBuilder.overwriteProps(config.getProperties());
        configBuilder.overwriteProps(includedConfig.getProperties());
        return processFeatureGroupConfig(configBuilder, fp, includedConfig, config);
    }

    private boolean processConfigSpec(FeaturePackRuntime.Builder fp, ConfigModel config) throws ProvisioningException {
        final ConfigModelResolver configBuilder = getConfigModelBuilder(config.getId());
        configBuilder.overwriteProps(config.getProperties());
        try {
            if(config.hasPackageDeps()) {
                processPackageDeps(fp, config);
            }
            return processConfigItemContainer(configBuilder, fp, config);
        } catch (ProvisioningException e) {
            throw new ProvisioningException(Errors.failedToResolveConfigSpec(config.getModel(), config.getName()), e);
        }
    }

    private ConfigModelResolver getConfigModelBuilder(ConfigId config) {
        if(config.getModel() == null) {
            if(config.getName() == null) {
                final ConfigModelResolver modelBuilder = ConfigModelResolver.anonymous();
                anonymousConfigs = PmCollections.add(anonymousConfigs, modelBuilder);
                return modelBuilder;
            }
            ConfigModelResolver modelBuilder = nameOnlyConfigs.get(config.getName());
            if(modelBuilder == null) {
                modelBuilder = ConfigModelResolver.forName(config.getName());
                nameOnlyConfigs = PmCollections.putLinked(nameOnlyConfigs, config.getName(), modelBuilder);
            }
            return modelBuilder;
        }
        if(config.getName() == null) {
            ConfigModelResolver modelBuilder = modelOnlyConfigs.get(config.getModel());
            if(modelBuilder == null) {
                modelBuilder = ConfigModelResolver.forModel(config.getModel());
                modelOnlyConfigs = PmCollections.putLinked(modelOnlyConfigs, config.getModel(), modelBuilder);
            }
            return modelBuilder;
        }

        Map<String, ConfigModelResolver> namedConfigs = namedModelConfigs.get(config.getModel());
        if(namedConfigs == null) {
            final ConfigModelResolver modelBuilder = ConfigModelResolver.forConfig(config.getModel(), config.getName());
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
            modelBuilder = ConfigModelResolver.forConfig(config.getModel(), config.getName());
            namedConfigs.put(config.getName(), modelBuilder);
        }
        return modelBuilder;
    }

    private boolean processFeatureGroupConfig(ConfigModelResolver modelBuilder, FeaturePackRuntime.Builder fp, FeatureGroupSupport fgConfig, final FeatureGroupSupport fgSpec)
            throws ProvisioningException {
        List<FeaturePackRuntime.Builder> pushedConfigs = Collections.emptyList();
        if(fgConfig.hasExternalFeatureGroups()) {
            for(Map.Entry<String, FeatureGroupSupport> entry : fgConfig.getExternalFeatureGroups().entrySet()) {
                final FeaturePackRuntime.Builder depFpRt = getFpDependency(fp, entry.getKey());
                if(modelBuilder.pushConfig(depFpRt.gav, resolveFeatureGroupConfig(depFpRt, entry.getValue()))) {
                    pushedConfigs = PmCollections.add(pushedConfigs, depFpRt);
                }
            }
        }
        if(modelBuilder.pushConfig(fp.gav, resolveFeatureGroupConfig(fp, fgConfig))) {
            pushedConfigs = PmCollections.add(pushedConfigs, fp);
        }
        if(fgSpec.hasPackageDeps()) {
            processPackageDeps(fp, fgSpec);
        }

        if (pushedConfigs.isEmpty()) {
            return false;
        }

        modelBuilder.startGroup();

        boolean resolvedFeatures = processConfigItemContainer(modelBuilder, fp, fgSpec);
        for(FeaturePackRuntime.Builder pushedFp : pushedConfigs) {
            final ResolvedFeatureGroupConfig popped = modelBuilder.popConfig(pushedFp.gav);
            if (!popped.includedFeatures.isEmpty()) {
                for (Map.Entry<ResolvedFeatureId, FeatureConfig> feature : popped.includedFeatures.entrySet()) {
                    final FeatureConfig includedFc = feature.getValue();
                    if (includedFc != null && includedFc.hasParams()) {
                        final ResolvedFeatureId includedId = feature.getKey();
                        if (modelBuilder.isFilteredOut(includedId.specId, includedId)) {
                            continue;
                        }
                        // make sure the included ID is in fact present on the feature group branch
                        if (!modelBuilder.includes(includedId)) {
                            throw new ProvisioningException(Errors.featureNotInScope(includedId, fgConfig.getName(), fp.gav));
                        }
                        resolvedFeatures |= resolveFeature(modelBuilder, pushedFp, includedFc);
                    }
                }
            }
        }
        modelBuilder.endGroup();
        return resolvedFeatures;
    }

    private ResolvedFeatureGroupConfig resolveFeatureGroupConfig(FeaturePackRuntime.Builder fp, FeatureGroupSupport fg) throws ProvisioningException {
        final ResolvedFeatureGroupConfig resolvedFgc = new ResolvedFeatureGroupConfig(fg.getName());
        resolvedFgc.inheritFeatures = fg.isInheritFeatures();
        if(fg.hasExcludedSpecs()) {
            resolvedFgc.excludedSpecs = resolveSpecIds(fp, fg.getExcludedSpecs());
        }
        if(fg.hasIncludedSpecs()) {
            resolvedFgc.includedSpecs = resolveSpecIds(fp, fg.getIncludedSpecs());
        }
        if(fg.hasExcludedFeatures()) {
            resolvedFgc.excludedFeatures = resolveExcludedIds(fp, fg.getExcludedFeatures());
        }
        if(fg.hasIncludedFeatures()) {
            resolvedFgc.includedFeatures = resolveIncludedIds(fp, fg.getIncludedFeatures());
        }
        return resolvedFgc;
    }

    private Map<ResolvedFeatureId, FeatureConfig> resolveIncludedIds(FeaturePackRuntime.Builder fp, Map<FeatureId, FeatureConfig> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, FeatureConfig> included = features.entrySet().iterator().next();
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                return Collections.singletonMap(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams()), fc);
            }
            return Collections.singletonMap(resolvedSpec.resolveFeatureId(fc.getParams()), fc);
        }
        final Map<ResolvedFeatureId, FeatureConfig> tmp = new HashMap<>(features.size());
        for (Map.Entry<FeatureId, FeatureConfig> included : features.entrySet()) {
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                tmp.put(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams()), fc);
            } else {
                tmp.put(resolvedSpec.resolveFeatureId(fc.getParams()), fc);
            }
        }
        return tmp;
    }

    private Set<ResolvedFeatureId> resolveExcludedIds(FeaturePackRuntime.Builder fp, Map<FeatureId, String> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, String> excluded = features.entrySet().iterator().next();
            final FeatureId excludedId = excluded.getKey();
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(excludedId.getSpec().getName());
            if(parentFeature != null) {
                return Collections.singleton(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, excluded.getValue(), excludedId.getParams()));
            }
            return Collections.singleton(resolvedSpec.resolveFeatureId(excludedId.getParams()));
        }
        final Set<ResolvedFeatureId> tmp = new HashSet<>(features.size());
        for (Map.Entry<FeatureId, String> excluded : features.entrySet()) {
            final FeatureId excludedId = excluded.getKey();
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(excludedId.getSpec().getName());
            if(parentFeature != null) {
                tmp.add(resolvedSpec.resolveIdFromForeignKey(parentFeature.id, excluded.getValue(), excludedId.getParams()));
            } else {
                tmp.add(resolvedSpec.resolveFeatureId(excludedId.getParams()));
            }
        }
        return tmp;
    }

    private Set<ResolvedSpecId> resolveSpecIds(FeaturePackRuntime.Builder fp, Set<SpecId> specs) throws ProvisioningException {
        if(specs.size() == 1) {
            final SpecId specId = specs.iterator().next();
            return Collections.singleton(new ResolvedSpecId(fp.gav, specId.getName()));
        }
        final Set<ResolvedSpecId> tmp = new HashSet<>(specs.size());
        for (SpecId specId : specs) {
            tmp.add(new ResolvedSpecId(fp.gav, specId.getName()));
        }
        return tmp;
    }

    private boolean processConfigItemContainer(ConfigModelResolver modelBuilder, FeaturePackRuntime.Builder fp, ConfigItemContainer ciContainer) throws ProvisioningException {
        boolean resolvedFeatures = false;
        final FeaturePackRuntime.Builder prevFpOrigin = this.fpOrigin;
        if(ciContainer.isResetFeaturePackOrigin()) {
            this.fpOrigin = fp;
        }
        if(ciContainer.hasItems()) {
            for(ConfigItem item : ciContainer.getItems()) {
                final FeaturePackRuntime.Builder itemFp = item.getFpDep() == null ? fp : getFpDependency(fp, item.getFpDep());
                try {
                    if (item.isGroup()) {
                        final FeatureGroupSupport nestedFg = (FeatureGroupSupport) item;
                        resolvedFeatures |= processFeatureGroupConfig(modelBuilder, itemFp, nestedFg,
                                itemFp.getFeatureGroupSpec(nestedFg.getName()));
                    } else {
                        resolvedFeatures |= resolveFeature(modelBuilder, itemFp, (FeatureConfig) item);
                    }
                } catch (ProvisioningException e) {
                    throw new ProvisioningException(item.isGroup() ?
                            Errors.failedToProcess(fp.gav, ((FeatureGroup)item).getName()) : Errors.failedToProcess(fp.gav, (FeatureConfig)item),
                            e);
                }
            }
        }
        this.fpOrigin = prevFpOrigin;
        return resolvedFeatures;
    }

    FeaturePackRuntime.Builder getFpDependency(FeaturePackRuntime.Builder fp, final String fpDepName) throws ProvisioningException {
        if(Constants.THIS.equals(fpDepName)) {
            return this.fpOrigin;
        }
        return loadFpBuilder(fp.spec.getDependency(fpDepName).getTarget().getGav());
    }

    private boolean resolveFeature(ConfigModelResolver modelBuilder, FeaturePackRuntime.Builder fp, FeatureConfig fc) throws ProvisioningException {
        final ResolvedFeatureSpec spec = fp.getFeatureSpec(fc.getSpecId().getName());
        final ResolvedFeatureId resolvedId = parentFeature == null ? spec.resolveFeatureId(fc.getParams()) : spec.resolveIdFromForeignKey(parentFeature.id, fc.getParentRef(), fc.getParams());
        if(modelBuilder.isFilteredOut(spec.id, resolvedId)) {
            return false;
        }

        final ResolvedFeature myParent = parentFeature;
        parentFeature = resolveFeatureDepsAndRefs(modelBuilder, fp, spec, resolvedId, spec.resolveNonIdParams(parentFeature == null ? null : parentFeature.id, fc.getParentRef(), fc.getParams()), fc.getFeatureDeps());
        processConfigItemContainer(modelBuilder, fp, fc);
        parentFeature = myParent;
        return true;
    }

    private ResolvedFeature resolveFeatureDepsAndRefs(ConfigModelResolver modelBuilder, FeaturePackRuntime.Builder fp,
            final ResolvedFeatureSpec spec, final ResolvedFeatureId resolvedId, Map<String, Object> resolvedParams,
            Collection<FeatureDependencySpec> featureDeps)
            throws ProvisioningException {

        if(spec.xmlSpec.hasPackageDeps()) {
            processPackageDeps(fp, spec.xmlSpec);
        }

        final ResolvedFeature resolvedFeature = modelBuilder.includeFeature(resolvedId, spec, resolvedParams, resolveFeatureDeps(modelBuilder, fp, featureDeps, spec));

        if(spec.xmlSpec.hasFeatureRefs()) {
            final ResolvedFeature myParent = parentFeature;
            parentFeature = resolvedFeature;
            for(FeatureReferenceSpec refSpec : spec.xmlSpec.getFeatureRefs()) {
                if(!refSpec.isInclude()) {
                    continue;
                }
                final FeaturePackRuntime.Builder refFp = refSpec.getDependency() == null ? fp : getFpDependency(fp, refSpec.getDependency());
                final ResolvedFeatureSpec refResolvedSpec = refFp.getFeatureSpec(refSpec.getFeature().getName());
                final List<ResolvedFeatureId> refIds = spec.resolveRefId(parentFeature, refSpec, refResolvedSpec);
                if(refIds.isEmpty()) {
                    continue;
                }
                for(ResolvedFeatureId refId : refIds) {
                    if(modelBuilder.includes(refId) || modelBuilder.isFilteredOut(refId.specId, refId)) {
                        continue;
                    }
                    resolveFeatureDepsAndRefs(modelBuilder, refFp, refResolvedSpec, refId, Collections.emptyMap(), Collections.emptyList());
                }
            }
            parentFeature = myParent;
        }
        return resolvedFeature;
    }

    private Map<ResolvedFeatureId, FeatureDependencySpec> resolveFeatureDeps(ConfigModelResolver modelBuilder,
            FeaturePackRuntime.Builder fp, Collection<FeatureDependencySpec> featureDeps, final ResolvedFeatureSpec spec)
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
                final FeaturePackRuntime.Builder depFp = depSpec.getDependency() == null ? fp : getFpDependency(fp, depSpec.getDependency());
                resolveFeatureDepsAndRefs(modelBuilder, depFp, depFp.getFeatureSpec(depId.getSpecId().getName()), depId, Collections.emptyMap(), Collections.emptyList());
            }
        }
        return resolvedDeps;
    }

    private void popFpConfigs(List<FeaturePackConfig> fpConfigs) throws ProvisioningException {
        for (FeaturePackConfig fpConfig : fpConfigs) {
            final Gav fpGav = fpConfig.getGav();
            final FeaturePackRuntime.Builder fp = loadFpBuilder(fpGav);
            final FeaturePackConfig popped = fp.pop();
            if (popped.hasIncludedPackages()) {
                for (PackageConfig pkgConfig : popped.getIncludedPackages()) {
                    if (!fp.isPackageExcluded(pkgConfig.getName())) {
                        resolvePackage(fp, pkgConfig.getName());
                    } else {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgConfig.getName()));
                    }
                }
            }
        }
    }

    private void pushFpConfig(List<FeaturePackConfig> pushed, FeaturePackConfig fpConfig)
            throws ProvisioningDescriptionException, ProvisioningException {
        final FeaturePackRuntime.Builder fp = loadFpBuilder(fpConfig.getGav());
        if(fp.isStackEmpty()) {
            fp.push(fpConfig);
            pushed.add(fpConfig);
            return;
        }

        boolean doPush = false;
        if(fp.isInheritPackages()) {
            if(fpConfig.hasExcludedPackages()) {
                for(String excluded : fpConfig.getExcludedPackages()) {
                    if(!fp.isPackageExcluded(excluded) && !fp.isPackageIncluded(excluded)) {
                        doPush = true;
                        break;
                    }
                }
            }
            if(!doPush && fpConfig.hasIncludedPackages()) {
                for(PackageConfig included : fpConfig.getIncludedPackages()) {
                    if(!fp.isPackageIncluded(included.getName()) && !fp.isPackageExcluded(included.getName())) {
                        doPush = true;
                        break;
                    }
                }
            }
        }

        if(!doPush && fpConfig.hasDefinedConfigs() && fp.isInheritConfigs()) {
            doPush = true;
        }

        if(doPush) {
            pushed.add(fpConfig);
            fp.push(fpConfig);
        }
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

    private void resolvePackage(FeaturePackRuntime.Builder fp, final String pkgName)
            throws ProvisioningException {
        final PackageRuntime.Builder pkgRt = fp.pkgBuilders.get(pkgName);
        if(pkgRt != null) {
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

        if(pkg.spec.hasPackageDeps()) {
            try {
                processPackageDeps(fp, pkg.spec);
            } catch(ProvisioningException e) {
                throw new ProvisioningDescriptionException(Errors.resolvePackage(fp.gav, pkg.spec.getName()), e);
            }
        }
        fp.addPackage(pkgName);
    }

    private void processPackageDeps(FeaturePackRuntime.Builder fp, final PackageDepsSpec pkgDeps)
            throws ProvisioningException {
        if (pkgDeps.hasLocalPackageDeps()) {
            for (PackageDependencySpec dep : pkgDeps.getLocalPackageDeps()) {
                if(fp.isPackageExcluded(dep.getName())) {
                    if(!dep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, dep.getName()));
                    }
                    continue;
                }
                try {
                    resolvePackage(fp, dep.getName());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        if(pkgDeps.hasExternalPackageDeps()) {
            final Collection<String> depNames = pkgDeps.getExternalPackageSources();
            final List<FeaturePackConfig> pushedConfigs = new ArrayList<>(depNames.size());
            for(String depName : depNames) {
                pushFpConfig(pushedConfigs, fp.spec.getDependency(depName).getTarget());
            }
            for(String depName : depNames) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final FeaturePackRuntime.Builder targetFp = loadFpBuilder(depSpec.getTarget().getGav());
                if(targetFp == null) {
                    throw new IllegalStateException(depSpec.getName() + " " + depSpec.getTarget().getGav() + " has not been layed out yet");
                }
                boolean resolvedPackages = false;
                for(PackageDependencySpec pkgDep : pkgDeps.getExternalPackageDeps(depName)) {
                    if(targetFp.isPackageExcluded(pkgDep.getName())) {
                        if(!pkgDep.isOptional()) {
                            throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(targetFp.gav, pkgDep.getName()));
                        }
                        continue;
                    }
                    try {
                        resolvePackage(targetFp, pkgDep.getName());
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
