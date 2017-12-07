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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.PackageDepsSpecBuilder;
import org.jboss.provisioning.spec.SpecId;
import org.jboss.provisioning.util.PmCollections;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class FeatureGroupBuilderSupport<T extends FeatureGroupSupport, B extends FeatureGroupBuilderSupport<T, B>>
    extends PackageDepsSpecBuilder<B>
    implements ConfigItemContainerBuilder<B> {

    protected String fpDep;
    protected String name;

    // dependency customizations
    protected boolean inheritFeatures = true;
    protected Set<SpecId> includedSpecs = Collections.emptySet();
    protected Map<FeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
    protected Set<SpecId> excludedSpecs = Collections.emptySet();
    protected Map<FeatureId, String> excludedFeatures = Collections.emptyMap();
    protected Map<String, FeatureGroup.Builder> externalFgConfigs = Collections.emptyMap();

    // added items
    protected List<ConfigItem> items = Collections.emptyList();

    protected FeatureGroupBuilderSupport() {
    }

    protected FeatureGroupBuilderSupport(String name) {
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

    public B setProperty(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public B setInheritFeatures(boolean inheritFeatures) {
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
        includedSpecs = PmCollections.addLinked(includedSpecs, specId);
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
        if(excludedFeatures.containsKey(featureId)) {
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
        includedFeatures = PmCollections.putLinked(includedFeatures, featureId, feature);
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
        excludedSpecs = PmCollections.add(excludedSpecs, specId);
        return (B) this;
    }

    public B excludeFeature(String fpDep, FeatureId featureId) throws ProvisioningDescriptionException {
        return excludeFeature(fpDep, featureId, null);
    }

    @SuppressWarnings("unchecked")
    public B excludeFeature(String fpDep, FeatureId featureId, String parentRef) throws ProvisioningDescriptionException {
        if(fpDep == null) {
            return excludeFeature(featureId, parentRef);
        }
        getExternalFgConfig(fpDep).excludeFeature(featureId, parentRef);
        return (B) this;
    }

    public B excludeFeature(FeatureId featureId) throws ProvisioningDescriptionException {
        return excludeFeature(featureId, null);
    }

    @SuppressWarnings("unchecked")
    public B excludeFeature(FeatureId featureId, String parentRef) throws ProvisioningDescriptionException {
        if(includedFeatures.containsKey(featureId)) {
            throw new ProvisioningDescriptionException(featureId + " has been explicitly included");
        }
        excludedFeatures = PmCollections.put(excludedFeatures, featureId, parentRef);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B addConfigItem(ConfigItem item) {
        items = PmCollections.add(items, item);
        return (B) this;
    }

    public abstract T build() throws ProvisioningDescriptionException;

    private FeatureGroup.Builder getExternalFgConfig(String fpDep) {
        if(fpDep == null) {
            throw new IllegalArgumentException();
        }
        FeatureGroup.Builder fgBuilder = externalFgConfigs.get(fpDep);
        if(fgBuilder != null) {
            return fgBuilder;
        }
        fgBuilder = FeatureGroup.builder(inheritFeatures);
        externalFgConfigs = PmCollections.putLinked(externalFgConfigs, fpDep, fgBuilder);
        return fgBuilder;
    }
}
