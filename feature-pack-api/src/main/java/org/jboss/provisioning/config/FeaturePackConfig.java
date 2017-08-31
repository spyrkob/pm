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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.ConfigId;
import org.jboss.provisioning.feature.IncludedConfig;
import org.jboss.provisioning.parameters.PackageParameter;

/**
 * This class represents a feature-pack configuration to be installed.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackConfig {

    public static class Builder {

        protected final ArtifactCoords.Gav gav;
        protected boolean inheritConfigs = true;
        protected Set<String> includedModels = Collections.emptySet();
        protected Map<ConfigId, IncludedConfig> includedConfigs = Collections.emptyMap();
        protected Set<String> excludedModels = Collections.emptySet();
        protected Map<String, Set<String>> excludedConfigs = Collections.emptyMap();
        protected Map<String, Map<String, Config>> configModels = Collections.emptyMap();
        protected boolean inheritPackages = true;
        protected Set<String> excludedPackages = Collections.emptySet();
        protected Map<String, PackageConfig> includedPackages = Collections.emptyMap();

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

        public Builder setInheritPackages(boolean inheritSelectedPackages) {
            this.inheritPackages = inheritSelectedPackages;
            return this;
        }

        public Builder excludePackage(String packageName) throws ProvisioningDescriptionException {
            if(includedPackages.containsKey(packageName)) {
                throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageName));
            }
            if(!excludedPackages.contains(packageName)) {
                switch(excludedPackages.size()) {
                    case 0:
                        excludedPackages = Collections.singleton(packageName);
                        break;
                    case 1:
                        if(excludedPackages.contains(packageName)) {
                            return this;
                        }
                        excludedPackages = new HashSet<>(excludedPackages);
                    default:
                        excludedPackages.add(packageName);
                }
            }
            return this;
        }

        public Builder excludeAllPackages(Collection<String> packageNames) throws ProvisioningDescriptionException {
            for(String packageName : packageNames) {
                excludePackage(packageName);
            }
            return this;
        }

        public Builder includeAllPackages(Collection<PackageConfig> packageConfigs) throws ProvisioningDescriptionException {
            for(PackageConfig packageConfig : packageConfigs) {
                includePackage(packageConfig);
            }
            return this;
        }

        public Builder includePackage(String packageName) throws ProvisioningDescriptionException {
            return includePackage(PackageConfig.newInstance(packageName));
        }

        public Builder includePackage(PackageConfig packageConfig) throws ProvisioningDescriptionException {
            if(excludedPackages.contains(packageConfig.getName())) {
                throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageConfig.getName()));
            }

            if(includedPackages.isEmpty()) {
                includedPackages = Collections.singletonMap(packageConfig.getName(), packageConfig);
                return this;
            }

            PackageConfig included = includedPackages.get(packageConfig.getName());
            if(included == null) {
                if(includedPackages.size() == 1) {
                    includedPackages = new HashMap<>(includedPackages);
                }
                includedPackages.put(packageConfig.getName(), packageConfig);
                return this;
            }

            if(!packageConfig.hasParams()) {
                return this;
            }

            final PackageConfig.Builder builder = PackageConfig.builder(included);
            for (PackageParameter param : packageConfig.getParameters()) {
                builder.addParameter(param);
            }
            included = builder.build();

            if(includedPackages.size() == 1) {
                includedPackages = Collections.singletonMap(included.getName(), included);
            } else {
                includedPackages.put(included.getName(), included);
            }
            return this;
        }

        public Builder addConfig(Config config) throws ProvisioningDescriptionException {
            if(configModels.isEmpty()) {
                configModels = Collections.singletonMap(config.getModel(), Collections.singletonMap(config.getName(), config));
                return this;
            }
            Map<String, Config> modelConfigs = configModels.get(config.getModel());
            if(modelConfigs == null) {
                if(configModels.size() == 1) {
                    configModels = new HashMap<>(configModels);
                }
                configModels.put(config.getModel(), Collections.singletonMap(config.getName(), config));
                return this;
            } else {
                if(modelConfigs.size() == 1) {
                    modelConfigs = new HashMap<>(modelConfigs);
                    if(configModels.size() == 1) {
                        configModels = Collections.singletonMap(config.getModel(), modelConfigs);
                    } else {
                        configModels.put(config.getModel(), modelConfigs);
                    }
                }
                if(modelConfigs.put(config.getName(), config) != null) {
                    throw new ProvisioningDescriptionException("There can be only one instance of a config model with a particular name in a feature-pack config.");
                }
            }
            return this;
        }

        public Builder excludeModel(String name) throws ProvisioningDescriptionException {
            if(includedModels.contains(name)) {
                throw new ProvisioningDescriptionException("Model " + name + " has been included");
            }
            if(!excludedModels.contains(name)) {
                switch(excludedModels.size()) {
                    case 0:
                        excludedModels = Collections.singleton(name);
                        break;
                    case 1:
                        if(excludedModels.contains(name)) {
                            return this;
                        }
                        excludedModels = new HashSet<>(excludedModels);
                    default:
                        excludedModels.add(name);
                }
            }
            return this;
        }

        public Builder includeModel(String name) throws ProvisioningDescriptionException {
            if(excludedModels.contains(name)) {
                throw new ProvisioningDescriptionException("Model " + name + " has been excluded");
            }
            if(!includedModels.contains(name)) {
                switch(includedModels.size()) {
                    case 0:
                        includedModels = Collections.singleton(name);
                        break;
                    case 1:
                        if(includedModels.contains(name)) {
                            return this;
                        }
                        includedModels = new HashSet<>(includedModels);
                    default:
                        includedModels.add(name);
                }
            }
            return this;
        }

        public Builder includeDefaultConfig(String model, String name) throws ProvisioningDescriptionException {
            return includeDefaultConfig(IncludedConfig.builder(model, name).build());
        }

        public Builder includeDefaultConfig(IncludedConfig includedConfig) throws ProvisioningDescriptionException {
            if(includedConfigs.isEmpty()) {
                includedConfigs = Collections.singletonMap(includedConfig.getId(), includedConfig);
                return this;
            }
            if(includedConfigs.containsKey(includedConfig.getId())) {
                throw new ProvisioningDescriptionException("Duplicate config " + includedConfig.getId());
            }
            if(includedConfigs.size() == 1) {
                final Map.Entry<ConfigId, IncludedConfig> first = includedConfigs.entrySet().iterator().next();
                includedConfigs = new HashMap<>(2);
                includedConfigs.put(first.getKey(), first.getValue());
            }
            includedConfigs.put(includedConfig.getId(), includedConfig);
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
    private final Set<String> includedModels;
    private final Set<String> excludedModels;
    private final Map<ConfigId, IncludedConfig> includedConfigs;
    private final Map<String, Set<String>> excludedConfigs;
    private final Map<String, Map<String, Config>> configModels;
    private final boolean inheritPackages;
    private final Set<String> excludedPackages;
    private final Map<String, PackageConfig> includedPackages;

    protected FeaturePackConfig(Builder builder) {
        assert builder.gav != null : "gav is null";
        this.gav = builder.gav;
        this.inheritConfigs = builder.inheritConfigs;
        this.includedModels = builder.includedModels.size() > 1 ? Collections.unmodifiableSet(builder.includedModels) : builder.includedModels;
        this.excludedModels = builder.excludedModels.size() > 1 ? Collections.unmodifiableSet(builder.excludedModels) : builder.excludedModels;
        this.includedConfigs = builder.includedConfigs;
        this.excludedConfigs = builder.excludedConfigs;
        this.configModels = builder.configModels;
        this.inheritPackages = builder.inheritPackages;
        this.excludedPackages = builder.excludedPackages.size() > 1 ? Collections.unmodifiableSet(builder.excludedPackages) : builder.excludedPackages;
        this.includedPackages = builder.includedPackages.size() > 1 ? Collections.unmodifiableMap(builder.includedPackages) : builder.includedPackages;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean isInheritConfigs() {
        return this.inheritConfigs;
    }

    public boolean hasFullModelsIncluded() {
        return !includedModels.isEmpty();
    }

    public Set<String> getFullModelsIncluded() {
        return includedModels;
    }

    public boolean isFullModelIncluded(String name) {
        return includedModels.contains(name);
    }

    public boolean hasFullModelsExcluded() {
        return !excludedModels.isEmpty();
    }

    public Set<String> getFullModelsExcluded() {
        return excludedModels;
    }

    public boolean isFullModelExcluded(String name) {
        return excludedModels.contains(name);
    }

    public boolean hasExcludedConfigs() {
        return !excludedConfigs.isEmpty();
    }

    public boolean isConfigExcluded(String model, String name) {
        final Set<String> names = excludedConfigs.get(model);
        return names == null ? false : names.contains(name);
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
        return includedConfigs.containsKey(id);
    }

    public Collection<IncludedConfig> getIncludedConfigs() {
        return includedConfigs.values();
    }

    public IncludedConfig getIncludedConfig(ConfigId id) {
        return includedConfigs.get(id);
    }

    public boolean hasDefinedConfigs() {
        return !this.configModels.isEmpty();
    }

    public Set<String> getDefinedConfigModels() {
        return configModels.keySet();
    }

    public Collection<Config> getDefinedConfigs(String model) {
        final Map<String, Config> configMap = configModels.get(model);
        return configMap == null ? Collections.emptyList() : configMap.values();
    }

    public boolean isInheritPackages() {
        return inheritPackages;
    }

    public boolean hasIncludedPackages() {
        return !includedPackages.isEmpty();
    }

    public boolean isIncluded(String packageName) {
        return includedPackages.containsKey(packageName);
    }

    public PackageConfig getIncludedPackage(String packageName) {
        return includedPackages.get(packageName);
    }

    public Collection<PackageConfig> getIncludedPackages() {
        return includedPackages.values();
    }

    public boolean hasExcludedPackages() {
        return !excludedPackages.isEmpty();
    }

    public boolean isExcluded(String packageName) {
        return excludedPackages.contains(packageName);
    }

    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configModels == null) ? 0 : configModels.hashCode());
        result = prime * result + ((excludedConfigs == null) ? 0 : excludedConfigs.hashCode());
        result = prime * result + ((excludedModels == null) ? 0 : excludedModels.hashCode());
        result = prime * result + ((excludedPackages == null) ? 0 : excludedPackages.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((includedConfigs == null) ? 0 : includedConfigs.hashCode());
        result = prime * result + ((includedModels == null) ? 0 : includedModels.hashCode());
        result = prime * result + ((includedPackages == null) ? 0 : includedPackages.hashCode());
        result = prime * result + (inheritConfigs ? 1231 : 1237);
        result = prime * result + (inheritPackages ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeaturePackConfig other = (FeaturePackConfig) obj;
        if (configModels == null) {
            if (other.configModels != null)
                return false;
        } else if (!configModels.equals(other.configModels))
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
        if (excludedPackages == null) {
            if (other.excludedPackages != null)
                return false;
        } else if (!excludedPackages.equals(other.excludedPackages))
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
        if (includedPackages == null) {
            if (other.includedPackages != null)
                return false;
        } else if (!includedPackages.equals(other.includedPackages))
            return false;
        if (inheritConfigs != other.inheritConfigs)
            return false;
        if (inheritPackages != other.inheritPackages)
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
        if(!includedConfigs.isEmpty()) {
            builder.append(" included configs ");
            final Iterator<Map.Entry<ConfigId, IncludedConfig>> i = includedConfigs.entrySet().iterator();
            IncludedConfig config = i.next().getValue();
            builder.append(config);
            while(i.hasNext()) {
                builder.append(',').append(i.next().getValue());
            }
        }
        if(!configModels.isEmpty()) {
            final Iterator<Entry<String, Map<String, Config>>> modelConfigs = configModels.entrySet().iterator();
            Entry<String, Map<String, Config>> modelConfig = modelConfigs.next();
            if(modelConfig.getKey() != null) {
                builder.append(" model=").append(modelConfig.getKey()).append('{');
            } else {
                builder.append(" default-model{");
            }

            Iterator<Config> configs = modelConfig.getValue().values().iterator();
            Config config = configs.next();
            builder.append(config);
            while(configs.hasNext()) {
                builder.append(',').append(configs.next());
            }
            builder.append('}');
            while(modelConfigs.hasNext()) {
                builder.append(',');
                modelConfig = modelConfigs.next();
                if(modelConfig.getKey() != null) {
                    builder.append(", model=").append(modelConfig.getKey()).append('{');
                } else {
                    builder.append(", default-model{");
                }
                configs = modelConfig.getValue().values().iterator();
                config = configs.next();
                builder.append(config);
                while(configs.hasNext()) {
                    builder.append(',').append(configs.next());
                }
                builder.append('}');
            }
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
