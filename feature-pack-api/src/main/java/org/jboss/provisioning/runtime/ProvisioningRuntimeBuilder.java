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
import org.jboss.provisioning.DefaultMessageWriter;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;
import org.jboss.provisioning.config.FeatureGroupConfigSupport;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.IncludedConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.spec.ConfigId;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.ConfigItemContainer;
import org.jboss.provisioning.spec.ConfigItem;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.PackageDependencies;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.SpecId;
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
        return newInstance(DefaultMessageWriter.getDefaultInstance());
    }

    public static ProvisioningRuntimeBuilder newInstance(final MessageWriter messageWriter) {
        return new ProvisioningRuntimeBuilder(messageWriter);
    }

    private static ResolvedFeatureId resolveFeatureId(ResolvedFeatureSpec spec, FeatureConfig config) throws ProvisioningDescriptionException {
        if(!spec.xmlSpec.hasId()) {
            return null;
        }
        final List<FeatureParameterSpec> idSpecs = spec.xmlSpec.getIdParams();
        if(idSpecs.size() == 1) {
            final FeatureParameterSpec idSpec = idSpecs.get(0);
            return new ResolvedFeatureId(spec.id, Collections.singletonMap(idSpec.getName(), getParamValue(spec.id, config.getParams(), idSpec)));
        }
        final Map<String, String> resolvedParams = new HashMap<>(idSpecs.size());
        for(FeatureParameterSpec param : idSpecs) {
            resolvedParams.put(param.getName(), getParamValue(spec.id, config.getParams(), param));
        }
        return new ResolvedFeatureId(spec.id, resolvedParams);
    }

    private static String getParamValue(ResolvedSpecId specId, Map<String, String> params, final FeatureParameterSpec param)
            throws ProvisioningDescriptionException {
        String value = params.get(param.getName());
        if(value == null) {
            value = param.getDefaultValue();
        }
        if(value == null && (param.isFeatureId() || !param.isNillable())) {
            throw new ProvisioningDescriptionException("Required parameter " + param.getName() + " of " + specId + " is missing value");
        }
        return value;
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
    ArtifactResolver artifactResolver;
    ProvisioningConfig config;
    PackageParameterResolver paramResolver;
    Path installDir;
    final Path workDir;
    final Path layoutDir;
    Path pluginsDir = null;

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> fpRtBuilders = new HashMap<>();
    private final MessageWriter messageWriter;
    private List<FeaturePackRuntime.Builder> fpRtBuildersOrdered = new ArrayList<>();
    List<ConfigModelBuilder> anonymousConfigs = Collections.emptyList();
    Map<String, ConfigModelBuilder> noModelNamedConfigs = Collections.emptyMap();
    Map<String, ConfigModelBuilder> noNameModelConfigs = Collections.emptyMap();
    Map<String, Map<String, ConfigModelBuilder>> modelConfigs = Collections.emptyMap();
    Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;
    private FeaturePackRuntime.Builder fpOrigin;
    Map<String, String> rtParams = new HashMap<>();

    private ResolvedFeature parentFeature;

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

        return new ProvisioningRuntime(this, messageWriter);
    }

    private void buildConfigs() throws ProvisioningException {
        if(!anonymousConfigs.isEmpty()) {
            for(ConfigModelBuilder config : anonymousConfigs) {
                config.build(this);
            }
        }
        if(!noModelNamedConfigs.isEmpty()) {
            for(Map.Entry<String, ConfigModelBuilder> entry : noModelNamedConfigs.entrySet()) {
                entry.getValue().build(this);
            }
        }

        if(!noNameModelConfigs.isEmpty()) {
            final Iterator<Map.Entry<String, ConfigModelBuilder>> i = noNameModelConfigs.entrySet().iterator();
            if(noNameModelConfigs.size() == 1) {
                final Map.Entry<String, ConfigModelBuilder> entry = i.next();
                final Map<String, ConfigModelBuilder> targetConfigs = modelConfigs.get(entry.getKey());
                if (targetConfigs == null) {
                    entry.getValue().build(this);
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
                        entry.getValue().build(this);
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

        if(fpConfig.isInheritConfigs()) {
            for(ConfigSpec config : fp.spec.getConfigs()) {
                if(fp.isConfigExcluded(config.getId())) {
                    continue;
                }
                final IncludedConfig includedConfig = fpConfig.getIncludedConfig(config.getId());
                if(includedConfig != null) {
                    contributed |= includeConfig(fp, includedConfig, config);
                } else {
                    contributed |= processConfigSpec(fp, config);
                }
            }
        } else {
            for(ConfigSpec config : fp.spec.getConfigs()) {
                if(fp.isConfigIncluded(config.getId())) {
                    final IncludedConfig includedConfig = fpConfig.getIncludedConfig(config.getId());
                    if(includedConfig != null) {
                        contributed |= includeConfig(fp, includedConfig, config);
                    } else {
                        contributed |= processConfigSpec(fp, config);
                    }
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
            for (ConfigSpec config : fpConfig.getDefinedConfigs()) {
                if (fp.isConfigExcluded(config.getId())) {
                    continue;
                }
                contributed |= processConfigSpec(fp, config);
            }
        }

        if(!fp.ordered && contributed) {
            orderFpRtBuilder(fp);
        }
    }

    private boolean includeConfig(FeaturePackRuntime.Builder fp, IncludedConfig includedConfig, ConfigSpec config) throws ProvisioningException {
        final ConfigModelBuilder configBuilder = getConfigModelBuilder(config.getId());
        configBuilder.overwriteProps(config.getProperties());
        return processFeatureGroupConfig(configBuilder, fp, includedConfig, config);
    }

    private boolean processConfigSpec(FeaturePackRuntime.Builder fp, ConfigSpec config) throws ProvisioningException {
        final ConfigModelBuilder configBuilder = getConfigModelBuilder(config.getId());
        configBuilder.overwriteProps(config.getProperties());
        try {
            return processFeatureGroupSpec(configBuilder, fp, config);
        } catch (ProvisioningException e) {
            throw new ProvisioningException(Errors.failedToResolveConfigSpec(config.getModel(), config.getName()), e);
        }
    }

    private ConfigModelBuilder getConfigModelBuilder(ConfigId config) {
        if(config.getModel() == null) {
            if(config.getName() == null) {
                final ConfigModelBuilder modelBuilder = ConfigModelBuilder.anonymous();
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
            return modelBuilder;
        }
        if(config.getName() == null) {
            if(noNameModelConfigs.isEmpty()) {
                final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forModel(config.getModel());
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
            return modelBuilder;
        }
        if (modelConfigs.isEmpty()) {
            final ConfigModelBuilder modelBuilder = ConfigModelBuilder.forConfig(config.getModel(), config.getName());
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
            return modelBuilder;
        }
        ConfigModelBuilder modelBuilder = namedConfigs.get(config.getName());
        if (modelBuilder == null) {
            if (namedConfigs.size() == 1) {
                namedConfigs = new HashMap<>(namedConfigs);
                if (modelConfigs.size() == 1) {
                    modelConfigs = new LinkedHashMap<>(modelConfigs);
                }
                modelConfigs.put(config.getModel(), namedConfigs);
            }
            modelBuilder = ConfigModelBuilder.forConfig(config.getModel(), config.getName());
            namedConfigs.put(config.getName(), modelBuilder);
        }
        return modelBuilder;
    }

    private boolean processFeatureGroupConfig(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, FeatureGroupConfigSupport fgConfig, final ConfigItemContainer fgSpec)
            throws ProvisioningException {
        List<FeaturePackRuntime.Builder> pushedConfigs = Collections.emptyList();
        if(fgConfig.hasExternalFeatureGroups()) {
            for(Map.Entry<String, FeatureGroupConfig> entry : fgConfig.getExternalFeatureGroups().entrySet()) {
                final FeaturePackRuntime.Builder depFpRt = getFpDependency(fp, entry.getKey());
                if(modelBuilder.pushConfig(depFpRt.gav, resolveFeatureGroupConfig(depFpRt, entry.getValue()))) {
                    switch(pushedConfigs.size()) {
                        case 0:
                            pushedConfigs = Collections.singletonList(depFpRt);
                            break;
                        case 1:
                            final FeaturePackRuntime.Builder first = pushedConfigs.get(0);
                            pushedConfigs = new ArrayList<>(2);
                            pushedConfigs.add(first);
                        default:
                            pushedConfigs.add(depFpRt);
                    }
                }
            }
        }
        if(modelBuilder.pushConfig(fp.gav, resolveFeatureGroupConfig(fp, fgConfig))) {
            switch(pushedConfigs.size()) {
                case 0:
                    pushedConfigs = Collections.singletonList(fp);
                    break;
                case 1:
                    final FeaturePackRuntime.Builder first = pushedConfigs.get(0);
                    pushedConfigs = new ArrayList<>(2);
                    pushedConfigs.add(first);
                default:
                    pushedConfigs.add(fp);
            }
        }
        if (pushedConfigs.isEmpty()) {
            return false;
        }
        boolean resolvedFeatures = processFeatureGroupSpec(modelBuilder, fp, fgSpec);
        for(FeaturePackRuntime.Builder pushedFp : pushedConfigs) {
            final ResolvedFeatureGroupConfig popped = modelBuilder.popConfig(pushedFp.gav);
            if (!popped.includedFeatures.isEmpty()) {
                for (Map.Entry<ResolvedFeatureId, FeatureConfig> feature : popped.includedFeatures.entrySet()) {
                    if (feature.getValue() != null) {
                        resolvedFeatures |= resolveFeature(modelBuilder, pushedFp, feature.getValue());
                    }
                }
            }
        }
        return resolvedFeatures;
    }

    private ResolvedFeatureGroupConfig resolveFeatureGroupConfig(FeaturePackRuntime.Builder fp, FeatureGroupConfigSupport fg) throws ProvisioningException {
        final ResolvedFeatureGroupConfig resolvedFgc = new ResolvedFeatureGroupConfig(fg.getName());
        resolvedFgc.inheritFeatures = fg.isInheritFeatures();
        if(fg.hasExcludedSpecs()) {
            resolvedFgc.excludedSpecs = resolveSpecIds(fp, fg.getExcludedSpecs());
        }
        if(fg.hasIncludedSpecs()) {
            resolvedFgc.includedSpecs = resolveSpecIds(fp, fg.getIncludedSpecs());
        }
        if(fg.hasExcludedFeatures()) {
            resolvedFgc.excludedFeatures = resolveFeatureSet(fp, fg.getExcludedFeatures());
        }
        if(fg.hasIncludedFeatures()) {
            resolvedFgc.includedFeatures = resolveFeatureMap(fp, fg.getIncludedFeatures());
        }
        return resolvedFgc;
    }

    private Map<ResolvedFeatureId, FeatureConfig> resolveFeatureMap(FeaturePackRuntime.Builder fp, Map<FeatureId, FeatureConfig> features) throws ProvisioningException {
        if (features.size() == 1) {
            final Map.Entry<FeatureId, FeatureConfig> included = features.entrySet().iterator().next();
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                initForeignKey(parentFeature, fc, resolvedSpec);
            }
            return Collections.singletonMap(new ResolvedFeatureId(resolvedSpec.id, included.getKey().getParams()), fc);
        }
        final Map<ResolvedFeatureId, FeatureConfig> tmp = new HashMap<>(features.size());
        for (Map.Entry<FeatureId, FeatureConfig> included : features.entrySet()) {
            final FeatureConfig fc = new FeatureConfig(included.getValue());
            final ResolvedFeatureSpec resolvedSpec = fp.getFeatureSpec(fc.getSpecId().getName());
            if (parentFeature != null) {
                initForeignKey(parentFeature, fc, resolvedSpec);
            }
            tmp.put(new ResolvedFeatureId(resolvedSpec.id, included.getKey().getParams()), fc);
        }
        return tmp;
    }

    private Set<ResolvedFeatureId> resolveFeatureSet(FeaturePackRuntime.Builder fp, Set<FeatureId> features) throws ProvisioningException {
        if (features.size() == 1) {
            FeatureId excludedId = features.iterator().next();
            if(parentFeature != null) {
                return Collections.singleton(initForeignKey(parentFeature.id, excludedId, fp.getFeatureSpec(excludedId.getSpec().getName())));
            }
            return Collections.singleton(resolveFeatureId(fp, excludedId));
        }
        final Set<ResolvedFeatureId> tmp = new HashSet<>(features.size());
        for (FeatureId excludedId : features) {
            final ResolvedFeatureId resolvedId = parentFeature == null ? resolveFeatureId(fp, excludedId) : initForeignKey(
                    parentFeature.id, excludedId, fp.getFeatureSpec(excludedId.getSpec().getName()));
            tmp.add(resolvedId);
        }
        return tmp;
    }

    ResolvedFeatureId resolveFeatureId(FeaturePackRuntime.Builder fp, final FeatureId featureId) {
        return new ResolvedFeatureId(new ResolvedSpecId(fp.gav, featureId.getSpec().getName()), featureId.getParams());
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

    private boolean processFeatureGroupSpec(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, ConfigItemContainer featureGroup) throws ProvisioningException {
        boolean resolvedFeatures = false;
        final FeaturePackRuntime.Builder prevFpOrigin = this.fpOrigin;
        if(featureGroup.isResetFeaturePackOrigin()) {
            this.fpOrigin = fp;
        }
        if(featureGroup.hasItems()) {
            for(ConfigItem item : featureGroup.getItems()) {
                final FeaturePackRuntime.Builder itemFp = item.getFpDep() == null ? fp : getFpDependency(fp, item.getFpDep());
                if(item.isGroup()) {
                    final FeatureGroupConfig nestedFg = (FeatureGroupConfig) item;
                    resolvedFeatures |= processFeatureGroupConfig(modelBuilder, itemFp, nestedFg, itemFp.getFeatureGroupSpec(nestedFg.getName()));
                } else {
                    final FeatureConfig fc = (FeatureConfig) item;
                    if (parentFeature != null) {
                        initForeignKey(parentFeature, fc, itemFp.getFeatureSpec(fc.getSpecId().getName()));
                    }
                    resolvedFeatures |= resolveFeature(modelBuilder, itemFp, fc);
                }
            }
        }
        this.fpOrigin = prevFpOrigin;
        return resolvedFeatures;
    }

    FeaturePackRuntime.Builder getFpDependency(FeaturePackRuntime.Builder fp, final String fpDepName)
            throws ProvisioningDescriptionException, ProvisioningException, ArtifactResolutionException {
        if(Constants.THIS.equals(fpDepName)) {
            return this.fpOrigin;
        }
        return loadFpBuilder(fp.spec.getDependency(fpDepName).getTarget().getGav());
    }

    private boolean resolveFeature(ConfigModelBuilder modelBuilder, FeaturePackRuntime.Builder fp, FeatureConfig fc) throws ProvisioningException {
        final SpecId specId = fc.getSpecId();
        final ResolvedFeatureSpec spec = fp.getFeatureSpec(specId.getName());

        final ResolvedFeatureId resolvedId = resolveFeatureId(spec, fc);
        if(modelBuilder.isFilteredOut(spec.id, resolvedId)) {
            return false;
        }

        if(spec.xmlSpec.hasPackageDeps()) {
            try {
                processPackageDeps(fp, specId.toString(), spec.xmlSpec);
            } catch(ProvisioningException e) {
                throw new ProvisioningDescriptionException(Errors.resolveFeature(spec.id), e);
            }
        }

        final ResolvedFeature myParent = parentFeature;
        parentFeature = modelBuilder.includeFeature(resolvedId, spec, fc, resolveFeatureDeps(modelBuilder, fp, fc, spec));

        if(spec.xmlSpec.hasFeatureRefs()) {
            for(FeatureReferenceSpec refSpec : spec.xmlSpec.getFeatureRefs()) {
                if(refSpec.isInclude()) {
                    final FeaturePackRuntime.Builder refFp = refSpec.getDependency() == null ? fp : getFpDependency(fp, refSpec.getDependency());
                    final ResolvedFeatureId refId = spec.resolveRefId(parentFeature, refSpec, refFp.getFeatureSpec(refSpec.getFeature().getName()));
                    if(refId == null || modelBuilder.includes(refId)) {
                        continue;
                    }
                    resolveFeature(modelBuilder, refFp, initFeatureConfig(refId));
                }
            }
        }

        processFeatureGroupSpec(modelBuilder, fp, fc);
        parentFeature = myParent;
        return true;
    }

    private Map<ResolvedFeatureId, FeatureDependencySpec> resolveFeatureDeps(ConfigModelBuilder modelBuilder,
            FeaturePackRuntime.Builder fp, FeatureConfig fc, final ResolvedFeatureSpec spec)
            throws ProvisioningException {
        Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps = spec.resolveFeatureDeps(this);
        if(fc.hasFeatureDeps()) {
            final Collection<FeatureDependencySpec> userDeps = fc.getFeatureDeps();
            if(resolvedDeps.isEmpty()) {
                resolvedDeps = new LinkedHashMap<>(userDeps.size());
            } else {
                resolvedDeps = new LinkedHashMap<>(resolvedDeps);
            }
            for (FeatureDependencySpec userDep : userDeps) {
                final ResolvedFeatureId depId = resolveFeatureId(userDep.getDependency() == null ? fp : getFpDependency(fp, userDep.getDependency()), userDep.getFeatureId());
                final FeatureDependencySpec specDep = resolvedDeps.put(depId, userDep);
                if(specDep != null) {
                    if(!userDep.isInclude() && specDep.isInclude()) {
                        resolvedDeps.put(depId, specDep);
                    }
                }
            }
        }
        if(!resolvedDeps.isEmpty()) {
            for(Map.Entry<ResolvedFeatureId, FeatureDependencySpec> dep : resolvedDeps.entrySet()) {
                final FeatureDependencySpec depSpec = dep.getValue();
                if(depSpec.isInclude() && !modelBuilder.includes(dep.getKey())) {
                    resolveFeature(modelBuilder, depSpec.getDependency() == null ? fp : getFpDependency(fp, depSpec.getDependency()), initFeatureConfig(dep.getKey()));
                }
            }
        }
        return resolvedDeps;
    }

    FeatureConfig initFeatureConfig(ResolvedFeatureId id) throws ProvisioningException {
        final FeatureConfig fc = new FeatureConfig(id.getSpecId().getName());
        for(Map.Entry<String, String> param : id.params.entrySet()) {
            fc.putParam(param.getKey(), param.getValue());
        }
        return fc;
    }

    private static void initForeignKey(ResolvedFeature parentFc, FeatureConfig childFc, final ResolvedFeatureSpec childSpec) throws ProvisioningException {
        final String parentRef = childFc.getParentRef() == null ? parentFc.getSpecId().getName() : childFc.getParentRef();
        final FeatureReferenceSpec refSpec = childSpec.xmlSpec.getFeatureRef(parentRef);
        if (refSpec == null) {
            throw new ProvisioningDescriptionException("Parent reference " + parentRef + " not found in " + childSpec.id);
        }
        if(refSpec.getParamsMapped() == 0) {
            for(Map.Entry<String, String> idEntry : parentFc.id.params.entrySet()) {
                final String prevValue = childFc.putParam(idEntry.getKey(), idEntry.getValue());
                if (prevValue != null && !prevValue.equals(idEntry.getValue())) {
                    final FeatureParameterSpec fkParam = childSpec.xmlSpec.getParam(idEntry.getKey());
                    if (fkParam.isFeatureId()) {
                        throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(childSpec.id, idEntry.getKey(), prevValue, idEntry.getValue()));
                    }
                }
            }
        } else {
            for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                final String paramValue = parentFc.getParam(refSpec.getTargetParam(i));
                if (paramValue == null) {
                    throw new ProvisioningDescriptionException(childSpec.id + " expects ID parameter '"
                            + refSpec.getTargetParam(i) + "' in " + parentFc.id);
                }
                final String prevValue = childFc.putParam(refSpec.getLocalParam(i), paramValue);
                if (prevValue != null && !prevValue.equals(paramValue)) {
                    final FeatureParameterSpec fkParam = childSpec.xmlSpec.getParam(refSpec.getLocalParam(i));
                    if (fkParam.isFeatureId()) {
                        throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(childSpec.id, refSpec.getLocalParam(i), prevValue, paramValue));
                    }
                }
            }
        }
    }

    private static ResolvedFeatureId initForeignKey(ResolvedFeatureId parentId, FeatureId childId, final ResolvedFeatureSpec childSpec) throws ProvisioningException {
        // TODO final String parentRef = childFc.getParentRef() == null ? parentFc.getSpecId().getName() : childFc.getParentRef();
        final String parentRef = parentId.getSpecId().getName();
        final FeatureReferenceSpec refSpec = childSpec.xmlSpec.getFeatureRef(parentRef);
        if (refSpec == null) {
            throw new ProvisioningDescriptionException("Parent reference " + parentRef + " not found in " + childSpec.id);
        }
        final Map<String, String> resolvedParams = new HashMap<>(parentId.params.size());
        resolvedParams.putAll(childId.getParams());
        if(refSpec.getParamsMapped() == 0) {
            for(Map.Entry<String, String> idEntry : parentId.params.entrySet()) {
                final String prevValue = resolvedParams.put(idEntry.getKey(), idEntry.getValue());
                if (prevValue != null && !prevValue.equals(idEntry.getValue())) {
                    final FeatureParameterSpec fkParam = childSpec.xmlSpec.getParam(idEntry.getKey());
                    if (fkParam.isFeatureId()) {
                        throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(childSpec.id, idEntry.getKey(), prevValue, idEntry.getValue()));
                    }
                }
            }
        } else {
            for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                final String paramValue = parentId.params.get(refSpec.getTargetParam(i));
                if (paramValue == null) {
                    throw new ProvisioningDescriptionException(childSpec.id + " expects ID parameter '"
                            + refSpec.getTargetParam(i) + "' in " + parentId);
                }
                final String prevValue = resolvedParams.put(refSpec.getLocalParam(i), paramValue);
                if (prevValue != null && !prevValue.equals(paramValue)) {
                    final FeatureParameterSpec fkParam = childSpec.xmlSpec.getParam(refSpec.getLocalParam(i));
                    if (fkParam.isFeatureId()) {
                        throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(childSpec.id, refSpec.getLocalParam(i), prevValue, paramValue));
                    }
                }
            }
        }
        return new ResolvedFeatureId(childSpec.id, resolvedParams);
    }

    private void popFpConfigs(List<FeaturePackConfig> fpConfigs) throws ProvisioningException {
        for (FeaturePackConfig fpConfig : fpConfigs) {
            final Gav fpGav = fpConfig.getGav();
            final FeaturePackRuntime.Builder fp = loadFpBuilder(fpGav);
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
                    if(!fp.isPackageExcluded(excluded) && !fp.isPackageIncluded(excluded, Collections.emptyList())) {
                        doPush = true;
                        break;
                    }
                }
            }
            if(!doPush && fpConfig.hasIncludedPackages()) {
                for(PackageConfig included : fpConfig.getIncludedPackages()) {
                    if(!fp.isPackageIncluded(included.getName(), included.getParameters()) && !fp.isPackageExcluded(included.getName())) {
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

        if(pkg.spec.hasPackageDeps()) {
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
        if (pkg.hasLocalPackageDeps()) {
            PackageDependencyGroupSpec localDeps = pkg.getLocalPackageDeps();
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
        if(pkg.hasExternalPackageDeps()) {
            final Collection<String> depNames = pkg.getExternalPackageSources();
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
                final PackageDependencyGroupSpec pkgDeps = pkg.getExternalPackageDeps(depName);
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
        this.rtParams.put(name, param);
        return this;
    }
}
