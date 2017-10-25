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

package org.jboss.provisioning.config;

import java.util.Iterator;
import java.util.Map;

import org.jboss.provisioning.spec.ConfigId;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.util.StringUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class IncludedConfig extends FeatureGroupConfigSupport {

    public static class Builder extends FeatureGroupConfigBuilderSupport<IncludedConfig, Builder> {

        private final String model;

        private Builder(String model, String name) {
            super(name);
            this.model = model;
        }

        @Override
        public IncludedConfig build() {
            return new IncludedConfig(this);
        }
    }

    public static Builder builder(String model, String name) {
        return new Builder(model, name);
    }

    final ConfigId id;

    private IncludedConfig(Builder builder) {
        super(builder);
        this.id = new ConfigId(builder.model, builder.name);
    }

    public ConfigId getId() {
        return id;
    }

    public String getModel() {
        return id.getModel();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id.getModel() == null) ? 0 : id.getModel().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        final IncludedConfig other = (IncludedConfig) obj;
        if (id.getModel() == null) {
            if (other.id.getModel() != null)
                return false;
        } else if (!id.getModel().equals(other.id.getModel()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[model=").append(id.getModel()).append(" name=").append(id.getName());
        if(fpDep != null) {
            buf.append(" fp=").append(fpDep);
        }
        if(!inheritFeatures) {
            buf.append(" inherit-features=false");
        }
        if(!includedSpecs.isEmpty()) {
            buf.append(" includedSpecs=");
            StringUtils.append(buf, includedSpecs);
        }
        if(!excludedSpecs.isEmpty()) {
            buf.append(" exlcudedSpecs=");
            StringUtils.append(buf, excludedSpecs);
        }
        if(!includedFeatures.isEmpty()) {
            buf.append(" includedFeatures=[");
            final Iterator<Map.Entry<FeatureId, FeatureConfig>> i = includedFeatures.entrySet().iterator();
            Map.Entry<FeatureId, FeatureConfig> entry = i.next();
            buf.append(entry.getKey());
            if(entry.getValue() != null) {
                buf.append("->").append(entry.getValue());
            }
            while(i.hasNext()) {
                entry = i.next();
                buf.append(';').append(entry.getKey());
                if(entry.getValue() != null) {
                    buf.append("->").append(entry.getValue());
                }
            }
            buf.append(']');
        }
        if(!excludedFeatures.isEmpty()) {
            buf.append(" exlcudedFeatures=");
            StringUtils.append(buf, excludedFeatures.keySet());
        }
        return buf.append(']').toString();
    }
}
