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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpecRegistry {

    public static class Builder {

        Map<String, FeatureSpec> featureSpecs = new HashMap<>();
        private boolean checkRefs;

        private Builder() {
        }

        public Builder addFeatureSpec(FeatureSpec spec) throws ProvisioningDescriptionException {
            if(featureSpecs.containsKey(spec.name)) {
                throw new ProvisioningDescriptionException("Duplicate feature spec " + spec.name);
            }
            featureSpecs.put(spec.name, spec);
            if(!checkRefs) {
                checkRefs = spec.hasRefs();
            }
            return this;
        }

        public FeatureSpecRegistry build() throws ProvisioningDescriptionException {
            if(checkRefs) {
                for(FeatureSpec spec : featureSpecs.values()) {
                    if(!spec.hasRefs()) {
                        continue;
                    }
                    for(FeatureReferenceSpec refSpec : spec.refs.values()) {
                        final FeatureSpec targetSpec = featureSpecs.get(refSpec.feature);
                        if(targetSpec == null) {
                            throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                    + refSpec.name + " which targets unknown " + refSpec.feature + " feature");
                        }
                        if(!targetSpec.hasId()) {
                            throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                    + refSpec.name + " which targets feature " + refSpec.feature + " that has no ID parameters");
                        }
                        if(targetSpec.idParams.size() != refSpec.paramMapping.size()) {
                            throw new ProvisioningDescriptionException("Parameters of reference " + refSpec.name + " of feature " + spec.name +
                                    " must correspond to the ID parameters of the target feature " + refSpec.feature);
                        }
                        for(Map.Entry<String, String> mapping : refSpec.paramMapping.entrySet()) {
                            if(!spec.params.containsKey(mapping.getKey())) {
                                throw new ProvisioningDescriptionException(spec.name
                                        + " feature does not include parameter " + mapping.getKey() + " mapped in "
                                        + refSpec.name + " reference");
                            }
                            if(!targetSpec.params.containsKey(mapping.getValue())) {
                                throw new ProvisioningDescriptionException(targetSpec.name
                                        + " feature does not include parameter '" + mapping.getValue() + "' targeted from "
                                        + spec.name + " through " + refSpec.name + " reference");
                            }
                        }
                    }
                }
            }
            return new FeatureSpecRegistry(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    final Map<String, FeatureSpec> featureSpecs;

    private FeatureSpecRegistry(Builder builder) {
        this.featureSpecs = builder.featureSpecs.size() > 1 ? Collections.unmodifiableMap(builder.featureSpecs) : builder.featureSpecs;
    }

    public FeatureSpec getFeatureSpec(String spec) throws ProvisioningDescriptionException {
        final FeatureSpec featureSpec = featureSpecs.get(spec);
        if(featureSpec == null) {
            throw new ProvisioningDescriptionException("Unknown feature spec " + spec);
        }
        return featureSpec;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureSpecs == null) ? 0 : featureSpecs.hashCode());
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
        FeatureSpecRegistry other = (FeatureSpecRegistry) obj;
        if (featureSpecs == null) {
            if (other.featureSpecs != null)
                return false;
        } else if (!featureSpecs.equals(other.featureSpecs))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        final Iterator<FeatureSpec> i = featureSpecs.values().iterator();
        buf.append(i.next());
        while(i.hasNext()) {
            buf.append(',').append(i.next());
        }
        return buf.append(']').toString();
    }
}
