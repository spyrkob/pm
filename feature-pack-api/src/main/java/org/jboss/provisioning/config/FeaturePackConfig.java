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
package org.jboss.provisioning.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.util.StringUtils;

/**
 * This class represents a feature-pack configuration to be installed.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackConfig extends PackageDepsConfig {

    public static class Builder extends PackageDepsConfigBuilder<Builder> {

        protected final ArtifactCoords.Gav gav;
        protected boolean inheritConfigs = true;
        protected boolean inheritModelOnlyConfigs = true;
        protected Set<String> includedModels = Collections.emptySet();
        protected Set<ConfigId> includedConfigs = Collections.emptySet();
        protected Map<String, Boolean> excludedModels = Collections.emptyMap();
        protected Map<String, Set<String>> excludedConfigs = Collections.emptyMap();
        protected Map<ConfigId, ConfigModel> definedConfigs = Collections.emptyMap();

        protected Builder(ArtifactCoords.Gav gav) {
            this(gav, true);
        }

        protected Builder(ArtifactCoords.Gav gav, boolean inheritPackages) {
            this.gav = gav;
            this.inheritPackages = inheritPackages;
        }

        public Builder setInheritConfigs(boolean inherit) {
            this.inheritConfigs = inherit;
            return this;
        }

        public Builder setInheritModelOnlyConfigs(boolean inheritModelOnlyConfigs) {
            this.inheritModelOnlyConfigs = inheritModelOnlyConfigs;
            return this;
        }

        public Builder addConfig(ConfigModel config) throws ProvisioningDescriptionException {
            if(definedConfigs.containsKey(config.getId())) {
                throw new ProvisioningDescriptionException("Config model with id " + config.getId() + " has already been defined in the configuration of " + gav);
            }
            definedConfigs = PmCollections.putLinked(definedConfigs, config.getId(), config);
            return this;
        }

        public Builder excludeConfigModel(String model) throws ProvisioningDescriptionException {
            return excludeConfigModel(model, true);
        }

        public Builder excludeConfigModel(String model, boolean namedConfigsOnly) throws ProvisioningDescriptionException {
            if(includedModels.contains(model)) {
                throw new ProvisioningDescriptionException("Model " + model + " has been included");
            }
            excludedModels = PmCollections.put(excludedModels, model, namedConfigsOnly);
            return this;
        }

        public Builder includeConfigModel(String name) throws ProvisioningDescriptionException {
            if(excludedModels.containsKey(name)) {
                throw new ProvisioningDescriptionException("Model " + name + " has been excluded");
            }
            includedModels = PmCollections.add(includedModels, name);
            return this;
        }

        public Builder includeDefaultConfig(String model, String name) throws ProvisioningDescriptionException {
            return includeDefaultConfig(new ConfigId(model, name));
        }

        public Builder includeDefaultConfig(ConfigId configId) throws ProvisioningDescriptionException {
            if(includedConfigs.contains(configId)) {
                throw new ProvisioningDescriptionException("Config model with id " + configId + " has already been included into the configuration of " + gav);
            }
            includedConfigs = PmCollections.add(includedConfigs, configId);
            return this;
        }

        public Builder excludeDefaultConfig(String model, String name) {
            if(excludedConfigs.isEmpty()) {
                excludedConfigs = Collections.singletonMap(model, Collections.singleton(name));
                return this;
            }
            Set<String> names = excludedConfigs.get(model);
            if(names == null) {
                if(excludedConfigs.size() == 1) {
                    excludedConfigs = new HashMap<>(excludedConfigs);
                }
                excludedConfigs.put(model, Collections.singleton(name));
            } else {
                if(names.size() == 1) {
                    names = new HashSet<>(names);
                    if(excludedConfigs.size() == 1) {
                        excludedConfigs = Collections.singletonMap(model, names);
                    } else {
                        excludedConfigs.put(model, names);
                    }
                }
                names.add(name);
            }
            return this;
        }

        public FeaturePackConfig build() {
            return new FeaturePackConfig(this);
        }
    }

    public static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    public static Builder builder(ArtifactCoords.Gav gav, boolean inheritPackageSet) {
        return new Builder(gav, inheritPackageSet);
    }

    public static FeaturePackConfig forGav(ArtifactCoords.Gav gav) {
        return new Builder(gav).build();
    }

    private final ArtifactCoords.Gav gav;
    private final boolean inheritConfigs;
    private final boolean inheritModelOnlyConfigs;
    private final Set<String> includedModels;
    private final Map<String, Boolean> excludedModels;
    private final Set<ConfigId> includedConfigs;
    private final Map<String, Set<String>> excludedConfigs;
    private final Map<ConfigId, ConfigModel> definedConfigs;

    protected FeaturePackConfig(Builder builder) {
        super(builder);
        assert builder.gav != null : "gav is null";
        this.gav = builder.gav;
        this.inheritConfigs = builder.inheritConfigs;
        this.inheritModelOnlyConfigs = builder.inheritModelOnlyConfigs;
        this.includedModels = PmCollections.unmodifiable(builder.includedModels);
        this.excludedModels = PmCollections.unmodifiable(builder.excludedModels);
        this.includedConfigs = PmCollections.unmodifiable(builder.includedConfigs);
        this.excludedConfigs = PmCollections.unmodifiable(builder.excludedConfigs);
        this.definedConfigs = PmCollections.unmodifiable(builder.definedConfigs);
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean isInheritConfigs() {
        return inheritConfigs;
    }

    public boolean isInheritModelOnlyConfigs() {
        return inheritModelOnlyConfigs;
    }

    public boolean hasFullModelsIncluded() {
        return !includedModels.isEmpty();
    }

    public Set<String> getFullModelsIncluded() {
        return includedModels;
    }

    public boolean isConfigModelIncluded(ConfigId configId) {
        return includedModels.contains(configId.getModel());
    }

    public boolean hasFullModelsExcluded() {
        return !excludedModels.isEmpty();
    }

    public Map<String, Boolean> getFullModelsExcluded() {
        return excludedModels;
    }

    public boolean isConfigModelExcluded(ConfigId configId) {
        final Boolean namedOnly = excludedModels.get(configId.getModel());
        if(namedOnly == null) {
            return false;
        }
        return namedOnly ? configId.getName() != null : true;
    }

    public boolean hasExcludedConfigs() {
        return !excludedConfigs.isEmpty();
    }

    public boolean isConfigExcluded(ConfigId configId) {
        final Set<String> names = excludedConfigs.get(configId.getModel());
        return names == null ? false : names.contains(configId.getName());
    }

    public Set<String> getExcludedModels() {
        return excludedConfigs.keySet();
    }

    public Set<String> getExcludedConfigs(String model) {
        return excludedConfigs.get(model);
    }

    public boolean hasIncludedConfigs() {
        return !includedConfigs.isEmpty();
    }

    public boolean isConfigIncluded(ConfigId id) {
        return includedConfigs.contains(id);
    }

    public Set<ConfigId> getIncludedConfigs() {
        return includedConfigs;
    }

    public boolean hasDefinedConfigs() {
        return !definedConfigs.isEmpty();
    }

    public ConfigModel getDefinedConfig(ConfigId configId) {
        return definedConfigs.get(configId);
    }

    public Collection<ConfigModel> getDefinedConfigs() {
        return definedConfigs.values();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((definedConfigs == null) ? 0 : definedConfigs.hashCode());
        result = prime * result + ((excludedConfigs == null) ? 0 : excludedConfigs.hashCode());
        result = prime * result + ((excludedModels == null) ? 0 : excludedModels.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((includedConfigs == null) ? 0 : includedConfigs.hashCode());
        result = prime * result + ((includedModels == null) ? 0 : includedModels.hashCode());
        result = prime * result + (inheritConfigs ? 1231 : 1237);
        result = prime * result + (inheritModelOnlyConfigs ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeaturePackConfig other = (FeaturePackConfig) obj;
        if (definedConfigs == null) {
            if (other.definedConfigs != null)
                return false;
        } else if (!definedConfigs.equals(other.definedConfigs))
            return false;
        if (excludedConfigs == null) {
            if (other.excludedConfigs != null)
                return false;
        } else if (!excludedConfigs.equals(other.excludedConfigs))
            return false;
        if (excludedModels == null) {
            if (other.excludedModels != null)
                return false;
        } else if (!excludedModels.equals(other.excludedModels))
            return false;
        if (gav == null) {
            if (other.gav != null)
                return false;
        } else if (!gav.equals(other.gav))
            return false;
        if (includedConfigs == null) {
            if (other.includedConfigs != null)
                return false;
        } else if (!includedConfigs.equals(other.includedConfigs))
            return false;
        if (includedModels == null) {
            if (other.includedModels != null)
                return false;
        } else if (!includedModels.equals(other.includedModels))
            return false;
        if (inheritConfigs != other.inheritConfigs)
            return false;
        if (inheritModelOnlyConfigs != other.inheritModelOnlyConfigs)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(gav.toString());
        if(!inheritConfigs) {
            builder.append(" inheritConfigs=false");
        }
        if(!inheritModelOnlyConfigs) {
            builder.append(" inheritModelOnlyConfigs=false");
        }
        if(!this.excludedModels.isEmpty()) {
            builder.append(" excluded models ");
            StringUtils.append(builder, excludedModels.entrySet());
        }
        if(!excludedConfigs.isEmpty()) {
            builder.append(" excluded configs ");
            StringUtils.append(builder, excludedConfigs.entrySet());
        }
        if(!includedConfigs.isEmpty()) {
            builder.append(" included configs ");
            StringUtils.append(builder, includedConfigs);
        }
        if(!definedConfigs.isEmpty()) {
            builder.append(" defined configs ");
            StringUtils.append(builder, definedConfigs.values());
        }
        if(!inheritPackages) {
            builder.append(" inheritPackages=false");
        }
        if(!excludedPackages.isEmpty()) {
            final String[] array = excludedPackages.toArray(new String[excludedPackages.size()]);
            Arrays.sort(array);
            builder.append(" excluded ").append(Arrays.asList(array));
        }
        if(!includedPackages.isEmpty()) {
            final String[] array = includedPackages.keySet().toArray(new String[includedPackages.size()]);
            Arrays.sort(array);
            builder.append(" included ").append(Arrays.asList(array));
        }
        return builder.append("]").toString();
    }
}
