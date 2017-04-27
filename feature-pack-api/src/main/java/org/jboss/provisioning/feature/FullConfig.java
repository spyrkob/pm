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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FullConfig {

    private static class ConfiguredFeature {
        final FeatureId id;
        final FeatureSpec spec;
        final FeatureConfig config;
        boolean committed;

        ConfiguredFeature(FeatureId id, FeatureSpec spec, FeatureConfig config) {
            this.id = id;
            this.spec = spec;
            this.config = config;
        }
    }

    public static class Builder {

        private final ConfigSchema schema;
        private Map<FeatureId, ConfiguredFeature> featuresById = new HashMap<>();
        private Map<String, List<ConfiguredFeature>> featuresBySpec = new LinkedHashMap<>();

        private Set<FeatureId> refsToResolve = new HashSet<>();

        private Builder(ConfigSchema schema) {
            this.schema = schema;
        }

        public Builder addFeature(FeatureConfig config) throws ProvisioningDescriptionException {
            final FeatureSpec spec = schema.getFeatureSpec(config.specName);
            final FeatureId id = spec.hasId() ? getId(spec.idParams, config) : null;
            final ConfiguredFeature feature = new ConfiguredFeature(id, spec, config);
            if(id != null) {
                if (featuresById.containsKey(id)) {
                    throw new ProvisioningDescriptionException("Duplicate feature " + id);
                }
                featuresById.put(id, feature);
            }
            List<ConfiguredFeature> features = featuresBySpec.get(config.specName);
            if(features == null) {
                features = new ArrayList<>();
                featuresBySpec.put(config.specName, features);
            }
            features.add(feature);

            if(!spec.refs.isEmpty()) {
                for(FeatureReferenceSpec refSpec : spec.refs.values()) {
                    refsToResolve.add(getRefId(spec, refSpec, config));
                }
            }
            if(!config.dependencies.isEmpty()) {
                for(FeatureId depId : config.dependencies) {
                    refsToResolve.add(depId);
                }
            }
            return this;
        }

        public FullConfig build() throws ProvisioningDescriptionException {
            if (!refsToResolve.isEmpty()) {
                for(FeatureId id : refsToResolve) {
                    if(!featuresById.containsKey(id)) {
                        throw new ProvisioningDescriptionException("Failed to resolve reference " + id);
                    }
                }
            }
            return new FullConfig(this);
        }

    }

    private static FeatureId getRefId(FeatureSpec spec, FeatureReferenceSpec refSpec, FeatureConfig config) throws ProvisioningDescriptionException {
        final FeatureId.Builder builder = FeatureId.builder(refSpec.feature);
        for(Map.Entry<String, String> mapping : refSpec.paramMapping.entrySet()) {
            final FeatureParameterSpec param = spec.params.get(mapping.getKey());
            String value = config.params.get(param.name);
            if(value == null) {
                if(param.featureId ||!param.nillable) {
                    throw new ProvisioningDescriptionException("Parameter " + param.name + " of " + config.specName + " can't be null.");
                }
                if(param.defaultValue != null) {
                    value = param.defaultValue;
                } else {
                    return null;
                }
            }
            builder.addParam(mapping.getValue(), value);
        }
        return builder.build();
    }

    private static FeatureId getId(List<FeatureParameterSpec> params, FeatureConfig config) throws ProvisioningDescriptionException {
        if(params.size() == 1) {
            final FeatureParameterSpec param = params.get(0);
            String value = config.params.get(param.name);
            if(value == null) {
                if(param.featureId ||!param.nillable) {
                    throw new ProvisioningDescriptionException("Parameter " + param.name + " of " + config.specName + " can't be null.");
                }
                if(param.defaultValue != null) {
                    value = param.defaultValue;
                } else {
                    return null;
                }
            }
            return FeatureId.create(config.specName, param.name, value);
        }
        final FeatureId.Builder builder = FeatureId.builder(config.specName);
        for(FeatureParameterSpec param : params) {
            String value = config.params.get(param.name);
            if(value == null) {
                if(param.featureId ||!param.nillable) {
                    throw new ProvisioningDescriptionException("Parameter " + param.name + " of " + config.specName + " can't be null.");
                }
                if(param.defaultValue != null) {
                    value = param.defaultValue;
                } else {
                    return null;
                }
            }
            builder.addParam(param.name, value);
        }
        return builder.build();
    }

    public static Builder builder(ConfigSchema schema) {
        return new Builder(schema);
    }

    private FullConfig(Builder builder) {

    }
}
