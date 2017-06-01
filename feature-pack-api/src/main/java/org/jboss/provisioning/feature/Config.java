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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Alexey Loubyansky
 */
public class Config implements BuilderWithFeatureGroups<Config> {

    String name;
    String model;
    Map<String, String> props = Collections.emptyMap();
    List<FeatureGroupConfig> featureGroups = Collections.emptyList();
    List<FeatureConfig> features = Collections.emptyList();

    public Config() {
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

    public boolean hasFeatureGroups() {
        return !featureGroups.isEmpty();
    }

    public List<FeatureGroupConfig> getFeatureGroups() {
        return featureGroups;
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
        return this;
    }

    @Override
    public Config addFeatureGroup(FeatureGroupConfig fg) {
        switch(featureGroups.size()) {
            case 0:
                featureGroups = Collections.singletonList(fg);
                break;
            case 1:
                featureGroups = new ArrayList<>(featureGroups);
            default:
                featureGroups.add(fg);
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
        result = prime * result + ((featureGroups == null) ? 0 : featureGroups.hashCode());
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
        if (featureGroups == null) {
            if (other.featureGroups != null)
                return false;
        } else if (!featureGroups.equals(other.featureGroups))
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
        if(!featureGroups.isEmpty()) {
            buf.append(' ').append(featureGroups.get(0));
            int i = 1;
            while(i < featureGroups.size()) {
                final FeatureGroupConfig dep = featureGroups.get(i++);
                buf.append(',').append(dep);
            }
        }
        return buf.toString();
    }
}
