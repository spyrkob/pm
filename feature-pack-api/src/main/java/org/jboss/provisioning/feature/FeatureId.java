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
public class FeatureId {

    public static class Builder {

        private final String specName;
        private Map<String, String> params = Collections.emptyMap();

        private Builder(String specName) {
            this.specName = specName;
        }

        public Builder addParam(String name, String value) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(name, value);
                    break;
                case 1:
                    params = new HashMap<>(params);
                default:
                    params.put(name, value);
            }
            return this;
        }

        public FeatureId build() throws ProvisioningDescriptionException {
            return new FeatureId(specName, params);
        }
    }

    public static Builder builder(String specName) {
        return new Builder(specName);
    }

    public static FeatureId create(String specName, String name, String value) throws ProvisioningDescriptionException {
        return new FeatureId(specName, Collections.singletonMap(name, value));
    }

    final String specName;
    final Map<String, String> params;

    public FeatureId(String specName, Map<String, String> params) throws ProvisioningDescriptionException {
        if(params.isEmpty()) {
            throw new ProvisioningDescriptionException("ID paramaters are missing");
        }
        this.specName = specName;
        this.params = params.size() == 1 ? params : Collections.unmodifiableMap(params);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((specName == null) ? 0 : specName.hashCode());
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
        FeatureId other = (FeatureId) obj;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (specName == null) {
            if (other.specName != null)
                return false;
        } else if (!specName.equals(other.specName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(specName);
        if (!params.isEmpty()) {
            buf.append(' ');
            final Iterator<Map.Entry<String, String>> i = params.entrySet().iterator();
            Map.Entry<String, String> entry = i.next();
            buf.append(entry.getKey()).append('=').append(entry.getValue());
            while(i.hasNext()) {
                entry = i.next();
                buf.append(',').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        return buf.append(']').toString();
    }
}
