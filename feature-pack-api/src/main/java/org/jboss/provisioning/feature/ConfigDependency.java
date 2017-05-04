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

package org.jboss.provisioning.feature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigDependency {

    public static class Builder {

        private String configName;
        private boolean inheritFeatures = true;
        private Set<String> includedSpecs = Collections.emptySet();
        private Map<FeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
        private Set<String> excludedSpecs = Collections.emptySet();
        private Set<FeatureId> excludedFeatures = Collections.emptySet();

        private Builder(String configName, boolean inheritConfigs) {
            this.configName = configName;
            this.inheritFeatures = inheritConfigs;
        }

        public Builder setInheritFeatures(boolean inheritFeatures) {
            this.inheritFeatures = inheritFeatures;
            return this;
        }

        public Builder includeSpec(String spec) throws ProvisioningDescriptionException {
            if(excludedSpecs.contains(spec)) {
                throw new ProvisioningDescriptionException(spec + " spec has been explicitly excluded");
            }
            switch(includedSpecs.size()) {
                case 0:
                    includedSpecs = Collections.singleton(spec);
                    break;
                case 1:
                    includedSpecs = new LinkedHashSet<>(includedSpecs);
                default:
                    includedSpecs.add(spec);
            }
            return this;
        }

        public Builder includeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
            return includeFeature(featureId, null);
        }

        public Builder includeFeature(FeatureId featureId, FeatureConfig feature) throws ProvisioningDescriptionException {
            if(excludedFeatures.contains(featureId)) {
                throw new ProvisioningDescriptionException(featureId + " has been explicitly excluded");
            }
            switch(includedFeatures.size()) {
                case 0:
                    includedFeatures = Collections.singletonMap(featureId, feature);
                    break;
                case 1:
                    includedFeatures = new LinkedHashMap<>(includedFeatures);
                default:
                    includedFeatures.put(featureId, feature);
            }
            return this;
        }

        public Builder excludeSpec(String spec) throws ProvisioningDescriptionException {
            if(includedSpecs.contains(spec)) {
                throw new ProvisioningDescriptionException(spec + " spec has been inplicitly excluded");
            }
            switch(excludedSpecs.size()) {
                case 0:
                    excludedSpecs = Collections.singleton(spec);
                    break;
                case 1:
                    excludedSpecs = new HashSet<>(excludedSpecs);
                default:
                    excludedSpecs.add(spec);
            }
            return this;
        }

        public Builder excludeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
            if(includedFeatures.containsKey(featureId)) {
                throw new ProvisioningDescriptionException(featureId + " has been explicitly included");
            }
            switch(excludedFeatures.size()) {
                case 0:
                    excludedFeatures = Collections.singleton(featureId);
                    break;
                case 1:
                    excludedFeatures = new HashSet<>(excludedFeatures);
                default:
                    excludedFeatures.add(featureId);
            }
            return this;
        }

        public ConfigDependency build() {
            return new ConfigDependency(this);
        }
    }

    public static Builder builder(String configName) {
        return builder(configName, true);
    }

    public static Builder builder(String configName, boolean inheritFeatures) {
        return new Builder(configName, inheritFeatures);
    }

    final String configName;
    final boolean inheritFeatures;
    final Set<String> includedSpecs;
    final Map<FeatureId, FeatureConfig> includedFeatures;
    final Set<String> excludedSpecs;
    final Set<FeatureId> excludedFeatures;

    private ConfigDependency(Builder builder) {
        this.configName = builder.configName;
        this.inheritFeatures = builder.inheritFeatures;
        this.includedSpecs = builder.includedSpecs.size() > 1 ? Collections.unmodifiableSet(builder.includedSpecs) : builder.includedSpecs;
        this.excludedSpecs = builder.excludedSpecs.size() > 1 ? Collections.unmodifiableSet(builder.excludedSpecs) : builder.excludedSpecs;
        this.includedFeatures = builder.includedFeatures.size() > 1 ? Collections.unmodifiableMap(builder.includedFeatures) : builder.includedFeatures;
        this.excludedFeatures = builder.excludedFeatures.size() > 1 ? Collections.unmodifiableSet(builder.excludedFeatures) : builder.excludedFeatures;
    }

    boolean isExcluded(String spec) {
        return excludedSpecs.contains(spec);
    }

    boolean isExcluded(FeatureId featureId) {
        if (excludedFeatures.contains(featureId)) {
            return true;
        }
        if (excludedSpecs.contains(featureId.specName)) {
            return !includedFeatures.containsKey(featureId.specName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configName == null) ? 0 : configName.hashCode());
        result = prime * result + ((excludedFeatures == null) ? 0 : excludedFeatures.hashCode());
        result = prime * result + ((excludedSpecs == null) ? 0 : excludedSpecs.hashCode());
        result = prime * result + ((includedFeatures == null) ? 0 : includedFeatures.hashCode());
        result = prime * result + ((includedSpecs == null) ? 0 : includedSpecs.hashCode());
        result = prime * result + (inheritFeatures ? 1231 : 1237);
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
        ConfigDependency other = (ConfigDependency) obj;
        if (configName == null) {
            if (other.configName != null)
                return false;
        } else if (!configName.equals(other.configName))
            return false;
        if (excludedFeatures == null) {
            if (other.excludedFeatures != null)
                return false;
        } else if (!excludedFeatures.equals(other.excludedFeatures))
            return false;
        if (excludedSpecs == null) {
            if (other.excludedSpecs != null)
                return false;
        } else if (!excludedSpecs.equals(other.excludedSpecs))
            return false;
        if (includedFeatures == null) {
            if (other.includedFeatures != null)
                return false;
        } else if (!includedFeatures.equals(other.includedFeatures))
            return false;
        if (includedSpecs == null) {
            if (other.includedSpecs != null)
                return false;
        } else if (!includedSpecs.equals(other.includedSpecs))
            return false;
        if (inheritFeatures != other.inheritFeatures)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(configName != null) {
            buf.append(configName);
        }
        if(!includedSpecs.isEmpty()) {
            buf.append(" includedSpecs=");
            final Iterator<String> i = includedSpecs.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!excludedSpecs.isEmpty()) {
            buf.append(" exlcudedSpecs=");
            final Iterator<String> i = excludedSpecs.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!includedFeatures.isEmpty()) {
            buf.append(" includedFeatures=[");
            final Iterator<Map.Entry<FeatureId, FeatureConfig>> i = includedFeatures.entrySet().iterator();
            Map.Entry<FeatureId, FeatureConfig> entry = i.next();
            buf.append(entry.getKey());
            if(entry.getValue() != null) {
                buf.append("->").append(entry.getValue());
            }
            while(i.hasNext()) {
                entry = i.next();
                buf.append(',').append(entry.getKey());
                if(entry.getValue() != null) {
                    buf.append("->").append(entry.getValue());
                }
            }
            buf.append(']');
        }
        if(!excludedFeatures.isEmpty()) {
            buf.append(" exlcudedFeatures=");
            final Iterator<FeatureId> i = excludedFeatures.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        return buf.append(']').toString();
    }
}
