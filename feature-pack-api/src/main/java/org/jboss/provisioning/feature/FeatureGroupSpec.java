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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupSpec {

    public static class Builder implements BuilderWithFeatures<Builder>, BuilderWithFeatureGroups<Builder> {

        String name;
        Map<String, FeatureGroupSpec.Builder> externalGroups = Collections.emptyMap();
        List<FeatureGroupConfig> localGroups = Collections.emptyList();
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

        @Override
        public Builder addFeatureGroup(String fpDep, FeatureGroupConfig group) {
            if(externalGroups.isEmpty()) {
                externalGroups = Collections.singletonMap(fpDep, FeatureGroupSpec.builder().addFeatureGroup(group));
            } else {
                FeatureGroupSpec.Builder specBuilder = externalGroups.get(fpDep);
                if(specBuilder == null) {
                    specBuilder = FeatureGroupSpec.builder().addFeatureGroup(group);
                    if(externalGroups.size() == 1) {
                        externalGroups = new HashMap<>(externalGroups);
                    }
                    externalGroups.put(fpDep, specBuilder);
                } else {
                    specBuilder.addFeatureGroup(group);
                }
            }
            return this;
        }

        public Builder addFeature(String fpDep, FeatureConfig fc) {
            if(externalGroups.isEmpty()) {
                externalGroups = Collections.singletonMap(fpDep, FeatureGroupSpec.builder().addFeature(fc));
            } else {
                FeatureGroupSpec.Builder specBuilder = externalGroups.get(fpDep);
                if(specBuilder == null) {
                    specBuilder = FeatureGroupSpec.builder().addFeature(fc);
                    if(externalGroups.size() == 1) {
                        externalGroups = new LinkedHashMap<>(externalGroups);
                    }
                    externalGroups.put(fpDep, specBuilder);
                } else {
                    specBuilder.addFeature(fc);
                }
            }
            return this;
        }

        @Override
        public Builder addFeatureGroup(FeatureGroupConfig dep) {
            switch (localGroups.size()) {
                case 0:
                    localGroups = Collections.singletonList(dep);
                    break;
                case 1:
                    localGroups = new ArrayList<>(localGroups);
                default:
                    localGroups.add(dep);
            }
            return this;
        }

        @Override
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

    final String name;
    final Map<String, FeatureGroupSpec> externalGroups;
    final List<FeatureGroupConfig> localGroups;
    final List<FeatureConfig> features;

    private FeatureGroupSpec(Builder builder) {
        this.name = builder.name;
        this.localGroups = builder.localGroups.size() > 1 ? Collections.unmodifiableList(builder.localGroups) : builder.localGroups;
        this.features = builder.features.size() > 1 ? Collections.unmodifiableList(builder.features) : builder.features;
        if(builder.externalGroups.isEmpty()) {
            this.externalGroups = Collections.emptyMap();
        } else if(builder.externalGroups.size() == 1) {
            final Map.Entry<String, FeatureGroupSpec.Builder> entry = builder.externalGroups.entrySet().iterator().next();
            this.externalGroups = Collections.singletonMap(entry.getKey(), entry.getValue().build());
        } else {
            final Iterator<Map.Entry<String, FeatureGroupSpec.Builder>> i = builder.externalGroups.entrySet().iterator();
            final Map<String, FeatureGroupSpec> tmp = new HashMap<>(builder.externalGroups.size());
            while(i.hasNext()) {
                final Map.Entry<String, FeatureGroupSpec.Builder> entry = i.next();
                tmp.put(entry.getKey(), entry.getValue().build());
            }
            this.externalGroups = Collections.unmodifiableMap(tmp);
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasExternalDependencies() {
        return !externalGroups.isEmpty();
    }

    public Map<String, FeatureGroupSpec> getExternalDependencies() {
        return externalGroups;
    }

    public boolean hasLocalDependencies() {
        return !localGroups.isEmpty();
    }

    public List<FeatureGroupConfig> getLocalDependencies() {
        return localGroups;
    }

    public boolean hasFeatures() {
        return !features.isEmpty();
    }

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
        FeatureGroupSpec other = (FeatureGroupSpec) obj;
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
