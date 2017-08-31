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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class FeatureGroupConfigBuilderSupport<T extends FeatureGroupConfigSupport, B extends FeatureGroupConfigBuilderSupport<T, B>> {

    String name;
    boolean inheritFeatures = true;
    Set<SpecId> includedSpecs = Collections.emptySet();
    Map<FeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
    Set<SpecId> excludedSpecs = Collections.emptySet();
    Set<FeatureId> excludedFeatures = Collections.emptySet();

    protected FeatureGroupConfigBuilderSupport() {
    }

    protected FeatureGroupConfigBuilderSupport(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public B setName(String name) {
        this.name = name;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B inheritFeatures(boolean inheritFeatures) {
        this.inheritFeatures = inheritFeatures;
        return (B) this;
    }

    public B includeSpec(String spec) throws ProvisioningDescriptionException {
        return includeSpec(SpecId.fromString(spec));
    }

    @SuppressWarnings("unchecked")
    public B includeSpec(SpecId spec) throws ProvisioningDescriptionException {
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
        return (B) this;
    }

    public B includeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
        return includeFeature(featureId, null);
    }

    @SuppressWarnings("unchecked")
    public B includeFeature(FeatureId featureId, FeatureConfig feature) throws ProvisioningDescriptionException {
        if(excludedFeatures.contains(featureId)) {
            throw new ProvisioningDescriptionException(featureId + " has been explicitly excluded");
        }
        if(feature == null) {
            feature = new FeatureConfig(featureId.specId);
        }
        if(feature.specId == null) {
            feature.specId = featureId.specId;
        }
        for (Map.Entry<String, String> idEntry : featureId.params.entrySet()) {
            final String prevValue = feature.putParam(idEntry.getKey(), idEntry.getValue());
            if (prevValue != null && !prevValue.equals(idEntry.getValue())) {
                throw new ProvisioningDescriptionException("Parameter " + idEntry.getKey() + " has value '"
                        + idEntry.getValue() + "' in feature ID and value '" + prevValue + "' in the feature body");
            }
        }
        switch(includedFeatures.size()) {
            case 0:
                includedFeatures = Collections.singletonMap(featureId, feature);
                break;
            case 1:
                final Map.Entry<FeatureId, FeatureConfig> entry = includedFeatures.entrySet().iterator().next();
                includedFeatures = new LinkedHashMap<>(2);
                includedFeatures.put(entry.getKey(), entry.getValue());
            default:
                includedFeatures.put(featureId, feature);
        }
        return (B) this;
    }

    public B excludeSpec(String spec) throws ProvisioningDescriptionException {
        return excludeSpec(SpecId.fromString(spec));
    }

    @SuppressWarnings("unchecked")
    public B excludeSpec(SpecId spec) throws ProvisioningDescriptionException {
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
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B excludeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
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
        return (B) this;
    }

    public abstract T build() throws ProvisioningDescriptionException;
}
