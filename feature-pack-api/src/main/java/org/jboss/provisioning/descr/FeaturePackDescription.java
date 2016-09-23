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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.Gav;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes the feature-pack as it is available in the repository.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDescription {

    public static class Builder {

        private Gav gav;
        private Map<Gav, ProvisionedFeaturePackDescription> dependencies = Collections.emptyMap();
        private Set<String> topPackages = Collections.emptySet();
        private Map<String, PackageDescription> packages = Collections.emptyMap();
        private List<Gav> provisioningPlugins = Collections.emptyList();

        protected Builder() {
            this(null);
        }

        protected Builder(Gav gav) {
            this.gav = gav;
        }

        public Builder setGAV(Gav gav) {
            this.gav = gav;
            return this;
        }

        public Builder addTopPackage(PackageDescription pkg) {
            addTopPackageName(pkg.getName());
            addPackage(pkg);
            return this;
        }

        public Builder addTopPackageName(String packageName) {
            assert packageName != null : "packageName is null";
            switch(topPackages.size()) {
                case 0:
                    topPackages = Collections.singleton(packageName);
                    break;
                case 1:
                    topPackages = new HashSet<String>(topPackages);
                default:
                    topPackages.add(packageName);
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

        public Builder addDependency(ProvisionedFeaturePackDescription dependency) {
            assert gav != null : "Gav is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(dependency.getGav(), dependency);
                    break;
                case 1:
                    dependencies = new HashMap<Gav, ProvisionedFeaturePackDescription>(dependencies);
                default:
                    dependencies.put(dependency.getGav(), dependency);
            }
            return this;
        }

        public Builder addAllDependencies(Collection<ProvisionedFeaturePackDescription> dependencies) {
            for(ProvisionedFeaturePackDescription dependency : dependencies) {
                addDependency(dependency);
            }
            return this;
        }

        public Builder addProvisioningPlugin(Gav gav) {
            assert gav != null : "gav is null";
            switch(provisioningPlugins.size()) {
                case 0:
                    provisioningPlugins = Collections.singletonList(gav);
                    break;
                case 1:
                    provisioningPlugins = new ArrayList<Gav>(provisioningPlugins);
                default:
                    provisioningPlugins.add(gav);
            }
            return this;
        }

        public FeaturePackDescription build() {
            return new FeaturePackDescription(gav, Collections.unmodifiableSet(topPackages), Collections.unmodifiableMap(packages),
                    Collections.unmodifiableMap(dependencies), Collections.unmodifiableList(provisioningPlugins));
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(Gav gav) {
        return new Builder(gav);
    }

    private final Gav gav;
    private final Map<Gav, ProvisionedFeaturePackDescription> dependencies;
    private final Set<String> topPackages;
    private final Map<String, PackageDescription> packages;
    private final List<Gav> provisioningPlugins;

    protected FeaturePackDescription(Gav gav, Set<String> topPackages, Map<String, PackageDescription> packages,
            Map<Gav, ProvisionedFeaturePackDescription> dependencies,
            List<Gav> provisioningPlugins) {
        assert gav != null : "Gav is null";
        assert dependencies != null : "dependencies is null";
        assert topPackages != null : "topPackages is null";
        assert packages != null : "packages is null";
        this.gav = gav;
        this.topPackages = topPackages;
        this.packages = packages;
        this.dependencies = dependencies;
        this.provisioningPlugins = provisioningPlugins;
    }

    public Gav getGav() {
        return gav;
    }

    public boolean hasTopPackages() {
        return !topPackages.isEmpty();
    }

    public Set<String> getTopPackageNames() {
        return topPackages;
    }

    public boolean isTopPackage(String name) {
        return topPackages.contains(name);
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

    public PackageDescription getPackageDescription(String name) {
        return packages.get(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<Gav> getDependencyGAVs() {
        return dependencies.keySet();
    }

    public Collection<ProvisionedFeaturePackDescription> getDependencies() {
        return dependencies.values();
    }

    public ProvisionedFeaturePackDescription getDependency(Gav gav) {
        return dependencies.get(gav);
    }

    public boolean hasProvisioningPlugins() {
        return !provisioningPlugins.isEmpty();
    }

    public List<Gav> getProvisioningPlugins() {
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
            for(Gav gav : dependencies.keySet()) {
                logger.println(gav.toString());
            }
            logger.decreaseOffset();
        }

        if(!provisioningPlugins.isEmpty()) {
            logger.println("Provisioning plugins:").increaseOffset();
            for(Gav gav : provisioningPlugins) {
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
        result = prime * result + ((topPackages == null) ? 0 : topPackages.hashCode());
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
        if (topPackages == null) {
            if (other.topPackages != null)
                return false;
        } else if (!topPackages.equals(other.topPackages))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FeaturePackDescription [gav=" + gav + ", dependencies=" + dependencies + ", topPackages=" + topPackages
                + ", packages=" + packages + ", provisioningPlugins=" + provisioningPlugins + "]";
    }
}
