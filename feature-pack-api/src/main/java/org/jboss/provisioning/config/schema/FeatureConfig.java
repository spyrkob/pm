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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfig {

    public interface Dependency {

        ConfigId getConfigId() throws ProvisioningDescriptionException;

        boolean isOptional();
    }

    private class ParameterizedDependency implements Dependency {

        private final ConfigPath path;
        private final String[] values;
        private final boolean optional;
        private final ConfigId configId;


        private ParameterizedDependency(String path, boolean optional) {

            if(path == null) {
                throw new IllegalArgumentException("str is null");
            }
            if(path.isEmpty()) {
                throw new IllegalArgumentException("str is empty");
            }

            int i = path.indexOf('/');
            if(i == 0) {
                throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
            }

            if (i < 0) {
                final int e = path.indexOf('=');
                if (e <= 0 || e == path.length() - 1) {
                    throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
                }
                final String value = path.substring(e + 1);
                if(value.charAt(0) == '$') {
                    configId = null;
                    this.values = new String[] { value };
                    this.path = ConfigPath.create(new String[] { path.substring(0, e) });
                } else {
                    configId = ConfigId.create(ConfigPath.create(new String[] { path.substring(0, e) }), new String[]{value});
                    this.values = null;
                    this.path = null;
                }
            } else {
                boolean includesParams = false;
                final List<String> names = new ArrayList<>();
                final List<String> values = new ArrayList<>();
                int c = 0;
                while (i > 0) {
                    final int e = path.indexOf('=', c);
                    if (e < 0 || e == c || e >= i - 1) {
                        throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
                    }
                    names.add(path.substring(c, e));
                    if(includesParams) {
                        values.add(path.substring(e + 1, i));
                    } else {
                        final String value = path.substring(e + 1, i);
                        includesParams = value.charAt(0) == '$';
                        values.add(value);
                    }
                    c = i + 1;
                    i = path.indexOf('/', c);
                }
                final int e = path.indexOf('=', c);
                if (e < 0 || e == c || e == path.length() - 1) {
                    throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
                }
                names.add(path.substring(c, e));
                values.add(path.substring(e + 1));

                if(includesParams) {
                    this.configId = null;
                    this.path = ConfigPath.create(names.toArray(new String[names.size()]));
                    this.values = values.toArray(new String[values.size()]);
                } else {
                    this.configId = ConfigId.create(ConfigPath.create(names.toArray(new String[names.size()])), values.toArray(new String[values.size()]));
                    this.path = null;
                    this.values = null;
                }
            }

            this.optional = optional;
        }

        @Override
        public ConfigId getConfigId() throws ProvisioningDescriptionException {
            if(configId != null) {
                return configId;
            }
            final String[] values = new String[this.values.length];
            int i = 0;
            while(i < values.length) {
                final String value = this.values[i];
                if(value.charAt(0) == '$') {
                    values[i++] = getParameterValue(value.substring(1), true);
                } else {
                    values[i++] = value;
                }
            }
            return ConfigId.create(path, values);
        }

        @Override
        public boolean isOptional() {
            return optional;
        }
    }

    public static FeatureConfig forName(String name) {
        return new FeatureConfig(name);
    }

    String configName;
    Map<String, String> params = Collections.emptyMap();
    Set<Dependency> dependencies = Collections.emptySet();

    public FeatureConfig() {
    }

    public FeatureConfig(String configName) {
        this.configName = configName;
    }

    public FeatureConfig setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public FeatureConfig addParameter(String name, String value) {
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

    public FeatureConfig addDependency(final Dependency dependency) {
        switch(dependencies.size()) {
            case 0:
                dependencies = Collections.singleton(dependency);
                break;
            case 1:
                dependencies = new LinkedHashSet<>(dependencies);
            default:
                dependencies.add(dependency);
        }
        return this;
    }

    public FeatureConfig addDependency(String path) {
        return addDependency(new ParameterizedDependency(path, false));
    }

    public FeatureConfig addDependency(String path, boolean optional) {
        return addDependency(new ParameterizedDependency(path, optional));
    }

    public String getParameterValue(String name, boolean required) throws ProvisioningDescriptionException {
        final String value = params.get(name);
        if(value == null && required) {
            throw new ProvisioningDescriptionException(configName + " configuration is missing required parameter '" + name + "'");
        }
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((configName == null) ? 0 : configName.hashCode());
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
        if (configName == null) {
            if (other.configName != null)
                return false;
        } else if (!configName.equals(other.configName))
            return false;
        return true;
    }
}
