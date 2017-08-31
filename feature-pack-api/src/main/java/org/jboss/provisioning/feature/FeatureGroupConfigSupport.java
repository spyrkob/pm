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
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeatureGroupConfigSupport {

    final String name;
    final boolean inheritFeatures;
    final Set<SpecId> includedSpecs;
    final Map<FeatureId, FeatureConfig> includedFeatures;
    final Set<SpecId> excludedSpecs;
    final Set<FeatureId> excludedFeatures;

    protected FeatureGroupConfigSupport(String name) {
        this.name = name;
        this.inheritFeatures = true;
        this.includedSpecs = Collections.emptySet();
        this.includedFeatures = Collections.emptyMap();
        this.excludedSpecs = Collections.emptySet();
        this.excludedFeatures = Collections.emptySet();
    }

    protected FeatureGroupConfigSupport(FeatureGroupConfigBuilderSupport<?, ?> builder) {
        this.name = builder.name;
        this.inheritFeatures = builder.inheritFeatures;
        this.includedSpecs = builder.includedSpecs.size() > 1 ? Collections.unmodifiableSet(builder.includedSpecs) : builder.includedSpecs;
        this.excludedSpecs = builder.excludedSpecs.size() > 1 ? Collections.unmodifiableSet(builder.excludedSpecs) : builder.excludedSpecs;
        this.includedFeatures = builder.includedFeatures.size() > 1 ? Collections.unmodifiableMap(builder.includedFeatures) : builder.includedFeatures;
        this.excludedFeatures = builder.excludedFeatures.size() > 1 ? Collections.unmodifiableSet(builder.excludedFeatures) : builder.excludedFeatures;
    }

    public String getName() {
        return name;
    }

    public boolean isInheritFeatures() {
        return inheritFeatures;
    }

    public boolean hasExcludedSpecs() {
        return !excludedSpecs.isEmpty();
    }

    public Set<SpecId> getExcludedSpecs() {
        return excludedSpecs;
    }

    public boolean hasIncludedSpecs() {
        return !includedSpecs.isEmpty();
    }

    public Set<SpecId> getIncludedSpecs() {
        return includedSpecs;
    }

    public boolean hasExcludedFeatures() {
        return !excludedFeatures.isEmpty();
    }

    public Set<FeatureId> getExcludedFeatures() {
        return excludedFeatures;
    }

    public boolean hasIncludedFeatures() {
        return !includedFeatures.isEmpty();
    }

    public Map<FeatureId, FeatureConfig> getIncludedFeatures() {
        return includedFeatures;
    }

    boolean isExcluded(SpecId spec) {
        return excludedSpecs.contains(spec);
    }

    boolean isExcluded(FeatureId featureId) {
        if (excludedFeatures.contains(featureId)) {
            return true;
        }
        if (excludedSpecs.contains(featureId.specId)) {
            return !includedFeatures.containsKey(featureId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((excludedFeatures == null) ? 0 : excludedFeatures.hashCode());
        result = prime * result + ((excludedSpecs == null) ? 0 : excludedSpecs.hashCode());
        result = prime * result + ((includedFeatures == null) ? 0 : includedFeatures.hashCode());
        result = prime * result + ((includedSpecs == null) ? 0 : includedSpecs.hashCode());
        result = prime * result + (inheritFeatures ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        FeatureGroupConfigSupport other = (FeatureGroupConfigSupport) obj;
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
