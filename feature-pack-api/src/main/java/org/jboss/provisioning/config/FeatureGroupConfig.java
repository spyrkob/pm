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

import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.SpecId;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupConfig extends FeatureGroupConfigSupport {

    public static class Builder extends FeatureGroupConfigBuilderSupport<FeatureGroupConfig, Builder> {

        private Builder(String featureGroupName, boolean inheritFeatures) {
            super(featureGroupName);
            this.inheritFeatures = inheritFeatures;
        }

        @Override
        public FeatureGroupConfig build() {
            return new FeatureGroupConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder(null, true);
    }

    public static Builder builder(boolean inheritFeatures) {
        return new Builder(null, inheritFeatures);
    }

    public static Builder builder(String featureGroupName) {
        return builder(featureGroupName, true);
    }

    public static Builder builder(String featureGroupName, boolean inheritFeatures) {
        return new Builder(featureGroupName, inheritFeatures);
    }

    public static FeatureGroupConfig forGroup(String featureGroupName) {
        return new FeatureGroupConfig(featureGroupName);
    }

    private FeatureGroupConfig(String name) {
        super(name);
    }

    private FeatureGroupConfig(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(name != null) {
            buf.append(name);
        }
        if(!inheritFeatures) {
            buf.append(" inherit-features=false");
        }
        if(!includedSpecs.isEmpty()) {
            buf.append(" includedSpecs=");
            final Iterator<SpecId> i = includedSpecs.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!excludedSpecs.isEmpty()) {
            buf.append(" exlcudedSpecs=");
            final Iterator<SpecId> i = excludedSpecs.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
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
            final Iterator<FeatureId> i = excludedFeatures.iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        return buf.append(']').toString();
    }
}
