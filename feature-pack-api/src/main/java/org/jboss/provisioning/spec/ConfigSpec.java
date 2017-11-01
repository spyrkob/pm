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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.util.StringUtils;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigSpec extends FeatureGroupSupport {

    public static class Builder extends FeatureGroupSupport.Builder<ConfigSpec, Builder> {

        private String model;
        private Map<String, String> props = Collections.emptyMap();

        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        public Builder setProperty(String name, String value) {
            switch(props.size()) {
                case 0:
                    props = Collections.singletonMap(name, value);
                    break;
                case 1:
                    props = new HashMap<>(props);
                default:
                    props.put(name, value);
            }
            return this;
        }

        @Override
        public ConfigSpec build() {
            return new ConfigSpec(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    final ConfigId id;
    final Map<String, String> props;

    private ConfigSpec(Builder builder) {
        super(builder);
        this.id = new ConfigId(builder.model, builder.name);
        this.props = PmCollections.map(builder.props);
    }

    public ConfigId getId() {
        return id;
    }

    public String getModel() {
        return id.model;
    }

    public boolean hasProperties() {
        return !props.isEmpty();
    }

    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id.model == null) ? 0 : id.model.hashCode());
        result = prime * result + ((props == null) ? 0 : props.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigSpec other = (ConfigSpec) obj;
        if (id.model == null) {
            if (other.id.model != null)
                return false;
        } else if (!id.model.equals(other.id.model))
            return false;
        if (props == null) {
            if (other.props != null)
                return false;
        } else if (!props.equals(other.props))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(name != null) {
            buf.append(name).append(' ');
        }
        if(id.model != null) {
            buf.append("model=").append(id.model).append(' ');
        }
        if(!props.isEmpty()) {
            StringUtils.append(buf, props.entrySet());
        }
        if(!items.isEmpty()) {
            buf.append(' ');
            StringUtils.append(buf, items);
        }
        return buf.toString();
    }
}
