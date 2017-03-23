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

package org.jboss.provisioning.state;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.provisioning.parameters.BuilderWithParameters;
import org.jboss.provisioning.parameters.PackageParameter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedPackage {

    public static class Builder implements BuilderWithParameters<Builder> {

        private final String name;
        private Map<String, PackageParameter> params = Collections.emptyMap();

        private Builder(String name) {
            this.name = name;
        }

        @Override
        public Builder addParameter(PackageParameter param) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(param.getName(), param);
                    break;
                case 1:
                    params = new HashMap<>(params);
                default:
                    params.put(param.getName(), param);
            }
            return this;
        }

        public ProvisionedPackage build() {
            return new ProvisionedPackage(this);
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static ProvisionedPackage newInstance(String name) {
        return new ProvisionedPackage(name);
    }

    private final String name;
    private final Map<String, PackageParameter> params;

    public ProvisionedPackage(Builder builder) {
        this.name = builder.name;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
    }

    public ProvisionedPackage(String name) {
        this.name = name;
        this.params = Collections.emptyMap();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        ProvisionedPackage other = (ProvisionedPackage) obj;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder().append(name);
        if(!params.isEmpty()) {
            buf.append('(');
            final Iterator<PackageParameter> i = params.values().iterator();
            PackageParameter param = i.next();
            buf.append(param.getName()).append('=').append(param.getValue());
            while(i.hasNext()) {
                param = i.next();
                buf.append(',').append(param.getName()).append('=').append(param.getValue());
            }
            buf.append(')');
        }
        return buf.toString();
    }
}
