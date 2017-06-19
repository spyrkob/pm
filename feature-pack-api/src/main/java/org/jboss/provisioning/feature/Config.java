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

/**
 *
 * @author Alexey Loubyansky
 */
public class Config extends AbstractFeatureGroup {

    public static class Builder extends AbstractFeatureGroup.Builder<Config, Builder> {

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
        public Config build() {
            return new Config(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    final String model;
    final Map<String, String> props;

    private Config(Builder builder) {
        super(builder);
        this.model = builder.model;
        this.props = builder.props.size() > 1 ? Collections.unmodifiableMap(builder.props) : builder.props;
    }
    public String getModel() {
        return model;
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
        result = prime * result + ((model == null) ? 0 : model.hashCode());
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
        Config other = (Config) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
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
        if(model != null) {
            buf.append("model=").append(model).append(' ');
        }
        if(!props.isEmpty()) {
            final Iterator<Map.Entry<String, String>> i = props.entrySet().iterator();
            Entry<String, String> next = i.next();
            buf.append(next.getKey()).append('=').append(next.getValue());
            while(i.hasNext()) {
                next = i.next();
                buf.append(',').append(next.getKey()).append('=').append(next.getValue());
            }
        }
        if(!localGroups.isEmpty()) {
            buf.append(' ').append(localGroups.get(0));
            int i = 1;
            while(i < localGroups.size()) {
                final FeatureGroupConfig dep = localGroups.get(i++);
                buf.append(',').append(dep);
            }
        }
        if(!features.isEmpty()) {
            buf.append(' ').append(features.get(0));
            int i = 1;
            while(i < features.size()) {
                final FeatureConfig dep = features.get(i++);
                buf.append(',').append(dep);
            }
        }
        return buf.toString();
    }
}
