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

package org.jboss.provisioning.spec;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.parameters.BuilderWithParameterSets;
import org.jboss.provisioning.parameters.BuilderWithParameters;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;

/**
 * Describes dependency on a single package.
 *
 * @author Alexey Loubyansky
 */
public class PackageDependencySpec implements Comparable<PackageDependencySpec> {

    public static class Builder implements BuilderWithParameters<Builder>, BuilderWithParameterSets<Builder> {

        private final String name;
        private final boolean optional;
        private Map<String, PackageParameter> params = Collections.emptyMap();
        private Map<String, ParameterSet> configs = Collections.emptyMap();

        protected Builder(String name) {
            this(name, false);
        }

        protected Builder(String name, boolean optional) {
            this.name = name;
            this.optional = optional;
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

        @Override
        public Builder addConfig(ParameterSet config) {
            switch(configs.size()) {
                case 0:
                    configs = Collections.singletonMap(config.getName(), config);
                    break;
                case 1:
                    if(configs.containsKey(config.getName())) {
                        configs = Collections.singletonMap(config.getName(),config);
                        break;
                    }
                    configs = new HashMap<>(configs);
                default:
                    configs.put(config.getName(), config);

            }
            return this;
        }

        public PackageDependencySpec build() {
            return new PackageDependencySpec(this);
        }
    }

    public static Builder builder(String packageName) {
        return new Builder(packageName);
    }

    public static Builder builder(String packageName, boolean optional) {
        return new Builder(packageName, optional);
    }

    /**
     * Creates a required dependency on the provided package name.
     *
     * @param name  target package name
     * @return  dependency description
     */
    public static PackageDependencySpec create(String name) {
        return new PackageDependencySpec(name, false);
    }

    /**
     * Creates a dependency on the provided package name.
     *
     * @param name  target package name
     * @param optional  whether the dependency should be optional
     * @return  dependency description
     */
    public static PackageDependencySpec create(String name, boolean optional) {
        return new PackageDependencySpec(name, optional);
    }

    private final String name;
    private final boolean optional;
    private final Map<String, PackageParameter> params;
    private final Map<String, ParameterSet> configs;

    protected PackageDependencySpec(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
        params = Collections.emptyMap();
        configs = Collections.emptyMap();
    }

    protected PackageDependencySpec(Builder builder) {
        this.name = builder.name;
        this.optional = builder.optional;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.configs = builder.configs.size() > 1 ? Collections.unmodifiableMap(builder.configs) : builder.configs;
    }

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean hasParams() {
        return !params.isEmpty();
    }

    public PackageParameter getParameter(String name) {
        return params.get(name);
    }

    public Collection<PackageParameter> getParameters() {
        return params.values();
    }

    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    public Collection<ParameterSet> getConfigs() {
        return configs.values();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configs == null) ? 0 : configs.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (optional ? 1231 : 1237);
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
        PackageDependencySpec other = (PackageDependencySpec) obj;
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
        if (optional != other.optional)
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
        buf.append('[')
            .append(name)
            .append(optional ? " optional]" : " required");
        if(hasParams()) {
            buf.append(" params=").append(params);
        }
        return buf.append(']').toString();
    }

    @Override
    public int compareTo(PackageDependencySpec o) {
        return name.compareTo(o.name);
    }
}
