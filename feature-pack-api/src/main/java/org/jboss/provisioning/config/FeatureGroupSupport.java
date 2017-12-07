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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.PackageDepsSpec;
import org.jboss.provisioning.spec.SpecId;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeatureGroupSupport extends PackageDepsSpec implements ConfigItem, ConfigItemContainer {

    final String fpDep;
    final String name;

    // customizations of the dependencies
    final boolean inheritFeatures;
    final Set<SpecId> includedSpecs;
    final Map<FeatureId, FeatureConfig> includedFeatures;
    final Set<SpecId> excludedSpecs;
    final Map<FeatureId, String> excludedFeatures; // featureId and optional parent-ref
    final Map<String, FeatureGroupSupport> externalFgConfigs;

    // added items
    protected final List<ConfigItem> items;

    protected FeatureGroupSupport(String fpDep, String name) {
        super();
        this.fpDep = fpDep;
        this.name = name;
        this.inheritFeatures = true;
        this.includedSpecs = Collections.emptySet();
        this.includedFeatures = Collections.emptyMap();
        this.excludedSpecs = Collections.emptySet();
        this.excludedFeatures = Collections.emptyMap();
        this.externalFgConfigs = Collections.emptyMap();
        this.items = Collections.emptyList();
    }

    protected FeatureGroupSupport(FeatureGroupBuilderSupport<?, ?> builder) throws ProvisioningDescriptionException {
        super(builder);
        this.fpDep = builder.fpDep;
        this.name = builder.name;
        this.inheritFeatures = builder.inheritFeatures;
        this.includedSpecs = PmCollections.unmodifiable(builder.includedSpecs);
        this.excludedSpecs = PmCollections.unmodifiable(builder.excludedSpecs);
        this.includedFeatures = PmCollections.unmodifiable(builder.includedFeatures);
        this.excludedFeatures = PmCollections.unmodifiable(builder.excludedFeatures);

        if(builder.externalFgConfigs.isEmpty()) {
            this.externalFgConfigs = Collections.emptyMap();
        } else if(builder.externalFgConfigs.size() == 1) {
            final Map.Entry<String, FeatureGroup.Builder> entry = builder.externalFgConfigs.entrySet().iterator().next();
            this.externalFgConfigs = Collections.singletonMap(entry.getKey(), entry.getValue().build());
        } else {
            final Map<String, FeatureGroup> tmp = new LinkedHashMap<>(builder.externalFgConfigs.size());
            for(Map.Entry<String, FeatureGroup.Builder> entry : builder.externalFgConfigs.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue().build());
            }
            this.externalFgConfigs = Collections.unmodifiableMap(tmp);
        }

        this.items = PmCollections.unmodifiable(builder.items);
    }

    @Override
    public String getFpDep() {
        return fpDep;
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public String getName() {
        return name;
    }

    public boolean hasProperties() {
        return false;
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
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

    public Map<FeatureId, String> getExcludedFeatures() {
        return excludedFeatures;
    }

    public boolean hasIncludedFeatures() {
        return !includedFeatures.isEmpty();
    }

    public Map<FeatureId, FeatureConfig> getIncludedFeatures() {
        return includedFeatures;
    }

    public boolean hasExternalFeatureGroups() {
        return !externalFgConfigs.isEmpty();
    }

    public Map<String, FeatureGroupSupport> getExternalFeatureGroups() {
        return externalFgConfigs;
    }

    @Override
    public boolean hasItems() {
        return !items.isEmpty();
    }

    @Override
    public List<ConfigItem> getItems() {
        return items;
    }

    @Override
    public boolean isResetFeaturePackOrigin() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((excludedFeatures == null) ? 0 : excludedFeatures.hashCode());
        result = prime * result + ((excludedSpecs == null) ? 0 : excludedSpecs.hashCode());
        result = prime * result + ((externalFgConfigs == null) ? 0 : externalFgConfigs.hashCode());
        result = prime * result + ((fpDep == null) ? 0 : fpDep.hashCode());
        result = prime * result + ((includedFeatures == null) ? 0 : includedFeatures.hashCode());
        result = prime * result + ((includedSpecs == null) ? 0 : includedSpecs.hashCode());
        result = prime * result + (inheritFeatures ? 1231 : 1237);
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        FeatureGroupSupport other = (FeatureGroupSupport) obj;
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
        if (externalFgConfigs == null) {
            if (other.externalFgConfigs != null)
                return false;
        } else if (!externalFgConfigs.equals(other.externalFgConfigs))
            return false;
        if (fpDep == null) {
            if (other.fpDep != null)
                return false;
        } else if (!fpDep.equals(other.fpDep))
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
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
