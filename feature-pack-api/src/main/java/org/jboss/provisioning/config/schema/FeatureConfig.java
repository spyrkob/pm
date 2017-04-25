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

package org.jboss.provisioning.config.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfig {

    public static class Dependency {
        final ConfigRef ref;
        final boolean optional;

        private Dependency(ConfigRef ref, boolean optional) {
            this.ref = ref;
            this.optional = optional;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (optional ? 1231 : 1237);
            result = prime * result + ((ref == null) ? 0 : ref.hashCode());
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
            Dependency other = (Dependency) obj;
            if (optional != other.optional)
                return false;
            if (ref == null) {
                if (other.ref != null)
                    return false;
            } else if (!ref.equals(other.ref))
                return false;
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append('[');
            if(optional) {
                buf.append("optional ");
            }
            return buf.append(ref).append(']').toString();
        }
    }

    public static class Builder {

        private final String spot;
        private Map<String, String> params = Collections.emptyMap();
        private Set<Dependency> dependencies = Collections.emptySet();

        private Builder(String spot) {
            this.spot = spot;
        }

        public Builder addParameter(String name, String value) throws ProvisioningDescriptionException {
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

        public Builder addDependency(ConfigRef ref) throws ProvisioningDescriptionException {
            return addDependency(ref, false);
        }

        public Builder addDependency(ConfigRef ref, boolean optional) throws ProvisioningDescriptionException {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singleton(new Dependency(ref, optional));
                    break;
                case 1:
                    dependencies = new LinkedHashSet<>(dependencies);
                default:
                    dependencies.add(new Dependency(ref, optional));
            }
            return this;
        }

        public FeatureConfig build() throws ProvisioningDescriptionException {
            return new FeatureConfig(this);
        }
    }

    public static Builder builder(String spot) {
        return new Builder(spot);
    }

    public static FeatureConfig forName(String spot) {
        return new FeatureConfig(spot);
    }

    final String spot;
    final Map<String, String> params;
    final Set<Dependency> dependencies;

    private FeatureConfig(String spot) {
        this.spot = spot;
        this.params = Collections.emptyMap();
        this.dependencies = Collections.emptySet();
    }

    private FeatureConfig(Builder builder) {
        this.spot = builder.spot;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableSet(builder.dependencies) : builder.dependencies;
    }

    public String getSpot() {
        return spot;
    }

    public Collection<String> getParameterNames() {
        return params.keySet();
    }

    public String getParameterValue(String name) {
        return params.get(name);
    }

    public String getParameterValue(String name, boolean required) throws ProvisioningDescriptionException {
        final String value = params.get(name);
        if(value == null && required) {
            throw new ProvisioningDescriptionException(spot + " configuration is missing required parameter '" + name + "'");
        }
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((spot == null) ? 0 : spot.hashCode());
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
        FeatureConfig other = (FeatureConfig) obj;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (spot == null) {
            if (other.spot != null)
                return false;
        } else if (!spot.equals(other.spot))
            return false;
        return true;
    }
}
