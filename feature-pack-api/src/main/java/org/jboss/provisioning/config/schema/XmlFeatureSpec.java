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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class XmlFeatureSpec {

    final String name;
    Map<String, FeatureParameter> parameters = Collections.emptyMap();
    String idParam;
    Set<ConfigRef> references = Collections.emptySet();
    Map<String, XmlFeatureOccurence> features = Collections.emptyMap();

    public XmlFeatureSpec(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addFeature(XmlFeatureOccurence feature) {
        switch (features.size()) {
            case 0:
                features = Collections.singletonMap(feature.specName, feature);
                break;
            case 1:
                features = new LinkedHashMap<>(features);
            default:
                features.put(feature.specName, feature);
        }
    }

    public void addReference(ConfigRef configRef) {
        switch (references.size()) {
            case 0:
                references = Collections.singleton(configRef);
                break;
            case 1:
                references = new HashSet<>(references);
            default:
                references.add(configRef);
        }
    }

    public void addParameter(FeatureParameter param) throws ProvisioningDescriptionException {
        if (param.id) {
            if (idParam != null) {
                throw new ProvisioningDescriptionException("Can't overwrite ID parameter " + idParam + " with " + param.name);
            }
            idParam = param.name;
        }
        switch (parameters.size()) {
            case 0:
                parameters = Collections.singletonMap(param.getName(), param);
                break;
            case 1:
                parameters = new HashMap<>(parameters);
            default:
                parameters.put(param.getName(), param);
        }
    }
}
