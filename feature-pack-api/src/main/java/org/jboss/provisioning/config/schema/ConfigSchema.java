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

package org.jboss.provisioning.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigSchema {

    public static class Builder {

        private XmlFeatureSpec root;
        private Map<String, XmlFeatureSpec> xmlSpecs = Collections.emptyMap();

        private int noIdCount;
        private List<FeatureConfigDescription> roots;
        private Map<String, FeatureConfigDescription> configDescr = new HashMap<>();


        private Builder() {
        }

        public Builder add(XmlFeatureSpec spec) throws ProvisioningDescriptionException {
            return add(spec, false);
        }

        public Builder add(XmlFeatureSpec spec, boolean root) throws ProvisioningDescriptionException {
            if(root) {
                if(this.root != null) {
                    throw new ProvisioningDescriptionException("Can't set schema root to " + spec.getName() + ", the root has already been set to " + this.root.getName());
                }
                this.root = spec;
            }
            switch(xmlSpecs.size()) {
                case 0:
                    xmlSpecs = Collections.singletonMap(spec.getName(), spec);
                    break;
                case 1:
                    xmlSpecs = new HashMap<>(xmlSpecs);
                default:
                    xmlSpecs.put(spec.getName(), spec);
            }
            return this;
        }

        public ConfigSchema build() throws ProvisioningDescriptionException {

            if(root == null) {
                throw new ProvisioningDescriptionException("The schema is missing root feature-spec");
            }
            if(root.features.isEmpty()) {
                throw new ProvisioningDescriptionException("Root spec doesn't include any feature");
            }

            if(root.features.size() == 1) {
                roots = Collections.singletonList(buildFeatureSpec(null, root.features.values().iterator().next()));
            } else {
                final List<FeatureConfigDescription> tmp = new ArrayList<>(root.features.size());
                for (XmlFeatureOccurence occurence : root.features.values()) {
                    tmp.add(buildFeatureSpec(null, occurence));
                }
                roots = Collections.unmodifiableList(tmp);
            }

            return new ConfigSchema(roots, configDescr);
        }

        private FeatureConfigDescription buildFeatureSpec(String parentSpot, XmlFeatureOccurence occurence) throws ProvisioningDescriptionException {

            final XmlFeatureSpec xmlSpec = xmlSpecs.get(occurence.specName);
            if(xmlSpec == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("The schema is missing feature spec ").append(occurence.specName);
                if(occurence.spot != null) {
                    buf.append(" for ").append(occurence.spot);
                }
                throw new ProvisioningDescriptionException(buf.toString());
            }

            final String spot = occurence.spot == null ? occurence.specName + ++noIdCount : occurence.spot;

            final List<FeatureConfigDescription> childDescr;
            if(xmlSpec.features.isEmpty()) {
                childDescr = Collections.emptyList();
            } else if(xmlSpec.features.size() == 1) {
                final XmlFeatureOccurence childOccurence = xmlSpec.features.values().iterator().next();
                final FeatureConfigDescription childSpec = buildFeatureSpec(spot, childOccurence);
                childDescr = Collections.singletonList(childSpec);
            } else {
                final List<FeatureConfigDescription> tmp = new ArrayList<>(xmlSpec.features.size());
                for(XmlFeatureOccurence childOccurence : xmlSpec.features.values()) {
                    final FeatureConfigDescription childSpec = buildFeatureSpec(spot, childOccurence);
                    tmp.add(childSpec);
                }
                childDescr = Collections.unmodifiableList(tmp);
            }

            final FeatureConfigDescription descr = new FeatureConfigDescription(spot, parentSpot, childDescr, xmlSpec, occurence);
            configDescr.put(spot, descr);
            return descr;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<FeatureConfigDescription> roots;
    private final Map<String, FeatureConfigDescription> configDescr;

    private ConfigSchema(List<FeatureConfigDescription> roots, Map<String, FeatureConfigDescription> featureSpecs) throws ProvisioningDescriptionException {
        this.roots = roots;
        this.configDescr = featureSpecs;
    }

    public List<FeatureConfigDescription> getRoots() {
        return roots;
    }

    public FeatureConfigDescription getDescription(String spot) throws ProvisioningDescriptionException {
        final FeatureConfigDescription descr = configDescr.get(spot);
        if(descr == null) {
            throw new ProvisioningDescriptionException("feature descr not found " + spot);
        }
        return descr;
    }
}
