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
import java.util.Iterator;
import java.util.Map;

import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;

/**
 *
 * @author Alexey Loubyansky
 */
class ProvisionedPackageImpl implements ProvisionedPackage {

    private final String name;
    private final Map<String, PackageParameter> params;
    private final Map<String, ParameterSet> configs;

    ProvisionedPackageImpl(String name, Map<String, PackageParameter> params, Map<String, ParameterSet> configs) {
        this.name = name;
        this.params = params;
        this.configs = configs;
    }

    ProvisionedPackageImpl(String name) {
        this.name = name;
        this.params = Collections.emptyMap();
        this.configs = Collections.emptyMap();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasParameters() {
        return !params.isEmpty();
    }

    @Override
    public Collection<PackageParameter> getParameters() {
        return params.values();
    }

    @Override
    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    @Override
    public Collection<ParameterSet> getConfigs() {
        return configs.values();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configs == null) ? 0 : configs.hashCode());
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
        ProvisionedPackageImpl other = (ProvisionedPackageImpl) obj;
        if (configs == null) {
            if (other.configs != null)
                return false;
        } else if (!configs.equals(other.configs))
            return false;
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
        if(!configs.isEmpty()) {
            buf.append('[');
            final Iterator<ParameterSet> i = configs.values().iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
            buf.append(']');
        }
        return buf.toString();
    }
}
