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

import java.util.Iterator;
import java.util.Map;

import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupSpec extends FeatureGroupSupport {

    public static class Builder extends FeatureGroupSupport.Builder<FeatureGroupSpec, Builder> {

        private Builder() {
            super();
        }

        private Builder(String name) {
            super(name);
        }

        @Override
        public FeatureGroupSpec build() {
            return new FeatureGroupSpec(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    private FeatureGroupSpec(Builder builder) {
        super(builder);
    }

    public FeatureGroupSpec(FeatureGroupSpec copy) {
        super(copy);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
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
        if(!externalGroups.isEmpty()) {
            final Iterator<Map.Entry<String, FeatureGroupSpec>> i = externalGroups.entrySet().iterator();
            if(space) {
                buf.append(' ');
            } else {
                space = true;
            }
            Map.Entry<String, FeatureGroupSpec> entry = i.next();
            buf.append("extDeps=[").append(entry.getKey()).append(':').append(entry.getValue());
            while(i.hasNext()) {
                entry = i.next();
                buf.append(entry.getKey()).append(':').append(entry.getValue());
            }
            buf.append(']');
        }
        if(!localGroups.isEmpty()) {
            final Iterator<FeatureGroupConfig> i = localGroups.iterator();
            if(space) {
                buf.append(' ');
            } else {
                space = true;
            }
            buf.append("localDeps=").append(i.next());
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
