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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureReferenceSpec {

    private static final String NAME = "name";

    public static class Builder {

        private final SpecId feature;
        private String name;
        private boolean nillable;
        private Map<String, String> paramMapping = null;

        private Builder(SpecId feature) {
            this.feature = feature;
            this.name = feature.toString();
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNillable(boolean nillable) {
            this.nillable = nillable;
            return this;
        }

        public Builder mapParam(String localName, String targetName) {
            if(paramMapping == null) {
                paramMapping = Collections.singletonMap(localName, targetName);
            } else {
                if(paramMapping.size() == 1) {
                    paramMapping = new HashMap<>(paramMapping);
                }
                paramMapping.put(localName, targetName);
            }
            return this;
        }

        public FeatureReferenceSpec build() throws ProvisioningDescriptionException {
            if(paramMapping == null) {
                paramMapping = Collections.singletonMap(feature.getName(), NAME);
            }
            return new FeatureReferenceSpec(name, feature, nillable, paramMapping);
        }
    }

    public static Builder builder(String feature) throws ProvisioningDescriptionException {
        return builder(SpecId.fromString(feature));
    }

    public static Builder builder(SpecId feature) {
        return new Builder(feature);
    }

    public static FeatureReferenceSpec create(SpecId feature) throws ProvisioningDescriptionException {
        return create(feature.toString(), feature, false);
    }

    public static FeatureReferenceSpec create(SpecId feature, boolean nillable) throws ProvisioningDescriptionException {
        return create(feature.toString(), feature, nillable);
    }

    public static FeatureReferenceSpec create(String name, SpecId feature) throws ProvisioningDescriptionException {
        return create(name, feature, false);
    }

    public static FeatureReferenceSpec create(String name, String feature, boolean nillable) throws ProvisioningDescriptionException {
        return create(name, SpecId.fromString(feature), nillable);
    }

    public static FeatureReferenceSpec create(String name, SpecId feature, boolean nillable) throws ProvisioningDescriptionException {
        return new FeatureReferenceSpec(name, feature, nillable, Collections.singletonMap(feature.getName(), NAME));
    }

    final String name;
    final SpecId feature;
    final boolean nillable;
    final String[] localParams;
    final String[] targetParams;

    private FeatureReferenceSpec(String name, SpecId feature, boolean nillable, Map<String, String> paramMapping) throws ProvisioningDescriptionException {
        this.name = name;
        this.feature = feature;
        this.nillable = nillable;
        if(paramMapping.isEmpty()) {
            throw new ProvisioningDescriptionException("Reference " + name + " is missing parameter mapping.");
        }
        this.localParams = new String[paramMapping.size()];
        this.targetParams = new String[paramMapping.size()];
        int i = 0;
        for(Map.Entry<String, String> mapping : paramMapping.entrySet()) {
            localParams[i] = mapping.getKey();
            targetParams[i++] = mapping.getValue();
        }

    }

    public String getName() {
        return name;
    }

    public SpecId getFeature() {
        return feature;
    }

    public boolean isNillable() {
        return nillable;
    }

    public int getParamsMapped() {
        return localParams.length;
    }

    public String getLocalParam(int i) {
        return localParams[i];
    }

    public String getTargetParam(int i) {
        return targetParams[i];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + Arrays.hashCode(localParams);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(targetParams);
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
        FeatureReferenceSpec other = (FeatureReferenceSpec) obj;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;
        if (!Arrays.equals(localParams, other.localParams))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nillable != other.nillable)
            return false;
        if (!Arrays.equals(targetParams, other.targetParams))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(name);
        buf.append(" feature=").append(feature);
        if(nillable) {
            buf.append(" nillable");
        }
        buf.append(' ');
        buf.append(localParams[0]).append('=').append(targetParams[0]);
        for(int i = 1; i < localParams.length; ++i) {
            buf.append(',').append(localParams[i]).append('=').append(targetParams[i]);
        }
        return buf.append(']').toString();
    }
}
