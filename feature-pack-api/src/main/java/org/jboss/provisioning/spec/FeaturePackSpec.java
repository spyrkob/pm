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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
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
        private List<ArtifactCoords> provisioningPlugins = Collections.emptyList();

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

        public Builder addProvisioningPlugin(ArtifactCoords coords) {
            assert coords != null : "gav is null";
            switch(provisioningPlugins.size()) {
                case 0:
                    provisioningPlugins = Collections.singletonList(coords);
                    break;
                case 1:
                    provisioningPlugins = new ArrayList<ArtifactCoords>(provisioningPlugins);
                default:
                    provisioningPlugins.add(coords);
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
    private final List<ArtifactCoords> provisioningPlugins;

    protected FeaturePackSpec(Builder builder) {
        this.gav = builder.gav;
        this.defPackages = builder.defPackages.size() > 1 ? Collections.unmodifiableSet(builder.defPackages) : builder.defPackages;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableMap(builder.dependencies) : builder.dependencies;
        this.dependencyByName = builder.dependencyByName.size() > 1 ? Collections.unmodifiableMap(builder.dependencyByName) : builder.dependencyByName;
        this.provisioningPlugins = builder.provisioningPlugins.size() > 1 ? Collections.unmodifiableList(builder.provisioningPlugins) : builder.provisioningPlugins;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
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

    public boolean hasProvisioningPlugins() {
        return !provisioningPlugins.isEmpty();
    }

    public List<ArtifactCoords> getProvisioningPlugins() {
        return provisioningPlugins;
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

        if(!provisioningPlugins.isEmpty()) {
            logger.println("Provisioning plugins:").increaseOffset();
            for(ArtifactCoords gav : provisioningPlugins) {
                logger.println(gav.toString());
            }
            logger.decreaseOffset();
        }
        logger.decreaseOffset();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((provisioningPlugins == null) ? 0 : provisioningPlugins.hashCode());
        result = prime * result + ((defPackages == null) ? 0 : defPackages.hashCode());
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
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (gav == null) {
            if (other.gav != null)
                return false;
        } else if (!gav.equals(other.gav))
            return false;
        if (provisioningPlugins == null) {
            if (other.provisioningPlugins != null)
                return false;
        } else if (!provisioningPlugins.equals(other.provisioningPlugins))
            return false;
        if (defPackages == null) {
            if (other.defPackages != null)
                return false;
        } else if (!defPackages.equals(other.defPackages))
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
        if(!defPackages.isEmpty()) {
            buf.append("; defaultPackages: ");
            final String[] array = defPackages.toArray(new String[defPackages.size()]);
            Arrays.sort(array);
            buf.append(array[0]);
            for(int i = 1; i < array.length; ++i) {
                buf.append(',').append(array[i]);
            }
        }
        if(!provisioningPlugins.isEmpty()) {
            buf.append("; provisioningPlugins=").append(provisioningPlugins);
        }
        return buf.append("]").toString();
    }
}
