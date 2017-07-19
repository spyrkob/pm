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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeatureGroupSupport implements FeatureGroup {

    abstract static class Builder<T extends FeatureGroupSupport, B extends Builder<T, B>> extends FeatureGroupBuilderSupport<B> {

        String name;

        protected Builder() {
        }

        protected Builder(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public B setName(String name) {
            this.name = name;
            return (B) this;
        }

        public abstract T build();
    }

    protected final String name;
    protected final Map<String, FeatureGroupSpec> externalGroups;
    protected final List<FeatureGroupConfig> localGroups;
    protected final List<FeatureConfig> features;

    protected FeatureGroupSupport(FeatureGroupSupport copy) {
        name = copy.name;
        switch(copy.externalGroups.size()) {
            case 0:
                externalGroups = Collections.emptyMap();
                break;
            case 1: {
                final Entry<String, FeatureGroupSpec> entry = copy.externalGroups.entrySet().iterator().next();
                externalGroups = Collections.singletonMap(entry.getKey(), new FeatureGroupSpec(entry.getValue()));
                break;
            }
            default:
                final Map<String, FeatureGroupSpec> tmp = new LinkedHashMap<>(copy.externalGroups.size());
                final Iterator<Map.Entry<String, FeatureGroupSpec>> i = copy.externalGroups.entrySet().iterator();
                while(i.hasNext()) {
                    final Entry<String, FeatureGroupSpec> entry = i.next();
                    tmp.put(entry.getKey(), new FeatureGroupSpec(entry.getValue()));
                }
                externalGroups = Collections.unmodifiableMap(tmp);
        }
        localGroups = copy.localGroups;
        switch(copy.features.size()) {
            case 0:
                features = Collections.emptyList();
                break;
            case 1:
                features = Collections.singletonList(new FeatureConfig(copy.features.get(0)));
                break;
            default:
                final List<FeatureConfig> tmp = new ArrayList<>(copy.features.size());
                for(FeatureConfig fc : copy.features) {
                    tmp.add(new FeatureConfig(fc));
                }
                features = Collections.unmodifiableList(tmp);
        }
    }

    protected FeatureGroupSupport(Builder<?, ?> builder) {
        name = builder.name;
        this.localGroups = builder.localGroups.size() > 1 ? Collections.unmodifiableList(builder.localGroups) : builder.localGroups;
        this.features = builder.features.size() > 1 ? Collections.unmodifiableList(builder.features) : builder.features;
        this.externalGroups = builder.buildExternalDependencies();
    }

    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#hasExternalDependencies()
     */
    @Override
    public boolean hasExternalDependencies() {
        return !externalGroups.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#getExternalDependencies()
     */
    @Override
    public Map<String, FeatureGroupSpec> getExternalDependencies() {
        return externalGroups;
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#hasLocalDependencies()
     */
    @Override
    public boolean hasLocalDependencies() {
        return !localGroups.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#getLocalDependencies()
     */
    @Override
    public List<FeatureGroupConfig> getLocalDependencies() {
        return localGroups;
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#hasFeatures()
     */
    @Override
    public boolean hasFeatures() {
        return !features.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.feature.FeatureGroup#getFeatures()
     */
    @Override
    public List<FeatureConfig> getFeatures() {
        return features;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalGroups == null) ? 0 : externalGroups.hashCode());
        result = prime * result + ((features == null) ? 0 : features.hashCode());
        result = prime * result + ((localGroups == null) ? 0 : localGroups.hashCode());
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
        FeatureGroupSupport other = (FeatureGroupSupport) obj;
        if (externalGroups == null) {
            if (other.externalGroups != null)
                return false;
        } else if (!externalGroups.equals(other.externalGroups))
            return false;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        if (localGroups == null) {
            if (other.localGroups != null)
                return false;
        } else if (!localGroups.equals(other.localGroups))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}