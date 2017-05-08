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
public class FullConfigBuilder {

    private static class SpecFeatures {
        List<ConfiguredFeature> list = new ArrayList<>();
        boolean liningUp;
    }

    private static class ConfiguredFeature {
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

    public static FullConfigBuilder newInstance(FeatureSpecLoader specLoader) {
        return newInstance(specLoader, name -> {
            throw new UnsupportedOperationException("Failed to load config " + name + ". Config loading has not been setup.");
        });
    }

    public static FullConfigBuilder newInstance(FeatureSpecLoader specLoader, ConfigLoader configLoader) {
        return newInstance(specLoader, configLoader, featureId -> {
            throw new UnsupportedOperationException("Failed to load " + featureId + ". Feature config loading has not been setup.");
        });
    }

    public static FullConfigBuilder newInstance(FeatureSpecLoader specLoader, ConfigLoader configLoader, FeatureConfigLoader featureLoader) {
        return new FullConfigBuilder(specLoader, configLoader, featureLoader);
    }

    private final FeatureSpecLoader specLoader;
    private final ConfigLoader configLoader;
    private final FeatureConfigLoader featureLoader;
    private FeatureSpecRegistry.Builder schema = FeatureSpecRegistry.builder();
    private Map<FeatureId, ConfiguredFeature> featuresById = new HashMap<>();
    private Map<String, SpecFeatures> featuresBySpec = new LinkedHashMap<>();

    private List<ConfigDependency> dependencyStack = null;

    private FullConfigBuilder(FeatureSpecLoader specLoader, ConfigLoader configLoader, FeatureConfigLoader featureLoader) {
        this.specLoader = specLoader;
        this.configLoader = configLoader;
        this.featureLoader = featureLoader;
    }

    public FullConfigBuilder addConfig(String name) throws ProvisioningDescriptionException {
        return addConfig(configLoader.load(name));
    }

    public FullConfigBuilder addConfig(Config config) throws ProvisioningDescriptionException {
        if(!config.dependencies.isEmpty()) {
            for(ConfigDependency dep : config.dependencies.values()) {
                processDependency(dep);
            }
        }
        if(!config.features.isEmpty()) {
            for(FeatureConfig feature : config.features) {
                if(!isExcluded(feature.specName)) {
                    addFeature(feature, false);
                }
            }
        }
        return this;
    }

    public FullConfigBuilder addFeature(FeatureConfig config) throws ProvisioningDescriptionException {
        addFeature(config, false);
        return this;
    }

    private void addFeature(FeatureConfig config, boolean include) throws ProvisioningDescriptionException {
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
        if(id != null) {
            feature = featuresById.get(id);
            if(feature != null) {
                if(include) {
                    feature.config.merge(config);
                    return;
                }
                throw new ProvisioningDescriptionException("Duplicate feature " + id);
            }
            if(include) {
                throw new ProvisioningDescriptionException("The original feature " + id + " not found");
            }
            feature = new ConfiguredFeature(id, spec, config);
            featuresById.put(id, feature);
        } else {
            feature = new ConfiguredFeature(null, spec, config);
        }
        SpecFeatures features = featuresBySpec.get(config.specName);
        if(features == null) {
            features = new SpecFeatures();
            featuresBySpec.put(config.specName, features);
        }
        features.list.add(feature);
        return;
    }

    public void build() throws ProvisioningDescriptionException {
        schema.build();
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

    private void processDependency(ConfigDependency dep) throws ProvisioningDescriptionException {
        if(dep.configName == null) {
            if(!dep.includedFeatures.isEmpty()) {
                for(Map.Entry<FeatureId, FeatureConfig> entry : dep.includedFeatures.entrySet()) {
                    final FeatureId featureId = entry.getKey();
                    if(!isExcluded(featureId)) {
                        if(entry.getValue() == null) {
                            if(!featuresById.containsKey(featureId)) {
                                addFeature(featureLoader.load(featureId), false);
                            }
                        } else {
                            if(featuresById.containsKey(featureId)) {
                                addFeature(entry.getValue(), true);
                            } else {
                                final FeatureConfig featureConfig = featureLoader.load(featureId);
                                featureConfig.merge(entry.getValue());
                                addFeature(featureConfig, false);
                            }
                        }
                    }
                }
            }
            return;
        }
        pushDependency(dep);
        addConfig(configLoader.load(dep.configName));
        popDependency();
        if(!dep.includedFeatures.isEmpty()) {
            for(Map.Entry<FeatureId, FeatureConfig> entry : dep.includedFeatures.entrySet()) {
                if(!isExcluded(entry.getKey())) {
                    addFeature(entry.getValue(), true);
                }
            }
        }
    }

    private void pushDependency(ConfigDependency dep) {
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
            final ConfigDependency dep = dependencyStack.get(--i);
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
            final ConfigDependency dep = dependencyStack.get(--i);
            if(dep.isExcluded(featureId)) {
                return true;
            }
        }
        return false;
    }

    private FeatureSpec getSpec(String specName) throws ProvisioningDescriptionException {
        FeatureSpec spec = schema.featureSpecs.get(specName);
        if(spec != null) {
            return spec;
        }
        spec = specLoader.load(specName);
        schema.addFeatureSpec(spec);
        return spec;
    }

    private static FeatureId getRefId(FeatureSpec spec, FeatureReferenceSpec refSpec, FeatureConfig config) throws ProvisioningDescriptionException {
        final FeatureId.Builder builder = FeatureId.builder(refSpec.feature);
        for(Map.Entry<String, String> mapping : refSpec.paramMapping.entrySet()) {
            final FeatureParameterSpec param = spec.params.get(mapping.getKey());
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
            builder.addParam(mapping.getValue(), paramValue);
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
