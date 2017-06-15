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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class Config implements BuilderWithFeatureGroups<Config> {

    String name;
    String model;
    Map<String, String> props = Collections.emptyMap();
    Map<String, Map<String, FeatureGroupConfig>> externalFgs = Collections.emptyMap();
    Map<String, FeatureGroupConfig> localFgs = Collections.emptyMap();
    List<FeatureConfig> features = Collections.emptyList();

    public Config() {
    }

    public Config(String model) {
        this(null, model);
    }

    public Config(String name, String model) {
        this.name = name;
        this.model = model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public boolean hasExternalFeatureGroups() {
        return !externalFgs.isEmpty();
    }

    public Set<String> getExternalFeatureGroupSources() {
        return externalFgs.keySet();
    }

    public Collection<FeatureGroupConfig> getExternalFeatureGroups(String fpDep) {
        final Map<String, FeatureGroupConfig> fgs = externalFgs.get(fpDep);
        return fgs == null ? Collections.emptyList() : fgs.values();
    }

    public boolean hasLocalFeatureGroups() {
        return !localFgs.isEmpty();
    }

    public Collection<FeatureGroupConfig> getLocalFeatureGroups() {
        return localFgs.values();
    }

    public boolean hasFeatures() {
        return !features.isEmpty();
    }

    public List<FeatureConfig> getFeatures() {
        return features;
    }

    public Config addFeature(FeatureConfig config) {
        switch(features.size()) {
            case 0:
                features = Collections.singletonList(config);
                break;
            case 1:
                features = new ArrayList<>(features);
            default:
                features.add(config);
        }
        return this;
    }

    @Override
    public Config addFeatureGroup(String fpDep, FeatureGroupConfig fg) {
        if(externalFgs.isEmpty()) {
            externalFgs = Collections.singletonMap(fpDep, Collections.singletonMap(fg.getName(), fg));
            return this;
        }
        Map<String, FeatureGroupConfig> fpGroups = externalFgs.get(fpDep);
        if(fpGroups == null) {
            if(externalFgs.size() == 1) {
                externalFgs = new HashMap<>(externalFgs);
            }
            externalFgs.put(fpDep, Collections.singletonMap(fg.getName(), fg));
        } else {
            FeatureGroupConfig existingFg = fpGroups.get(fg.getName());
            if(existingFg == null) {
                if(fpGroups.size() == 1) {
                    fpGroups = new HashMap<>(fpGroups);
                    if(externalFgs.size() == 1) {
                        externalFgs = Collections.singletonMap(fpDep, fpGroups);
                    }
                }
                fpGroups.put(fg.getName(), fg);
            } else {
                throw new IllegalArgumentException("Duplicate feature-group " + fg.getName() + " for feature-pack dependency " + fpDep);
            }
        }
        return this;
    }

    @Override
    public Config addFeatureGroup(FeatureGroupConfig fgConfig) {
        if(localFgs.isEmpty()) {
            localFgs = Collections.singletonMap(fgConfig.getName(), fgConfig);
            return this;
        }
        final FeatureGroupConfig existingFgConfig = localFgs.get(fgConfig.getName());
        if(existingFgConfig == null) {
            if(localFgs.size() == 1) {
                localFgs = new LinkedHashMap<>(localFgs);
            }
            localFgs.put(fgConfig.getName(), fgConfig);
        } else {
            // TODO merge
            throw new IllegalArgumentException("Duplicate feature-group " + fgConfig.getName());
        }
        return this;
    }

    public Config setProperty(String name, String value) {
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

    public boolean hasProperties() {
        return !props.isEmpty();
    }

    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localFgs == null) ? 0 : localFgs.hashCode());
        result = prime * result + ((features == null) ? 0 : features.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((props == null) ? 0 : props.hashCode());
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
        if (localFgs == null) {
            if (other.localFgs != null)
                return false;
        } else if (!localFgs.equals(other.localFgs))
            return false;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
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
        if(!localFgs.isEmpty()) {
            buf.append(' ').append(localFgs.get(0));
            int i = 1;
            while(i < localFgs.size()) {
                final FeatureGroupConfig dep = localFgs.get(i++);
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
