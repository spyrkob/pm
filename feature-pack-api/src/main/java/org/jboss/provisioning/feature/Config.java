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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class Config {

    public static class Builder {

        String name;
        List<ConfigDependency> dependencies = Collections.emptyList();
        List<FeatureConfig> features = Collections.emptyList();

        private Builder() {
        }

        private Builder(String name) {
            this.name = name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addDependency(ConfigDependency dep) {
            switch (dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dep);
                    break;
                case 1:
                    dependencies = new ArrayList<>(dependencies);
                default:
                    dependencies.add(dep);
            }
            return this;
        }

        public Builder addFeature(FeatureConfig feature) {
            switch(features.size()) {
                case 0:
                    features = Collections.singletonList(feature);
                    break;
                case 1:
                    features = new ArrayList<>(features);
                default:
                    features.add(feature);
            }
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    final String name;
    final List<ConfigDependency> dependencies;
    final List<FeatureConfig> features;

    private Config(Builder builder) {
        this.name = builder.name;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableList(builder.dependencies) : builder.dependencies;
        this.features = builder.features.size() > 1 ? Collections.unmodifiableList(builder.features) : builder.features;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((features == null) ? 0 : features.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Config other = (Config) obj;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        boolean space = false;
        if(name != null) {
            buf.append(name);
            space = true;
        }
        if(!dependencies.isEmpty()) {
            final Iterator<ConfigDependency> i = dependencies.iterator();
            if(space) {
                buf.append(' ');
            } else {
                space = true;
            }
            buf.append("deps=").append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!features.isEmpty()) {
            final Iterator<FeatureConfig> i = features.iterator();
            if(space) {
                buf.append(' ');
            }
            buf.append("features=").append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        return buf.append(']').toString();
    }
}
