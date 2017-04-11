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
        final ConfigRef id;
        final FeatureConfigDescription descr;
        final FeatureConfig config;

        private ConfiguredFeature parent;

        private boolean linedUp;

        ConfiguredFeature(ConfigRef id, FeatureConfigDescription descr, FeatureConfig config) {
            this.id = id;
            this.descr = descr;
            this.config = config;
        }
    }

    public static class Builder {

        private final ConfigSchema schema;
        private Map<String, List<ConfiguredFeature>> spots = new HashMap<>();
        private Map<ConfigRef, ConfiguredFeature> refs = Collections.emptyMap();

        private List<ConfiguredFeature> line = new ArrayList<>();

        private Builder(ConfigSchema schema) throws ProvisioningDescriptionException {
            this.schema = schema;
        }

        public Builder add(FeatureConfig config) throws ProvisioningDescriptionException {
            final FeatureConfigDescription configDescr = schema.getDescription(config.spot);
            final ConfiguredFeature configured;
            if (configDescr.idParam != null) {
                final String configId = config.getParameterValue(configDescr.idParam, true);
                final ConfigRef configRef;
                configRef = ConfigRef.create(config.spot, configId);
                if (refs.containsKey(configRef)) {
                    throw new ProvisioningDescriptionException("Config with id " + configRef + " already registered");
                }
                configured = new ConfiguredFeature(configRef, configDescr, config);
                switch (refs.size()) {
                    case 0:
                        refs = Collections.singletonMap(configRef, configured);
                        break;
                    case 1:
                        refs = new HashMap<>(refs);
                    default:
                        refs.put(configRef, configured);
                }
            } else {
                configured = new ConfiguredFeature(null, configDescr, config);
            }

            List<ConfiguredFeature> spotFeatures = spots.get(config.spot);
            if(spotFeatures == null) {
                spots.put(config.spot, Collections.singletonList(configured));
            } else {
                if(!configDescr.maxOccursUnbounded) {
                    throw new ProvisioningDescriptionException("Feature config may not appear more than once at " + config.spot);
                }
                if(spotFeatures.size() == 1) {
                    spotFeatures = new ArrayList<>(spotFeatures);
                    spots.put(config.spot, spotFeatures);
                }
                spotFeatures.add(configured);
            }
            return this;
        }

        public void build() throws ProvisioningDescriptionException {

            final String[] spotNames = spots.keySet().toArray(new String[spots.size()]);
            for (String spot : spotNames) {
                initParents(spot);
            }

            for(List<ConfiguredFeature> features : spots.values()) {
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
                        buf.append(config.descr.spot);
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
                        buf.append(config.descr.spot);
                    } else {
                        buf.append(config.id);
                    }
                    buf.append(" configuration is missing parameters");
                    throw new ProvisioningDescriptionException(buf.toString());
                }

                if (!config.descr.refParams.isEmpty()) {
                    for (Map.Entry<String, String> entry : config.descr.refParams.entrySet()) {
                        final String featureId = config.config.getParameterValue(entry.getKey(), true);
                        final ConfigRef ref = ConfigRef.create(entry.getValue(), featureId);
                        final ConfiguredFeature dependency = refs.get(ref);
                        if (dependency == null) {
                            final StringBuilder buf = new StringBuilder();
                            if (config.id != null) {
                                buf.append(config.id);
                            } else {
                                buf.append("Configuration of ").append(config.descr.spot);
                            }
                            buf.append(" has unsatisfied dependency on ").append(ref);
                            throw new ProvisioningDescriptionException(buf.toString());
                        }
                        lineUp(dependency);
                    }
                }
            } else if(!config.config.params.isEmpty()) {
                final StringBuilder buf = new StringBuilder();
                if (config.id == null) {
                    buf.append(config.descr.spot);
                } else {
                    buf.append(config.id);
                }
                buf.append(" configuration does not accept parameters");
                throw new ProvisioningDescriptionException(buf.toString());
            }

            line.add(config);
            final StringBuilder buf = new StringBuilder();
            buf.append("lined up ");
            if (config.id != null) {
                buf.append(config.id);
            } else {
                buf.append(config.descr.spot);
            }
            System.out.println(buf.toString());
        }

        private void initParents(String spot) throws ProvisioningDescriptionException {
            final FeatureConfigDescription spotDescr = schema.getDescription(spot);
            if(spotDescr.parentSpot != null) {
                initParents(spotDescr.parentSpot);
            }
            final List<ConfiguredFeature> features = spots.get(spot);
            if(features == null) {
                return;
            }
            for (ConfiguredFeature feature : features) {
                initParent(feature);
            }
        }

        private void initParent(ConfiguredFeature feature) throws ProvisioningDescriptionException {
            if(feature.parent != null) {
                return;
            }

            if(feature.descr.parentSpot != null) {
                if(feature.descr.parentRefParam != null) {
                    final String parentId = feature.config.getParameterValue(feature.descr.parentRefParam, true);
                    final ConfigRef parentRef = ConfigRef.create(feature.descr.parentSpot, parentId);
                    feature.parent = refs.get(parentRef);
                    if(feature.parent == null) {
                        throw new ProvisioningDescriptionException("Failed to resolve parent " + parentRef + " for " + feature.id);
                    }
                } else {
                    final FeatureConfigDescription parentDescr = schema.getDescription(feature.descr.parentSpot);
                    if (parentDescr == null) {
                        throw new ProvisioningDescriptionException("Unknown feature config description "
                                + feature.descr.parentSpot);
                    }
                    List<ConfiguredFeature> parents = spots.get(parentDescr.spot);
                    if (parents == null) {
                        feature.parent = new ConfiguredFeature(null, parentDescr, null);
                        parents = Collections.singletonList(feature.parent);
                        spots.put(parentDescr.spot, parents);
                    } else {
                        if (parentDescr.maxOccursUnbounded) {
                            feature.parent = new ConfiguredFeature(null, parentDescr, null);
                            if (parents.size() == 1) {
                                parents = new ArrayList<>(parents);
                                spots.put(parentDescr.spot, parents);
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
    }

    public static Builder builder(ConfigSchema schema) throws ProvisioningDescriptionException {
        return new Builder(schema);
    }

}
