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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeatureGroupConfig.Builder;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.SpecId;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class FeatureGroupConfigBuilderSupport<T extends FeatureGroupConfigSupport, B extends FeatureGroupConfigBuilderSupport<T, B>> {

    String fpDep;
    String name;
    boolean inheritFeatures = true;
    Set<SpecId> includedSpecs = Collections.emptySet();
    Map<FeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
    Set<SpecId> excludedSpecs = Collections.emptySet();
    Set<FeatureId> excludedFeatures = Collections.emptySet();
    Map<String, FeatureGroupConfig.Builder> externalFgConfigs = Collections.emptyMap();

    protected FeatureGroupConfigBuilderSupport() {
    }

    protected FeatureGroupConfigBuilderSupport(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public B setFpDep(String fpDep) {
        this.fpDep = fpDep;
        return (B) this;
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

    @SuppressWarnings("unchecked")
    public B includeSpec(String fpDep, String spec) throws ProvisioningDescriptionException {
        if(fpDep == null) {
            return includeSpec(spec);
        }
        getExternalFgConfig(fpDep).includeSpec(spec);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B includeSpec(String spec) throws ProvisioningDescriptionException {
        final SpecId specId = SpecId.fromString(spec);
        if(excludedSpecs.contains(specId)) {
            throw new ProvisioningDescriptionException(specId + " spec has been explicitly excluded");
        }
        switch(includedSpecs.size()) {
            case 0:
                includedSpecs = Collections.singleton(specId);
                break;
            case 1:
                includedSpecs = new LinkedHashSet<>(includedSpecs);
            default:
                includedSpecs.add(specId);
        }
        return (B) this;
    }

    public B includeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
        return includeFeature(featureId, null);
    }

    public B includeFeature(String fpDep, FeatureId featureId) throws ProvisioningDescriptionException {
        return includeFeature(fpDep, featureId, null);
    }

    @SuppressWarnings("unchecked")
    public B includeFeature(String fpDep, FeatureId featureId, FeatureConfig feature) throws ProvisioningDescriptionException {
        if(fpDep == null) {
            return includeFeature(featureId, feature);
        }
        getExternalFgConfig(fpDep).includeFeature(featureId, feature);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B includeFeature(FeatureId featureId, FeatureConfig feature) throws ProvisioningDescriptionException {
        if(excludedFeatures.contains(featureId)) {
            throw new ProvisioningDescriptionException(featureId + " has been explicitly excluded");
        }
        if(feature == null) {
            feature = new FeatureConfig(featureId.getSpec());
        }
        if(feature.specId == null) {
            feature.specId = featureId.getSpec();
        }
        for (Map.Entry<String, String> idEntry : featureId.getParams().entrySet()) {
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

    @SuppressWarnings("unchecked")
    public B excludeSpec(String fpDep, String spec) throws ProvisioningDescriptionException {
        if(fpDep == null) {
            return excludeSpec(spec);
        }
        getExternalFgConfig(fpDep).excludeSpec(spec);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B excludeSpec(String spec) throws ProvisioningDescriptionException {
        final SpecId specId = SpecId.fromString(spec);
        if(includedSpecs.contains(specId)) {
            throw new ProvisioningDescriptionException(specId + " spec has been inplicitly excluded");
        }
        switch(excludedSpecs.size()) {
            case 0:
                excludedSpecs = Collections.singleton(specId);
                break;
            case 1:
                excludedSpecs = new HashSet<>(excludedSpecs);
            default:
                excludedSpecs.add(specId);
        }
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B excludeFeature(String fpDep, FeatureId featureId) throws ProvisioningDescriptionException {
        if(fpDep == null) {
            return excludeFeature(featureId);
        }
        getExternalFgConfig(fpDep).excludeFeature(featureId);
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
                final FeatureId first = excludedFeatures.iterator().next();
                excludedFeatures = new HashSet<>(2);
                excludedFeatures.add(first);
            default:
                excludedFeatures.add(featureId);
        }
        return (B) this;
    }

    public abstract T build() throws ProvisioningDescriptionException;

    private FeatureGroupConfig.Builder getExternalFgConfig(String fpDep) {
        if(fpDep == null) {
            throw new IllegalArgumentException();
        }
        FeatureGroupConfig.Builder fgBuilder = externalFgConfigs.get(fpDep);
        if(fgBuilder != null) {
            return fgBuilder;
        }
        fgBuilder = FeatureGroupConfig.builder(inheritFeatures);
        if(externalFgConfigs.isEmpty()) {
            externalFgConfigs = Collections.singletonMap(fpDep, fgBuilder);
            return fgBuilder;
        }
        if(externalFgConfigs.size() == 1) {
            final Map.Entry<String, Builder> first = externalFgConfigs.entrySet().iterator().next();
            externalFgConfigs = new LinkedHashMap<>(2);
            externalFgConfigs.put(first.getKey(), first.getValue());
        }
        externalFgConfigs.put(fpDep, fgBuilder);
        return fgBuilder;
    }
}
