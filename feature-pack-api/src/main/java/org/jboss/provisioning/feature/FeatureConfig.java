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

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfig implements BuilderWithFeatures<FeatureConfig> {

    public static FeatureConfig newConfig(FeatureId id) throws ProvisioningDescriptionException {
        final FeatureConfig config = new FeatureConfig(id.specId.name);
        for(Map.Entry<String, String> param : id.params.entrySet()) {
            config.setParam(param.getKey(), param.getValue());
        }
        return config;
    }

    public static FeatureConfig newConfig(String specName) throws ProvisioningDescriptionException {
        return new FeatureConfig(specName);
    }

    SpecId specId;
    Map<String, String> params = Collections.emptyMap();
    Set<FeatureId> dependencies = Collections.emptySet();
    String parentRef;
    List<FeatureConfig> nested = Collections.emptyList();

    public FeatureConfig() {
    }

    public FeatureConfig(String specName) throws ProvisioningDescriptionException {
        this.specId = SpecId.fromString(specName);
    }

    public FeatureConfig(SpecId specId) throws ProvisioningDescriptionException {
        this.specId = specId;
    }

    public FeatureConfig setSpecName(String specName) throws ProvisioningDescriptionException {
        this.specId = SpecId.fromString(specName);
        return this;
    }

    public SpecId getSpecId() {
        return this.specId;
    }

    public String getParentRef() {
        return this.parentRef;
    }

    public FeatureConfig setParentRef(String parentRef) {
        this.parentRef = parentRef;
        return this;
    }

    public boolean hasParams() {
        return !params.isEmpty();
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getParam(String name) {
        return params.get(name);
    }

    public FeatureConfig setParam(String name, String value) {
        putParam(name, value);
        return this;
    }

    public String putParam(String name, String value) {
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

    public Set<FeatureId> getDependencies() {
        return dependencies;
    }

    @Override
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

    public boolean hasNested() {
        return !nested.isEmpty();
    }

    public List<FeatureConfig> getNested() {
        return nested;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((nested == null) ? 0 : nested.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((parentRef == null) ? 0 : parentRef.hashCode());
        result = prime * result + ((specId == null) ? 0 : specId.hashCode());
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
        if (specId == null) {
            if (other.specId != null)
                return false;
        } else if (!specId.equals(other.specId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(specId);
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
