/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.descr;

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
import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes the feature-pack as it is available in the repository.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDescription {

    public static class Builder {

        private ArtifactCoords.Gav gav;
        private Map<ArtifactCoords.Ga, FeaturePackDependencyDescription> dependencies = Collections.emptyMap();
        private Set<String> dependencyNames = Collections.emptySet();
        private Set<String> defPackages = Collections.emptySet();
        private Map<String, PackageDescription> packages = Collections.emptyMap();
        private List<ArtifactCoords.Gav> provisioningPlugins = Collections.emptyList();

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

        public Builder addDefaultPackage(PackageDescription pkg) {
            markAsDefaultPackage(pkg.getName());
            addPackage(pkg);
            return this;
        }

        public Builder markAsDefaultPackage(String packageName) {
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

        public Builder addPackage(PackageDescription pkg) {
            assert pkg != null : "package is null";
            switch(packages.size()) {
                case 0:
                    packages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    packages = new HashMap<String, PackageDescription>(packages);
                default:
                    packages.put(pkg.getName(), pkg);
            }
            return this;
        }

        public Builder addDependency(ProvisionedFeaturePackDescription dependency) throws ProvisioningDescriptionException {
            return addDependency(null, dependency);
        }

        public Builder addDependency(String name, ProvisionedFeaturePackDescription dependency) throws ProvisioningDescriptionException {
            return addDependency(FeaturePackDependencyDescription.create(name, dependency));
        }

        public Builder addDependency(FeaturePackDependencyDescription dependency) throws ProvisioningDescriptionException {
            if(dependency.getName() != null) {
                if(dependencyNames.isEmpty()) {
                    dependencyNames = Collections.singleton(dependency.getName());
                } else if(dependencyNames.contains(dependency.getName())){
                    throw new ProvisioningDescriptionException(Errors.duplicateDependencyName(dependency.getName()));
                } else {
                    if(dependencyNames.size() == 1) {
                        dependencyNames = new HashSet<>(dependencyNames);
                    }
                    dependencyNames.add(dependency.getName());
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

        public Builder addAllDependencies(Collection<ProvisionedFeaturePackDescription> dependencies) throws ProvisioningDescriptionException {
            for(ProvisionedFeaturePackDescription dependency : dependencies) {
                addDependency(dependency);
            }
            return this;
        }

        public Builder addProvisioningPlugin(ArtifactCoords.Gav gav) {
            assert gav != null : "gav is null";
            switch(provisioningPlugins.size()) {
                case 0:
                    provisioningPlugins = Collections.singletonList(gav);
                    break;
                case 1:
                    provisioningPlugins = new ArrayList<ArtifactCoords.Gav>(provisioningPlugins);
                default:
                    provisioningPlugins.add(gav);
            }
            return this;
        }

        public FeaturePackDescription build() throws ProvisioningDescriptionException {
            // package dependency consistency check
            if (!packages.isEmpty()) {
                final Set<String> allPackageNames = packages.keySet();
                for (PackageDescription pkg : packages.values()) {
                    if (pkg.hasLocalDependencies()) {
                        List<String> notFound = Collections.emptyList();
                        for(PackageDependencyDescription pkgDep : pkg.getLocalDependencies().getDescriptions()) {
                            if(!allPackageNames.contains(pkgDep.getName())) {
                                switch(notFound.size()) {
                                    case 0:
                                        notFound = Collections.singletonList(pkgDep.getName());
                                        break;
                                    case 1:
                                        notFound = new ArrayList<>(notFound);
                                    default:
                                        notFound.add(pkgDep.getName());
                                }
                            }
                        }
                        if (!notFound.isEmpty()) {
                            throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependencies(pkg.getName(), notFound));
                        }
                    }
                }
            }

            return new FeaturePackDescription(this);
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    private final ArtifactCoords.Gav gav;
    private final Map<ArtifactCoords.Ga, FeaturePackDependencyDescription> dependencies;
    private final Set<String> defPackages;
    private final Map<String, PackageDescription> packages;
    private final List<ArtifactCoords.Gav> provisioningPlugins;

    protected FeaturePackDescription(Builder builder) {
        this.gav = builder.gav;
        this.defPackages = Collections.unmodifiableSet(builder.defPackages);
        this.packages = Collections.unmodifiableMap(builder.packages);
        this.dependencies = Collections.unmodifiableMap(builder.dependencies);
        this.provisioningPlugins = Collections.unmodifiableList(builder.provisioningPlugins);
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

    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    public boolean hasPackage(String name) {
        return packages.containsKey(name);
    }

    public Collection<PackageDescription> getPackages() {
        return packages.values();
    }

    public Set<String> getPackageNames() {
        return packages.keySet();
    }

    public PackageDescription getPackage(String name) {
        return packages.get(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<ArtifactCoords.Ga> getDependencyGaParts() {
        return dependencies.keySet();
    }

    public Collection<FeaturePackDependencyDescription> getDependencies() {
        return dependencies.values();
    }

    public FeaturePackDependencyDescription getDependency(ArtifactCoords.Ga gaPart) {
        return dependencies.get(gaPart);
    }

    public boolean hasProvisioningPlugins() {
        return !provisioningPlugins.isEmpty();
    }

    public List<ArtifactCoords.Gav> getProvisioningPlugins() {
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

        if(!packages.isEmpty()) {
            final List<String> names = new ArrayList<String>(packages.keySet());
            names.sort(null);
            for(String name : names) {
                packages.get(name).logContent(logger);
            }
        }

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
            for(ArtifactCoords.Gav gav : provisioningPlugins) {
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
        result = prime * result + ((packages == null) ? 0 : packages.hashCode());
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
        FeaturePackDescription other = (FeaturePackDescription) obj;
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
        if (packages == null) {
            if (other.packages != null)
                return false;
        } else if (!packages.equals(other.packages))
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
        if(!packages.isEmpty()) {
            buf.append("; packages=");
            final String[] array = packages.keySet().toArray(new String[packages.size()]);
            Arrays.sort(array);
            buf.append(packages.get(array[0]));
            for(int i = 1; i < array.length; ++i) {
                buf.append(',').append(packages.get(array[i]));
            }
        }
        if(!provisioningPlugins.isEmpty()) {
            buf.append("; provisioningPlugins=").append(provisioningPlugins);
        }
        return buf.append("]").toString();
    }
}
