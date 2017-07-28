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

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureAnnotation {

    final String name;
    private Map<String, String> attrs = Collections.emptyMap();

    public FeatureAnnotation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public FeatureAnnotation setAttr(String name, String value) {
        if(attrs.isEmpty()) {
            attrs = Collections.singletonMap(name, value);
            return this;
        }
        if(attrs.containsKey(name)) {
            if(attrs.size() == 1) {
                attrs = Collections.singletonMap(name, value);
            } else {
                attrs.put(name, value);
            }
            return this;
        }
        if(attrs.size() == 1) {
            final Map.Entry<String, String> entry = attrs.entrySet().iterator().next();
            attrs = new HashMap<>(2);
            attrs.put(entry.getKey(), entry.getValue());
        }
        attrs.put(name, value);
        return this;
    }

    public boolean hasAttrs() {
        return !attrs.isEmpty();
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public boolean hasAttr(String name) {
        return attrs.containsKey(name);
    }

    public String getAttr(String name) {
        return attrs.get(name);
    }

    public String getAttr(String name, String defaultValue) {
        return attrs.getOrDefault(name, defaultValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
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
        FeatureAnnotation other = (FeatureAnnotation) obj;
        if (attrs == null) {
            if (other.attrs != null)
                return false;
        } else if (!attrs.equals(other.attrs))
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
        buf.append("name=").append(name);
        if(!attrs.isEmpty()) {
            buf.append(" attrs={");
            final Iterator<Map.Entry<String, String>> i = attrs.entrySet().iterator();
            Map.Entry<String, String> entry = i.next();
            buf.append(entry.getKey()).append('=').append(entry.getValue());
            while(i.hasNext()) {
                entry = i.next();
                buf.append(';').append(entry.getKey()).append('=').append(entry.getValue());
            }
            buf.append('}');
        }
        return buf.toString();
    }
}
