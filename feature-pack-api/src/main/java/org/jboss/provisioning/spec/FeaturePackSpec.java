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
package org.jboss.provisioning.spec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes the feature-pack as it is available in the repository.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackSpec {

    public static class Builder {

        private ArtifactCoords.Gav gav;
        private Map<ArtifactCoords.Ga, FeaturePackDependencySpec> dependencies = Collections.emptyMap();
        private Map<String, FeaturePackDependencySpec> dependencyByName = Collections.emptyMap();
        private Set<String> defPackages = Collections.emptySet();
        private Set<String> defConfigs = Collections.emptySet();
        private Config unnamedConfig;

        protected Builder() {
            this(null);
        }

        protected Builder(ArtifactCoords.Gav gav) {
            this.gav = gav;
        }

        public Builder setGav(ArtifactCoords.Gav gav) {
            this.gav = gav;
            return this;
        }

        public ArtifactCoords.Gav getGav() {
            return gav;
        }

        public Builder addDefaultPackage(String packageName) {
            assert packageName != null : "packageName is null";
            switch(defPackages.size()) {
                case 0:
                    defPackages = Collections.singleton(packageName);
                    break;
                case 1:
                    defPackages = new HashSet<String>(defPackages);
                default:
                    defPackages.add(packageName);
            }
            return this;
        }

        public Builder addDefaultConfig(String configName) throws ProvisioningDescriptionException {
            assert configName != null : "configName is null";
            if(unnamedConfig != null) {
                throw new ProvisioningDescriptionException("Feature-pack already has default unnamed config");
            }
            switch(defConfigs.size()) {
                case 0:
                    defConfigs = Collections.singleton(configName);
                    break;
                case 1:
                    defConfigs = new HashSet<String>(defConfigs);
                default:
                    defConfigs.add(configName);
            }
            return this;
        }

        public Builder setConfig(Config config) throws ProvisioningDescriptionException {
            if(config.getName() != null) {
                throw new ProvisioningDescriptionException("Config included into feature-pack spec cannot have a name");
            }
            if (!defConfigs.isEmpty()) {
                throw new ProvisioningDescriptionException("Unnamed config cannot be mixed with named configs");
            }
            if (unnamedConfig != null) {
                throw new ProvisioningDescriptionException("There could be only one unnamed config");
            }
            this.unnamedConfig = config;
            return this;
        }

        public Builder addDependency(FeaturePackConfig dependency) throws ProvisioningDescriptionException {
            return addDependency(null, dependency);
        }

        public Builder addDependency(String name, FeaturePackConfig dependency) throws ProvisioningDescriptionException {
            return addDependency(FeaturePackDependencySpec.create(name, dependency));
        }

        public Builder addDependency(FeaturePackDependencySpec dependency) throws ProvisioningDescriptionException {
            if(dependency.getName() != null) {
                if(dependencyByName.isEmpty()) {
                    dependencyByName = Collections.singletonMap(dependency.getName(), dependency);
                } else if(dependencyByName.containsKey(dependency.getName())){
                    throw new ProvisioningDescriptionException(Errors.duplicateDependencyName(dependency.getName()));
                } else {
                    if(dependencyByName.size() == 1) {
                        dependencyByName = new HashMap<>(dependencyByName);
                    }
                    dependencyByName.put(dependency.getName(), dependency);
                }
            }
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(dependency.getTarget().getGav().toGa(), dependency);
                    break;
                case 1:
                    dependencies = new LinkedHashMap<>(dependencies);
                default:
                    dependencies.put(dependency.getTarget().getGav().toGa(), dependency);
            }
            return this;
        }

        public Builder addAllDependencies(Collection<FeaturePackConfig> dependencies) throws ProvisioningDescriptionException {
            for(FeaturePackConfig dependency : dependencies) {
                addDependency(dependency);
            }
            return this;
        }

        public FeaturePackSpec build() throws ProvisioningDescriptionException {
            return new FeaturePackSpec(this);
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    private final ArtifactCoords.Gav gav;
    private final Map<ArtifactCoords.Ga, FeaturePackDependencySpec> dependencies;
    private final Map<String, FeaturePackDependencySpec> dependencyByName;
    private final Set<String> defPackages;
    private final Set<String> defConfigs;
    private final Config unnamedConfig;

    protected FeaturePackSpec(Builder builder) {
        this.gav = builder.gav;
        this.defPackages = builder.defPackages.size() > 1 ? Collections.unmodifiableSet(builder.defPackages) : builder.defPackages;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableMap(builder.dependencies) : builder.dependencies;
        this.dependencyByName = builder.dependencyByName.size() > 1 ? Collections.unmodifiableMap(builder.dependencyByName) : builder.dependencyByName;
        this.defConfigs = builder.defConfigs.size() > 1 ? Collections.unmodifiableSet(builder.defConfigs) : builder.defConfigs;
        this.unnamedConfig = builder.unnamedConfig;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean hasUnnamedConfig() {
        return unnamedConfig != null;
    }

    public Config getUnnamedConfig() {
        return unnamedConfig;
    }

    public boolean hasDefaultConfigs() {
        return !defConfigs.isEmpty();
    }

    public Set<String> getDefaultConfigs() {
        return defConfigs;
    }

    public boolean hasDefaultPackages() {
        return !defPackages.isEmpty();
    }

    public Set<String> getDefaultPackageNames() {
        return defPackages;
    }

    public boolean isDefaultPackage(String name) {
        return defPackages.contains(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<ArtifactCoords.Ga> getDependencyGaParts() {
        return dependencies.keySet();
    }

    public Collection<FeaturePackDependencySpec> getDependencies() {
        return dependencies.values();
    }

    public FeaturePackDependencySpec getDependency(ArtifactCoords.Ga gaPart) {
        return dependencies.get(gaPart);
    }

    public FeaturePackDependencySpec getDependency(String name) {
        return dependencyByName.get(name);
    }

    public String logContent() throws IOException {
        final DescrFormatter logger = new DescrFormatter();
        logContent(logger);
        return logger.toString();
    }

    void logContent(DescrFormatter logger) throws IOException {
        logger.print("FeaturePack ");
        logger.println(gav.toString());
        logger.increaseOffset();

        if(!dependencies.isEmpty()) {
            logger.println("Dependencies:");
            logger.increaseOffset();
            for(ArtifactCoords.Ga ga : dependencies.keySet()) {
                logger.println(ga.toGav().toString());
            }
            logger.decreaseOffset();
        }
        logger.decreaseOffset();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defConfigs == null) ? 0 : defConfigs.hashCode());
        result = prime * result + ((defPackages == null) ? 0 : defPackages.hashCode());
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((dependencyByName == null) ? 0 : dependencyByName.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((unnamedConfig == null) ? 0 : unnamedConfig.hashCode());
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
        FeaturePackSpec other = (FeaturePackSpec) obj;
        if (defConfigs == null) {
            if (other.defConfigs != null)
                return false;
        } else if (!defConfigs.equals(other.defConfigs))
            return false;
        if (defPackages == null) {
            if (other.defPackages != null)
                return false;
        } else if (!defPackages.equals(other.defPackages))
            return false;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (dependencyByName == null) {
            if (other.dependencyByName != null)
                return false;
        } else if (!dependencyByName.equals(other.dependencyByName))
            return false;
        if (gav == null) {
            if (other.gav != null)
                return false;
        } else if (!gav.equals(other.gav))
            return false;
        if (unnamedConfig == null) {
            if (other.unnamedConfig != null)
                return false;
        } else if (!unnamedConfig.equals(other.unnamedConfig))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[gav=").append(gav);
        if(!dependencies.isEmpty()) {
            buf.append("; dependencies: ");
            final ArtifactCoords.Ga[] array = dependencies.keySet().toArray(new ArtifactCoords.Ga[dependencies.size()]);
            Arrays.sort(array);
            buf.append(dependencies.get(array[0]));
            for(int i = 1; i < array.length; ++i) {
                buf.append(',').append(dependencies.get(array[i]));
            }
        }
        if(unnamedConfig != null) {
            buf.append("; config=").append(unnamedConfig);
        }
        if(!defConfigs.isEmpty()) {
            buf.append("; defaultConfigs: ");
            final String[] array = defConfigs.toArray(new String[defConfigs.size()]);
            Arrays.sort(array);
            buf.append(array[0]);
            for(int i = 1; i < array.length; ++i) {
                buf.append(',').append(array[i]);
            }
        }
        if(!defPackages.isEmpty()) {
            buf.append("; defaultPackages: ");
            final String[] array = defPackages.toArray(new String[defPackages.size()]);
            Arrays.sort(array);
            buf.append(array[0]);
            for(int i = 1; i < array.length; ++i) {
                buf.append(',').append(array[i]);
            }
        }
        return buf.append("]").toString();
    }
}
