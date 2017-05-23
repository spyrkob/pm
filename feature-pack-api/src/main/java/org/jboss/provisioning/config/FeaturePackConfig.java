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
import org.jboss.provisioning.feature.FeatureGroup;
import org.jboss.provisioning.parameters.PackageParameter;

/**
 * This class represents a feature-pack configuration to be installed.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackConfig {

    public static class Builder {

        protected final ArtifactCoords.Gav gav;
        protected FeatureGroup featureGroup;
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

        public Builder setFeatureGroup(FeatureGroup featureGroup) {
            this.featureGroup = featureGroup;
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
    private final FeatureGroup featureGroup;
    private final boolean inheritPackages;
    private final Set<String> excludedPackages;
    private final Map<String, PackageConfig> includedPackages;

    protected FeaturePackConfig(Builder builder) {
        assert builder.gav != null : "gav is null";
        this.gav = builder.gav;
        this.featureGroup = builder.featureGroup;
        this.inheritPackages = builder.inheritPackages;
        this.excludedPackages = builder.excludedPackages.size() > 1 ? Collections.unmodifiableSet(builder.excludedPackages) : builder.excludedPackages;
        this.includedPackages = builder.includedPackages.size() > 1 ? Collections.unmodifiableMap(builder.includedPackages) : builder.includedPackages;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean hasFeatureGroup() {
        return featureGroup != null;
    }

    public FeatureGroup getFeatureGroup() {
        return featureGroup;
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
        result = prime * result + ((featureGroup == null) ? 0 : featureGroup.hashCode());
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
        if (featureGroup == null) {
            if (other.featureGroup != null)
                return false;
        } else if (!featureGroup.equals(other.featureGroup))
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
        if(featureGroup != null) {
            builder.append(' ').append(featureGroup);
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
