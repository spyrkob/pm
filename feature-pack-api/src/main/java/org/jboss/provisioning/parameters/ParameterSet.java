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

package org.jboss.provisioning.parameters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class ParameterSet {

    public static class Builder implements BuilderWithParameters<Builder> {

        private String name;
        private Map<String, PackageParameter> params = Collections.emptyMap();

        private Builder() {
        }

        private Builder(String name) {
            this.name = name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder addParameter(PackageParameter param) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(param.getName(), param);
                    break;
                case 1:
                    if(params.containsKey(param.getName())) {
                        params = Collections.singletonMap(param.getName(), param);
                        break;
                    }
                    params = new HashMap<>(params);
                default:
                    params.put(param.getName(), param);
            }
            return this;
        }

        public ParameterSet build() {
            return new ParameterSet(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static ParameterSet forName(String name) {
        return new ParameterSet(name);
    }

    private final String name;
    private final Map<String, PackageParameter> params;

    private ParameterSet(String name) {
        this.name = name;
        params = Collections.emptyMap();
    }

    private ParameterSet(Builder builder) {
        this.name = builder.name;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
    }

    public String getName() {
        return name;
    }

    public boolean hasParameters() {
        return !params.isEmpty();
    }

    public Collection<PackageParameter> getParameters() {
        return params.values();
    }

    public boolean containsAll(Set<String> paramNames) {
        for(String name : paramNames) {
            if(!params.containsKey(name)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasParameter(String name) {
        return params.containsKey(name);
    }

    public PackageParameter getParameter(String name) {
        return params.get(name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
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
        ParameterSet other = (ParameterSet) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(name).append('{');
        if(!params.isEmpty()) {
            final Iterator<PackageParameter> i = params.values().iterator();
            PackageParameter param = i.next();
            buf.append(param.getName()).append('=').append(param.getValue());
            while(i.hasNext()) {
                buf.append(',');
                param = i.next();
                buf.append(param.getName()).append('=').append(param.getValue());            }
        }
        return buf.append('}').toString();
    }
}
