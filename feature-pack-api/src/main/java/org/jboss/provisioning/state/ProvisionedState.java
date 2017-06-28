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

package org.jboss.provisioning.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;

/**
 * Represents provisioned installation.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedState implements FeaturePackSet<ProvisionedFeaturePack> {

    public static class Builder {
        private Map<ArtifactCoords.Gav, ProvisionedFeaturePack> featurePacks = Collections.emptyMap();
        private List<ProvisionedConfig> configs = Collections.emptyList();

        private Builder() {
        }

        public Builder addFeaturePack(ProvisionedFeaturePack fp) {
            switch(featurePacks.size()) {
                case 0:
                    featurePacks = Collections.singletonMap(fp.getGav(), fp);
                    break;
                case 1:
                    final Map.Entry<ArtifactCoords.Gav, ProvisionedFeaturePack> first = featurePacks.entrySet().iterator().next();
                    featurePacks = new LinkedHashMap<>(2);
                    featurePacks.put(first.getKey(), first.getValue());
                default:
                    featurePacks.put(fp.getGav(), fp);
            }
            return this;
        }

        public Builder addConfig(ProvisionedConfig config) {
            switch(configs.size()) {
                case 0:
                    configs = Collections.singletonList(config);
                    break;
                case 1:
                    final ProvisionedConfig first = configs.get(0);
                    configs = new ArrayList<>(2);
                    configs.add(first);
                default:
                    configs.add(config);
            }
            return this;
        }

        public ProvisionedState build() {
            return new ProvisionedState(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ArtifactCoords.Gav, ProvisionedFeaturePack> featurePacks;
    private final List<ProvisionedConfig> configs;

    ProvisionedState(Builder builder) {
        this.featurePacks = builder.featurePacks.size() > 1 ? Collections.unmodifiableMap(builder.featurePacks) : builder.featurePacks;
        this.configs = builder.configs.size() > 1 ? Collections.unmodifiableList(builder.configs) : builder.configs;
    }

    @Override
    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    @Override
    public Set<ArtifactCoords.Gav> getFeaturePackGavs() {
        return featurePacks.keySet();
    }

    @Override
    public Collection<ProvisionedFeaturePack> getFeaturePacks() {
        return featurePacks.values();
    }

    @Override
    public ProvisionedFeaturePack getFeaturePack(ArtifactCoords.Gav gav) {
        return featurePacks.get(gav);
    }

    @Override
    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    @Override
    public List<ProvisionedConfig> getConfigs() {
        return configs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configs == null) ? 0 : configs.hashCode());
        result = prime * result + ((featurePacks == null) ? 0 : featurePacks.hashCode());
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
        ProvisionedState other = (ProvisionedState) obj;
        if (configs == null) {
            if (other.configs != null)
                return false;
        } else if (!configs.equals(other.configs))
            return false;
        if (featurePacks == null) {
            if (other.featurePacks != null)
                return false;
        } else if (!featurePacks.equals(other.featurePacks))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[state [");
        if(!featurePacks.isEmpty()) {
            final Iterator<ProvisionedFeaturePack> i = featurePacks.values().iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(", ").append(i.next());
            }
        }
        return buf.append("]]").toString();
    }
}
