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

package org.jboss.provisioning.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class Config {

    public static class ConfiguredFeature {
        final ConfigId id;
        final FeatureConfigDescription descr;
        final FeatureConfig config;

        private ConfiguredFeature parent;

        private boolean linedUp;

        ConfiguredFeature(ConfigId id, FeatureConfigDescription descr, FeatureConfig config) {
            this.id = id;
            this.descr = descr;
            this.config = config;
        }
    }

    public static class Builder {

        private final ConfigSchema schema;
        private Map<String, List<ConfiguredFeature>> configsByName = new HashMap<>();
        private Map<ConfigId, ConfiguredFeature> configsById = Collections.emptyMap();

        private List<ConfiguredFeature> line = new ArrayList<>();

        private Builder(ConfigSchema schema) throws ProvisioningDescriptionException {
            this.schema = schema;
        }

        public Builder add(FeatureConfig config) throws ProvisioningDescriptionException {
            final FeatureConfigDescription configDescr = schema.getDescription(config.configName);
            final ConfiguredFeature configured;
            if (configDescr.idParam != null) {
                final ConfigId configId = configDescr.getConfigId(config);
                if (configsById.containsKey(configId)) {
                    throw new ProvisioningDescriptionException("Duplicate feature ID " + configId);
                }
                configured = new ConfiguredFeature(configId, configDescr, config);
                switch (configsById.size()) {
                    case 0:
                        configsById = Collections.singletonMap(configId, configured);
                        break;
                    case 1:
                        configsById = new HashMap<>(configsById);
                    default:
                        configsById.put(configId, configured);
                }
            } else {
                configured = new ConfiguredFeature(null, configDescr, config);
            }

            List<ConfiguredFeature> featureConfigs = configsByName.get(config.configName);
            if(featureConfigs == null) {
                configsByName.put(config.configName, Collections.singletonList(configured));
            } else {
                if(!configDescr.maxOccursUnbounded) {
                    throw new ProvisioningDescriptionException("Feature config may not appear more than once at " + config.configName);
                }
                if(featureConfigs.size() == 1) {
                    featureConfigs = new ArrayList<>(featureConfigs);
                    configsByName.put(config.configName, featureConfigs);
                }
                featureConfigs.add(configured);
            }
            return this;
        }

        public void build() throws ProvisioningDescriptionException {

            final String[] configNames = configsByName.keySet().toArray(new String[configsByName.size()]);
            for (String name : configNames) {
                initParents(name);
            }

            for(List<ConfiguredFeature> features : configsByName.values()) {
                for(ConfiguredFeature feature : features) {
                    lineUp(feature);
                }
            }
        }

        private void lineUp(ConfiguredFeature config) throws ProvisioningDescriptionException {
            if (config.linedUp) {
                return;
            }
            config.linedUp = true;

            if (config.parent != null && !config.parent.linedUp) {
                lineUp(config.parent);
            }

            if (config.config == null) {
                if (!config.descr.params.isEmpty()) {
                    final StringBuilder buf = new StringBuilder();
                    if (config.id == null) {
                        buf.append(config.descr.path);
                    } else {
                        buf.append(config.id);
                    }
                    buf.append(" is missing configuration");
                    throw new ProvisioningDescriptionException(buf.toString());
                }
                return;
            }

            if (!config.descr.params.isEmpty()) {
                if (config.config.params.isEmpty()) {
                    final StringBuilder buf = new StringBuilder();
                    if (config.id == null) {
                        buf.append(config.descr.path);
                    } else {
                        buf.append(config.id);
                    }
                    buf.append(" configuration is missing parameters");
                    throw new ProvisioningDescriptionException(buf.toString());
                }

                if (!config.descr.configRefs.isEmpty()) {
                    for (ConfigRef configRef : config.descr.configRefs) {
                        final FeatureConfigDescription refDescr = this.schema.getDescription(configRef.name);
                        final ConfigId refId = config.descr.getConfigId(refDescr.path,
                                configRef.pathParams == null ? refDescr.path.names : configRef.pathParams,
                                        !configRef.nillable, config.config);
                        if (refId != null) {
                            lineUp(config, refId, false);
                        }
                    }
                }
            } else if (!config.config.params.isEmpty()) {
                final StringBuilder buf = new StringBuilder();
                if (config.id == null) {
                    buf.append(config.descr.path);
                } else {
                    buf.append(config.id);
                }
                buf.append(" configuration does not accept parameters");
                throw new ProvisioningDescriptionException(buf.toString());
            }

            if(!config.config.dependencies.isEmpty()) {
                for(FeatureConfig.Dependency dependency : config.config.dependencies) {
                    lineUp(config, dependency.getConfigId(), dependency.isOptional());
                }
            }

            line.add(config);
            final StringBuilder buf = new StringBuilder();
            buf.append("lined up ");
            if (config.id != null) {
                buf.append(config.id);
            } else {
                buf.append(config.descr.path);
            }
            System.out.println(buf.toString());
        }

        private void lineUp(ConfiguredFeature config, final ConfigId dependencyId, boolean optional) throws ProvisioningDescriptionException {
            final ConfiguredFeature dependency = configsById.get(dependencyId);
            if (dependency == null) {
                if(optional) {
                    return;
                }
                final StringBuilder buf = new StringBuilder();
                if (config.id != null) {
                    buf.append(config.id);
                } else {
                    buf.append("Configuration of ").append(config.descr.path);
                }
                buf.append(" has unsatisfied dependency on ").append(dependencyId);
                throw new ProvisioningDescriptionException(buf.toString());
            }
            lineUp(dependency);
        }

        private void initParents(String configName) throws ProvisioningDescriptionException {
            final FeatureConfigDescription configDescr = schema.getDescription(configName);
            if(configDescr.parentConfigName != null) {
                initParents(configDescr.parentConfigName);
            }
            final List<ConfiguredFeature> features = configsByName.get(configName);
            if(features == null) {
                return;
            }
            for (ConfiguredFeature feature : features) {
                initParent(feature);
            }
        }

        private void initParent(ConfiguredFeature feature) throws ProvisioningDescriptionException {
            if(feature.parent != null || feature.descr.parentConfigName == null) {
                return;
            }
            if (feature.descr.parentPath != null) {
                final ConfigId parentId = feature.descr.getConfigId(feature.descr.parentPath, feature.descr.parentPath.names, true, feature.config);
                feature.parent = configsById.get(parentId);
                if (feature.parent == null) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Failed to resolve parent feature ").append(parentId).append(" of ");
                    if (feature.id == null) {
                        buf.append(feature.descr.path);
                    } else {
                        buf.append(feature.id);
                    }
                    buf.append(" configuration");
                    throw new ProvisioningDescriptionException(buf.toString());
                }
            } else {
                final FeatureConfigDescription parentDescr = schema.getDescription(feature.descr.parentConfigName);
                if (parentDescr == null) {
                    throw new ProvisioningDescriptionException("Unknown feature config description " + feature.descr.parentConfigName);
                }
                List<ConfiguredFeature> parents = configsByName.get(parentDescr.configName);
                if (parents == null) {
                    feature.parent = new ConfiguredFeature(null, parentDescr, null);
                    parents = Collections.singletonList(feature.parent);
                    configsByName.put(parentDescr.configName, parents);
                } else {
                    if (parentDescr.maxOccursUnbounded) {
                        feature.parent = new ConfiguredFeature(null, parentDescr, null);
                        if (parents.size() == 1) {
                            parents = new ArrayList<>(parents);
                            configsByName.put(parentDescr.configName, parents);
                        }
                        parents.add(feature.parent);
                    } else {
                        if (parents.size() > 1) {
                            throw new ProvisioningDescriptionException("Expected only one parent");
                        }
                        feature.parent = parents.get(0);
                    }
                }
            }
        }
    }

    public static Builder builder(ConfigSchema schema) throws ProvisioningDescriptionException {
        return new Builder(schema);
    }

}
