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

package org.jboss.provisioning.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.state.ProvisionedFeature;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedConfigBuilder implements ProvisionedConfig {

    public static ProvisionedConfigBuilder builder() {
        return new ProvisionedConfigBuilder();
    }

    private String model;
    private String name;
    private Map<String, String> props = Collections.emptyMap();
    private List<ProvisionedFeature> features = Collections.emptyList();

    private ProvisionedConfigBuilder() {
    }

    public ProvisionedConfigBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ProvisionedConfigBuilder setModel(String model) {
        this.model = model;
        return this;
    }

    public ProvisionedConfigBuilder setProperty(String name, String value) {
        switch(props.size()) {
            case 0:
                props = Collections.singletonMap(name, value);
                break;
            case 1:
                final Map.Entry<String, String> entry = props.entrySet().iterator().next();
                props = new HashMap<>(2);
                props.put(entry.getKey(), entry.getValue());
            default:
                props.put(name, value);
        }
        return this;
    }

    public ProvisionedConfigBuilder addFeature(ProvisionedFeature feature) {
        switch(features.size()) {
            case 0:
                features = Collections.singletonList(feature);
                break;
            case 1:
                final ProvisionedFeature first = features.get(0);
                features = new ArrayList<>(2);
                features.add(first);
            default:
                features.add(feature);
        }
        return this;
    }

    public ProvisionedConfig build() {
        if(props.size() > 1) {
            props = Collections.unmodifiableMap(props);
        }
        if(features.size() > 1) {
            features = Collections.unmodifiableList(features);
        }
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public boolean hasProperties() {
        return !props.isEmpty();
    }

    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public boolean hasFeatures() {
        return !features.isEmpty();
    }

    @Override
    public void handle(ProvisionedConfigHandler handler) throws ProvisioningException {
        // TODO Auto-generated method stub

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        ProvisionedConfigBuilder other = (ProvisionedConfigBuilder) obj;
        System.out.println("ProvisionedConfigBuilder.equals " + features + " " + other.features);

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
}
