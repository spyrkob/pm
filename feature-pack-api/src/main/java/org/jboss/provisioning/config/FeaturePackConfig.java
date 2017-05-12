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
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.spec.FeaturePackSpec;

/**
 * This class represents a feature-pack configuration to be installed.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackConfig {

    public static class Builder {

        protected final ArtifactCoords.Gav gav;
        protected Config config;
        protected boolean inheritPackages = true;
        protected Set<String> excludedPackages = Collections.emptySet();
        protected Map<String, PackageConfig> includedPackages = Collections.emptyMap();
        protected FeaturePackSpec fpSpec;

        protected Builder(ArtifactCoords.Gav gav) {
            this(gav, true);
        }

        protected Builder(ArtifactCoords.Gav gav, boolean inheritPackages) {
            this.gav = gav;
            this.inheritPackages = inheritPackages;
        }

        protected Builder(FeaturePackSpec fpSpec, FeaturePackConfig fpConfig) {
            this.gav = fpConfig.getGav();
            this.fpSpec = fpSpec;
            inheritPackages = fpConfig.inheritPackages;
            excludedPackages = fpConfig.excludedPackages.size() > 1 ? new HashSet<>(fpConfig.excludedPackages) : fpConfig.excludedPackages;
            includedPackages = fpConfig.includedPackages.size() > 1 ? new HashMap<>(fpConfig.includedPackages) : fpConfig.includedPackages;
        }

        public Builder setConfig(Config config) {
            this.config = config;
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

        private void removeFromIncluded(String packageName) {
            if(!includedPackages.containsKey(packageName)) {
                return;
            }
            if(includedPackages.size() == 1) {
                includedPackages = Collections.emptyMap();
            } else {
                includedPackages.remove(packageName);
            }
        }

        private void removeFromExcluded(String packageName) {
            if(!excludedPackages.contains(packageName)) {
                return;
            }
            if(excludedPackages.size() == 1) {
                excludedPackages = Collections.emptySet();
            } else {
                excludedPackages.remove(packageName);
            }
        }

        public Builder merge(FeaturePackConfig other) throws ProvisioningDescriptionException {
            assertSameGav(other);

            if(inheritPackages == other.inheritPackages) {
                // this.includes + other.includes
                // this.excludes - other.includes
                // common excludes stay
                if(other.hasIncludedPackages()) {
                    for(PackageConfig pkgConfig : other.includedPackages.values()) {
                        includePackage(pkgConfig);
                        removeFromExcluded(pkgConfig.getName());
                    }
                }
                if (!excludedPackages.isEmpty()) {
                    if (other.hasExcludedPackages()) {
                        Set<String> tmp = Collections.emptySet();
                        for (String name : other.excludedPackages) {
                            if (excludedPackages.contains(name)) {
                                switch(tmp.size()) {
                                    case 0:
                                        tmp = Collections.singleton(name);
                                        break;
                                    case 1:
                                        tmp = new HashSet<>(tmp);
                                    default:
                                        tmp.add(name);
                                }
                            }
                        }
                        excludedPackages = tmp;
                    } else {
                        excludedPackages = Collections.emptySet();
                    }
                }
            } else if (inheritPackages) {
                //this.excludes - other.includes
                if(other.hasIncludedPackages()) {
                    for(PackageConfig pkgConfig : other.includedPackages.values()) {
                        removeFromExcluded(pkgConfig.getName());
                        includePackage(pkgConfig);
                    }
                }
            } else {
                // this.excludes = other.excludes - this.includes
                // inheritPackages = true
                excludedPackages = new HashSet<>(other.excludedPackages);
                if(!includedPackages.isEmpty()) {
                    for(String name : includedPackages.keySet()) {
                        removeFromExcluded(name);
                    }
                }
                inheritPackages = true;
            }
            return this;
        }

        public Builder enforce(FeaturePackConfig other) throws ProvisioningDescriptionException {
            assertSameGav(other);

            this.inheritPackages = other.inheritPackages;
            this.includedPackages = other.includedPackages;
            this.excludedPackages = other.excludedPackages;

            return this;
        }

        private void assertSameGav(FeaturePackConfig other) {
            if(!gav.equals(other.gav)) {
                throw new IllegalArgumentException("Feature pack GAVs don't match " + gav + " vs " + other.gav);
            }
        }

        public FeaturePackConfig build() {
            if(fpSpec != null) {
                // remove redundant explicit excludes/includes
                if(inheritPackages) {
                    if(!includedPackages.isEmpty() && fpSpec.hasDefaultPackages()) {
                        for(String name : fpSpec.getDefaultPackageNames()) {
                            final PackageConfig packageConfig = includedPackages.get(name);
                            if(packageConfig != null && !packageConfig.hasParams()) {
                                removeFromIncluded(name);
                            }
                        }
                    }
                } else {
                    if(!excludedPackages.isEmpty() && fpSpec.hasDefaultPackages()) {
                        for(String name : fpSpec.getDefaultPackageNames()) {
                            if(excludedPackages.contains(name)) {
                                removeFromExcluded(name);
                            }
                        }
                    }
                }
            }
            return new FeaturePackConfig(this);
        }
    }

    public static Builder builder(FeaturePackSpec fpSpec, FeaturePackConfig fpConfig) {
        return new Builder(fpSpec, fpConfig);
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
    private final Config config;
    private final boolean inheritPackages;
    private final Set<String> excludedPackages;
    private final Map<String, PackageConfig> includedPackages;

    protected FeaturePackConfig(Builder builder) {
        assert builder.gav != null : "gav is null";
        this.gav = builder.gav;
        this.config = builder.config;
        this.inheritPackages = builder.inheritPackages;
        this.excludedPackages = builder.excludedPackages.size() > 1 ? Collections.unmodifiableSet(builder.excludedPackages) : builder.excludedPackages;
        this.includedPackages = builder.includedPackages.size() > 1 ? Collections.unmodifiableMap(builder.includedPackages) : builder.includedPackages;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean hasConfig() {
        return config != null;
    }

    public Config getConfig() {
        return config;
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
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((excludedPackages == null) ? 0 : excludedPackages.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((includedPackages == null) ? 0 : includedPackages.hashCode());
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
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
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
        if (includedPackages == null) {
            if (other.includedPackages != null)
                return false;
        } else if (!includedPackages.equals(other.includedPackages))
            return false;
        if (inheritPackages != other.inheritPackages)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(gav.toString());
        if(config != null) {
            builder.append(' ').append(config);
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
