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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.parameters.BuilderWithParameterSets;
import org.jboss.provisioning.parameters.BuilderWithParameters;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;

/**
 * Package configuration.
 *
 * @author Alexey Loubyansky
 */
public class PackageConfig {

    public static class Builder implements BuilderWithParameters<Builder>, BuilderWithParameterSets<Builder> {

        private String name;
        private Map<String, PackageParameter> params = Collections.emptyMap();
        private Map<String, ParameterSet> configs = Collections.emptyMap();

        protected Builder(String name) {
            this.name = name;
        }

        protected Builder(PackageConfig config) {
            this.name = config.name;
            params = config.params.size() > 1 ? new HashMap<>(config.params) : config.params;
            configs = config.configs.size() > 1 ? new HashMap<>(config.configs) : config.configs;
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

        public PackageConfig build() {
            return new PackageConfig(this);
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(PackageConfig config) {
        return new Builder(config);
    }

    public static PackageConfig newInstance(String name) {
        return new PackageConfig(name);
    }

    private final String name;
    private final Map<String, PackageParameter> params;
    private final Map<String, ParameterSet> configs;

    private PackageConfig(String name) {
        this.name = name;
        params = Collections.emptyMap();
        configs = Collections.emptyMap();
    }

    private PackageConfig(Builder builder) {
        this.name = builder.name;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.configs = builder.configs.size() > 1 ? Collections.unmodifiableMap(builder.configs) : builder.configs;
    }

    public String getName() {
        return name;
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

    public ParameterSet getConfig(String name) {
        return configs.get(name);
    }

    public Collection<ParameterSet> getConfigs() {
        return configs.values();
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
        PackageConfig other = (PackageConfig) obj;
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
}
