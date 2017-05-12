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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfig {

    public static FeatureConfig newConfig(String specName) {
        return new FeatureConfig(specName);
    }

    String specName;
    Map<String, String> params = Collections.emptyMap();
    Set<FeatureId> dependencies = Collections.emptySet();
    String parentRef;
    List<FeatureConfig> nested = Collections.emptyList();

    public FeatureConfig() {
    }

    public FeatureConfig(String specName) {
        this.specName = specName;
    }

    public FeatureConfig setSpecName(String specName) {
        this.specName = specName;
        return this;
    }

    public String getSpecName() {
        return this.specName;
    }

    public FeatureConfig setParentRef(String parentRef) {
        this.parentRef = parentRef;
        return this;
    }

    public FeatureConfig setParam(String name, String value) {
        putParam(name, value);
        return this;
    }

    String putParam(String name, String value) {
        switch(params.size()) {
            case 0:
                params = Collections.singletonMap(name, value);
                return null;
            case 1:
                params = new HashMap<>(params);
            default:
                return params.put(name, value);
        }
    }

    public FeatureConfig addDependency(FeatureId featureId) {
        switch(dependencies.size()) {
            case 0:
                dependencies = Collections.singleton(featureId);
                break;
            case 1:
                dependencies = new LinkedHashSet<>(dependencies);
            default:
                dependencies.add(featureId);
        }
        return this;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public FeatureConfig addFeature(FeatureConfig config) {
        switch(nested.size()) {
            case 0:
                nested = Collections.singletonList(config);
                break;
            case 1:
                nested = new ArrayList<>(nested);
            default:
                nested.add(config);
        }
        return this;
    }

    void merge(FeatureConfig config) {
        if (!config.params.isEmpty()) {
            if (params.isEmpty()) {
                params = config.params;
            } else {
                if (params.size() == 1) {
                    params = new HashMap<>(params);
                }
                for (Map.Entry<String, String> param : config.params.entrySet()) {
                    params.put(param.getKey(), param.getValue());
                }
            }
        }
        if (!config.dependencies.isEmpty()) {
            if (dependencies.isEmpty()) {
                dependencies = config.dependencies;
            } else {
                if (dependencies.size() == 1) {
                    dependencies = new LinkedHashSet<>(dependencies);
                }
                dependencies.addAll(config.dependencies);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((nested == null) ? 0 : nested.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((parentRef == null) ? 0 : parentRef.hashCode());
        result = prime * result + ((specName == null) ? 0 : specName.hashCode());
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
        if (nested == null) {
            if (other.nested != null)
                return false;
        } else if (!nested.equals(other.nested))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (parentRef == null) {
            if (other.parentRef != null)
                return false;
        } else if (!parentRef.equals(other.parentRef))
            return false;
        if (specName == null) {
            if (other.specName != null)
                return false;
        } else if (!specName.equals(other.specName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(specName);
        if (!params.isEmpty()) {
            buf.append(' ');
            final Iterator<Map.Entry<String, String>> i = params.entrySet().iterator();
            Map.Entry<String, String> entry = i.next();
            buf.append(entry.getKey()).append('=').append(entry.getValue());
            while (i.hasNext()) {
                entry = i.next();
                buf.append(',').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        if(parentRef != null) {
            buf.append(" parentRef=").append(parentRef);
        }
        if(!dependencies.isEmpty()) {
            buf.append(" dependencies=");
            final Iterator<FeatureId> i = dependencies.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!nested.isEmpty()) {
            buf.append(" nested=");
            int i = 0;
            buf.append(nested.get(i++));
            while(i < nested.size()) {
                buf.append(',').append(nested.get(i++));
            }
        }
        return buf.append(']').toString();
    }
}
