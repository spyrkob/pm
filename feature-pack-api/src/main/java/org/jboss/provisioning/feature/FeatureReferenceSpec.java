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
import java.util.Map.Entry;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureReferenceSpec {

    public static class Builder {

        private final String feature;
        private String name;
        private boolean nillable;
        private Map<String, String> paramMapping = null;

        private Builder(String feature) {
            this.feature = feature;
            this.name = feature;
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
                paramMapping = Collections.singletonMap(feature, "name");
            }
            return new FeatureReferenceSpec(name, feature, nillable, paramMapping);
        }
    }

    public static Builder builder(String feature) {
        return new Builder(feature);
    }

    public static FeatureReferenceSpec create(String feature) throws ProvisioningDescriptionException {
        return create(feature, feature, false);
    }

    public static FeatureReferenceSpec create(String feature, boolean nillable) throws ProvisioningDescriptionException {
        return create(feature, feature, nillable);
    }

    public static FeatureReferenceSpec create(String name, String feature) throws ProvisioningDescriptionException {
        return create(name, feature, false);
    }

    public static FeatureReferenceSpec create(String name, String feature, boolean nillable) throws ProvisioningDescriptionException {
        return new FeatureReferenceSpec(name, feature, nillable, Collections.singletonMap(feature, "name"));
    }

    final String name;
    final String feature;
    final boolean nillable;
    final Map<String, String> paramMapping;

    private FeatureReferenceSpec(String name, String feature, boolean nillable, Map<String, String> paramMapping) throws ProvisioningDescriptionException {
        this.name = name;
        this.feature = feature;
        this.nillable = nillable;
        if(paramMapping.isEmpty()) {
            throw new ProvisioningDescriptionException("Reference " + name + " is missing parameter mapping.");
        }
        this.paramMapping = paramMapping.size() == 1 ? paramMapping : Collections.unmodifiableMap(paramMapping);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
        result = prime * result + ((paramMapping == null) ? 0 : paramMapping.hashCode());
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nillable != other.nillable)
            return false;
        if (paramMapping == null) {
            if (other.paramMapping != null)
                return false;
        } else if (!paramMapping.equals(other.paramMapping))
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
        final Iterator<Map.Entry<String, String>> i = paramMapping.entrySet().iterator();
        Entry<String, String> mapping = i.next();
        buf.append(mapping.getKey()).append('=').append(mapping.getValue());
        while(i.hasNext()) {
            mapping = i.next();
            buf.append(',').append(mapping.getKey()).append('=').append(mapping.getValue());
        }
        return buf.append(']').toString();
    }
}
