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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.schema.FeatureConfigType.Occurence;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfig implements FeatureConfigResolver {

    public static class Builder {

        final FeatureConfigType type;
        private final String name;
        private Map<ConfigTypeId, List<FeatureConfig>> subconfigs = Collections.emptyMap();
        private Map<String, ConfigRef> dependencies = Collections.emptyMap();
        private Map<ConfigRef, FeatureConfig> refs = Collections.emptyMap();

        Builder(FeatureConfigType type, String name) {
            this.type = type;
            this.name = name;
        }

        public Builder addFeatureConfig(FeatureConfig featureConfig) throws ProvisioningDescriptionException {
            final Occurence occurence = type.occurences.get(featureConfig.typeId);
            if(occurence == null) {
                throw new ProvisioningDescriptionException("Unexpected feature configuration " + featureConfig.typeId + " in " + type.id);
            }

            List<FeatureConfig> specConfigs = subconfigs.get(featureConfig.typeId);
            if(specConfigs != null && specConfigs.size() > 1) {
                if(!occurence.isMaxOccursUnbounded()) {
                    throw new ProvisioningDescriptionException("Feature config " + featureConfig.typeId + " can appear only once and was already added");
                }
                specConfigs.add(featureConfig);
            } else {
                if(specConfigs == null) {
                    specConfigs = Collections.singletonList(featureConfig);
                } else {
                    specConfigs = new ArrayList<>(specConfigs);
                    specConfigs.add(featureConfig);
                }
                switch(subconfigs.size()) {
                    case 0:
                        subconfigs = Collections.singletonMap(featureConfig.typeId, specConfigs);
                        break;
                    case 1:
                        subconfigs = new LinkedHashMap<>(subconfigs);
                    default:
                        subconfigs.put(featureConfig.typeId, specConfigs);
                }
            }

            if(featureConfig.ref != null) {
                if(refs.isEmpty()) {
                    refs = Collections.singletonMap(featureConfig.ref, featureConfig);
                } else {
                    if(refs.containsKey(featureConfig.ref)) {
                        throw new ProvisioningDescriptionException("Duplicate reference " + featureConfig.ref);
                    }
                    if(refs.size() == 1) {
                        refs = new LinkedHashMap<>(refs);
                    }
                    refs.put(featureConfig.ref, featureConfig);
                }
            }
            return this;
        }

        public Builder addDependency(String depId, ConfigRef ref) throws ProvisioningDescriptionException {
            if(dependencies.containsKey(depId)) {
                throw new ProvisioningDescriptionException("Dependency " + depId + " is already satisfied by " + dependencies.get(depId));
            }
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(depId, ref);
                    break;
                case 1:
                    dependencies = new LinkedHashMap<>(dependencies);
                default:
                    dependencies.put(depId, ref);
            }
            return this;
        }

        public FeatureConfig build() throws ProvisioningDescriptionException {
            for(FeatureConfigType.Occurence occurence : type.occurences.values()) {
                List<FeatureConfig> occurences = subconfigs.get(occurence.typeId);
                if(occurences != null) {
                    if(occurences.size() > 1) {
                        if(subconfigs.size() == 1) {
                            subconfigs = Collections.singletonMap(occurence.typeId, Collections.unmodifiableList(occurences));
                        } else {
                            subconfigs.put(occurence.typeId, Collections.unmodifiableList(occurences));
                        }
                    }
                } else if(occurence.required) {
                    throw new ProvisioningDescriptionException("Required configuration is missing: " + occurence.typeId);
                }
            }
            for(FeatureConfigType.Dependency dep : type.dependencies.values()) {
                if(!dep.optional) {
                    final ConfigRef configId = dependencies.get(dep.depId);
                    if (configId == null) {
                        final StringBuilder buf = new StringBuilder();
                        buf.append("Dependency ").append(dep.depId).append(" of ");
                        if(name != null) {
                            buf.append(ConfigRef.create(type.id, name));
                        } else {
                            buf.append(type.id);
                        }
                        buf.append(" is not satisfied");
                        throw new ProvisioningDescriptionException(buf.toString());
                    }
                }
            }
            return new FeatureConfig(this);
        }
    }

    //final ConfigId id;
    final ConfigTypeId typeId;
    final ConfigRef ref;

    // Subconfigs (local configs) by spec id and then by config instance name
    final Map<ConfigTypeId, List<FeatureConfig>> subconfigs;
    final Map<ConfigRef, FeatureConfig> refs;

    // Dependency is a dependency id and the name of the config
    final Map<String, ConfigRef> dependencies;

//    private final Map<String, PackageParameter> params;
//    private final Set<String> excludedPackages;
//    private final Set<String> includedPackages;

    public ConfigTypeId getTypeId() {
        return typeId;
    }

    public ConfigRef getRef() {
        return ref;
    }

    private FeatureConfig(Builder builder) {
        this.typeId = builder.type.id;
        this.ref = builder.name == null ? null : ConfigRef.create(typeId, builder.name);
        this.subconfigs = builder.subconfigs.size() > 1 ? Collections.unmodifiableMap(builder.subconfigs) : builder.subconfigs;
        this.refs = builder.refs.size() > 1 ? Collections.unmodifiableMap(builder.refs) : builder.refs;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableMap(builder.dependencies) : builder.dependencies;
    }

    @Override
    public FeatureConfig resolve(ConfigRef ref) throws ProvisioningException {
        final FeatureConfig featureConfig = refs.get(ref);
        if(featureConfig == null) {
            throw new ProvisioningException("Failed to resolve reference " + ref);
        }
        return featureConfig;
    }
}
