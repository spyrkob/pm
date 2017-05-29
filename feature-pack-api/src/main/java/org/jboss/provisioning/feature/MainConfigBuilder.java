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

package org.jboss.provisioning.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;


/**
 *
 * @author Alexey Loubyansky
 */
public class MainConfigBuilder {

    private static final int ADD = 1;
    private static final int OVERWRITE = 2;

    private static class SpecFeatures {
        List<ConfiguredFeature> list = new ArrayList<>();
        boolean liningUp;
    }

    static class ConfiguredFeature {
        final FeatureId id;
        final FeatureSpec spec;
        final FeatureConfig config;
        boolean liningUp;

        ConfiguredFeature(FeatureId id, FeatureSpec spec, FeatureConfig config) {
            this.id = id;
            this.spec = spec;
            this.config = config;
        }
    }

    public static MainConfigBuilder newInstance(FeatureSpecLoader specLoader) {
        return newInstance(specLoader, FeatureGroupLoader.NOT_CONFIGURED);
    }

    public static MainConfigBuilder newInstance(FeatureSpecLoader specLoader, FeatureGroupLoader configLoader) {
        return newInstance(specLoader, configLoader, featureId -> {
            throw new UnsupportedOperationException("Failed to load " + featureId + ". Feature config loading has not been setup.");
        });
    }

    public static MainConfigBuilder newInstance(FeatureSpecLoader specLoader, FeatureGroupLoader configLoader, FeatureConfigLoader featureLoader) {
        return new MainConfigBuilder(specLoader, configLoader, featureLoader);
    }

    private final FeatureSpecLoader specLoader;
    private final FeatureGroupLoader featureGroupLoader;
    private final FeatureConfigLoader featureLoader;
    private Map<String, FeatureSpec> featureSpecs = new HashMap<>();
    private boolean checkSpecRefs;
    private Map<FeatureId, ConfiguredFeature> featuresById = new HashMap<>();
    private Map<String, SpecFeatures> featuresBySpec = new LinkedHashMap<>();

    private List<FeatureGroupConfig> dependencyStack = null;

    private MainConfigBuilder(FeatureSpecLoader specLoader, FeatureGroupLoader fgLoader, FeatureConfigLoader featureLoader) {
        this.specLoader = specLoader;
        this.featureGroupLoader = fgLoader;
        this.featureLoader = featureLoader;
    }

    public MainConfigBuilder addFeatureGroup(String fgName) throws ProvisioningDescriptionException {
        return addFeatureGroup(null, fgName);
    }

    public MainConfigBuilder addFeatureGroup(String fgSource, String fgName) throws ProvisioningDescriptionException {
        return addFeatureGroup(featureGroupLoader.load(fgSource, fgName));
    }

    public MainConfigBuilder addFeatureGroup(FeatureGroupSpec featureGroup) throws ProvisioningDescriptionException {
        if(!featureGroup.dependencies.isEmpty()) {
            for(FeatureGroupConfig dep : featureGroup.dependencies) {
                processDependency(dep);
            }
        }
        if(!featureGroup.features.isEmpty()) {
            for(FeatureConfig feature : featureGroup.features) {
                if(!isExcluded(feature.specName)) {
                    addFeature(feature, ADD);
                }
            }
        }
        return this;
    }

    public MainConfigBuilder addFeature(FeatureConfig config) throws ProvisioningDescriptionException {
        addFeature(config, ADD);
        return this;
    }

    private void addFeature(FeatureConfig config, int action) throws ProvisioningDescriptionException {
        final FeatureSpec spec = getSpec(config.specName);
        final FeatureId id = spec.hasId() ? getId(spec.idParams, config) : null;
        if(!spec.params.isEmpty()) {
            // check that non-nillable parameters have values
            for(FeatureParameterSpec param : spec.params.values()) {
                if(!param.nillable) {
                    getParamValue(config, param);
                }
            }
            if(!config.params.isEmpty()) {
                for(String paramName : config.params.keySet()) {
                    if(!spec.params.containsKey(paramName)) {
                        final StringBuilder buf = new StringBuilder();
                        if(id == null) {
                            buf.append(config.specName).append(" configuration");
                        } else {
                            buf.append(id);
                        }
                        buf.append(" includes unknown parameter '" + paramName + "'");
                        throw new ProvisioningDescriptionException(buf.toString());
                    }
                }
            }
        }

        ConfiguredFeature feature;
        if(id == null) {
            feature = new ConfiguredFeature(null, spec, config);
        } else {
            feature = featuresById.get(id);
            if(feature != null) {
                if((action & OVERWRITE) == 0) {
                    throw new ProvisioningDescriptionException("Duplicate feature " + id);
                }
                feature.config.merge(config);
                if(!config.nested.isEmpty()) {
                    addNested(id, config, action | ADD);
                }
                return;
            }
            if((action & ADD) == 0) {
                throw new ProvisioningDescriptionException("The original feature " + id + " not found");
            }
            feature = new ConfiguredFeature(id, spec, config);
            featuresById.put(id, feature);
        }
        SpecFeatures features = featuresBySpec.get(config.specName);
        if(features == null) {
            features = new SpecFeatures();
            featuresBySpec.put(config.specName, features);
        }
        features.list.add(feature);
        if(!config.nested.isEmpty()) {
            if(id == null) {
                for(FeatureConfig nested : config.nested) {
                    addFeature(nested, action | ADD);
                }
            } else {
                addNested(id, config, action | ADD);
            }
        }
        return;
    }

    private void addNested(FeatureId parentId, FeatureConfig parent, int action) throws ProvisioningDescriptionException {
        for(FeatureConfig config : parent.nested) {
            final FeatureSpec spec = getSpec(config.specName);
            final String parentRef = config.parentRef == null ? parent.specName : config.parentRef;
            final FeatureReferenceSpec refSpec = spec.refs.get(parentRef);
            if(refSpec == null) {
                throw new ProvisioningDescriptionException("Parent reference " + parentRef + " not found in " + spec.name);
            }
            for (int i = 0; i < refSpec.localParams.length; ++i) {
                final String paramValue = parentId.getParam(refSpec.targetParams[i]);
                if (paramValue == null) {
                    throw new ProvisioningDescriptionException(parentId + " is missing ID parameter " + refSpec.targetParams[i]
                            + " for " + spec.name);
                }
                final String prevValue = config.putParam(refSpec.localParams[i], paramValue);
                if (prevValue != null && !prevValue.equals(paramValue)) {
                    throw new ProvisioningDescriptionException("Value " + prevValue + " of ID parameter "
                            + refSpec.localParams[i] + " of " + spec.name
                            + " conflicts with the corresponding parent ID value " + paramValue);
                }
            }
            addFeature(config, action);
        }
    }

    public void build() throws ProvisioningDescriptionException {
        if(checkSpecRefs) {
            for(FeatureSpec spec : featureSpecs.values()) {
                if(!spec.hasRefs()) {
                    continue;
                }
                for(FeatureReferenceSpec refSpec : spec.refs.values()) {
                    final FeatureSpec targetSpec = featureSpecs.get(refSpec.feature);
                    if(targetSpec == null) {
                        throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                + refSpec.name + " which targets unknown " + refSpec.feature + " feature");
                    }
                    if(!targetSpec.hasId()) {
                        throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                + refSpec.name + " which targets feature " + refSpec.feature + " that has no ID parameters");
                    }
                    if(targetSpec.idParams.size() != refSpec.localParams.length) {
                        throw new ProvisioningDescriptionException("Parameters of reference " + refSpec.name + " of feature " + spec.name +
                                " must correspond to the ID parameters of the target feature " + refSpec.feature);
                    }
                    for(int i = 0; i < refSpec.localParams.length; ++i) {
                        if(!spec.params.containsKey(refSpec.localParams[i])) {
                            throw new ProvisioningDescriptionException(spec.name
                                    + " feature does not include parameter " + refSpec.localParams[i] + " mapped in "
                                    + refSpec.name + " reference");
                        }
                        if(!targetSpec.params.containsKey(refSpec.targetParams[i])) {
                            throw new ProvisioningDescriptionException(targetSpec.name
                                    + " feature does not include parameter '" + refSpec.targetParams[i] + "' targeted from "
                                    + spec.name + " through " + refSpec.name + " reference");
                        }
                    }
                }
            }
        }
        for(SpecFeatures features : featuresBySpec.values()) {
            lineUp(features);
        }
    }

    private void lineUp(SpecFeatures features) throws ProvisioningDescriptionException {
        if(features.liningUp) {
            return;
        }
        features.liningUp = true;
        for(ConfiguredFeature feature : features.list) {
            lineUp(feature);
        }
    }

    private void lineUp(ConfiguredFeature feature) throws ProvisioningDescriptionException {
        if(feature.liningUp) {
            return;
        }
        feature.liningUp = true;
        if(feature.spec.hasRefs()) {
            for(FeatureReferenceSpec refSpec : feature.spec.refs.values()) {
                final FeatureId refId = getRefId(feature.spec, refSpec, feature.config);
                if(refId != null) {
                    final SpecFeatures specFeatures = featuresBySpec.get(refId.specName);
                    if(!specFeatures.liningUp) {
                        lineUp(specFeatures);
                    } else {
                        final ConfiguredFeature dep = featuresById.get(refId);
                        if(dep == null) {
                            throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved reference ").append(refId).toString());
                        }
                        lineUp(dep);
                    }
                }
            }
        }
        if(feature.config.hasDependencies()) {
            for(FeatureId depId : feature.config.dependencies) {
                final ConfiguredFeature dependency = featuresById.get(depId);
                if(dependency == null) {
                    throw new ProvisioningDescriptionException(errorFor(feature).append(" has unsatisfied dependency on ").append(depId).toString());
                }
                lineUp(dependency);
            }
        }

        final StringBuilder buf = errorFor(feature);
        System.out.println(buf.toString());
    }

    private StringBuilder errorFor(ConfiguredFeature feature) {
        final StringBuilder buf = new StringBuilder();
        if (feature.id != null) {
            buf.append(feature.id);
        } else {
            buf.append(feature.spec.name).append(" configuration");
        }
        return buf;
    }

    private void processDependency(FeatureGroupConfig dep) throws ProvisioningDescriptionException {
        if(dep.featureGroupName == null) {
            if(!dep.includedFeatures.isEmpty()) {
                for(Map.Entry<FeatureId, FeatureConfig> entry : dep.includedFeatures.entrySet()) {
                    final FeatureId featureId = entry.getKey();
                    if(!isExcluded(featureId)) {
                        if(entry.getValue() == null) {
                            if(!featuresById.containsKey(featureId)) {
                                addFeature(featureLoader.load(featureId), ADD);
                            }
                        } else {
                            if(featuresById.containsKey(featureId)) {
                                addFeature(entry.getValue(), OVERWRITE);
                            } else {
                                final FeatureConfig featureConfig = featureLoader.load(featureId);
                                featureConfig.merge(entry.getValue());
                                addFeature(featureConfig, ADD);
                            }
                        }
                    }
                }
            }
            return;
        }
        pushDependency(dep);
        addFeatureGroup(featureGroupLoader.load(dep.source, dep.featureGroupName));
        popDependency();
        if(!dep.includedFeatures.isEmpty()) {
            for(Map.Entry<FeatureId, FeatureConfig> entry : dep.includedFeatures.entrySet()) {
                if(!isExcluded(entry.getKey())) {
                    addFeature(entry.getValue(), OVERWRITE);
                }
            }
        }
    }

    private void pushDependency(FeatureGroupConfig dep) {
        if(dependencyStack == null) {
            dependencyStack = new ArrayList<>();
        }
        dependencyStack.add(dep);
    }

    private void popDependency() {
        dependencyStack.remove(dependencyStack.size() - 1);
    }

    private boolean isExcluded(String spec) {
        if(dependencyStack == null) {
            return false;
        }
        int i = dependencyStack.size();
        while(i > 0) {
            final FeatureGroupConfig dep = dependencyStack.get(--i);
            if(dep.isExcluded(spec)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(FeatureId featureId) {
        if(dependencyStack == null) {
            return false;
        }
        int i = dependencyStack.size();
        while(i > 0) {
            final FeatureGroupConfig dep = dependencyStack.get(--i);
            if(dep.isExcluded(featureId)) {
                return true;
            }
        }
        return false;
    }

    private FeatureSpec getSpec(String specName) throws ProvisioningDescriptionException {
        FeatureSpec spec = featureSpecs.get(specName);
        if(spec != null) {
            return spec;
        }
        spec = specLoader.load(specName);
        featureSpecs.put(specName, spec);
        if(!checkSpecRefs) {
            checkSpecRefs = spec.hasRefs();
        }
        return spec;
    }

    private static FeatureId getRefId(FeatureSpec spec, FeatureReferenceSpec refSpec, FeatureConfig config) throws ProvisioningDescriptionException {
        final FeatureId.Builder builder = FeatureId.builder(refSpec.feature);
        for(int i = 0; i < refSpec.localParams.length; ++i) {
            final FeatureParameterSpec param = spec.params.get(refSpec.localParams[i]);
            final String paramValue = getParamValue(config, param);
            if(paramValue == null) {
                if (!refSpec.nillable) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Reference ").append(refSpec).append(" of ");
                    if (spec.hasId()) {
                        buf.append(getId(spec.idParams, config));
                    } else {
                        buf.append(spec.name).append(" configuration ");
                    }
                    buf.append(" cannot be null");
                    throw new ProvisioningDescriptionException(buf.toString());
                }
                return null;
            }
            builder.addParam(refSpec.targetParams[i], paramValue);
        }
        return builder.build();
    }

    private static FeatureId getId(List<FeatureParameterSpec> params, FeatureConfig config) throws ProvisioningDescriptionException {
        if(params.size() == 1) {
            final FeatureParameterSpec param = params.get(0);
            return FeatureId.create(config.specName, param.name, getParamValue(config, param));
        }
        final FeatureId.Builder builder = FeatureId.builder(config.specName);
        for(FeatureParameterSpec param : params) {
            builder.addParam(param.name, getParamValue(config, param));
        }
        return builder.build();
    }

    private static String getParamValue(FeatureConfig config, final FeatureParameterSpec param)
            throws ProvisioningDescriptionException {
        final String value = config.params.getOrDefault(param.name, param.defaultValue);
        if(value == null && (param.featureId || !param.nillable)) {
            throw new ProvisioningDescriptionException(config.specName + " configuration is missing required parameter " + param.name);
        }
        return value;
    }
}
