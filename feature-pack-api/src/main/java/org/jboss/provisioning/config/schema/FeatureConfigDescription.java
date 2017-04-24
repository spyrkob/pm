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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigDescription {

    final String spot;
    final String parentSpot;
    final SchemaPath path;
    final SchemaPath parentPath;
    final boolean required;
    final boolean maxOccursUnbounded;
    final List<FeatureConfigDescription> features;
    final Map<String, FeatureParameter> params;
    final String idParam;
    final Set<SpotRef> spotRefs;

    FeatureConfigDescription(String spot, String parentSpot, SchemaPath schemaPath, SchemaPath parentPath,
            List<FeatureConfigDescription> features, XmlFeatureSpec xmlSpec, XmlFeatureOccurence occurence) {
        this.spot = spot;
        this.parentSpot = parentSpot;
        this.path = schemaPath;
        this.parentPath = parentPath;
        this.required = occurence.required;
        this.maxOccursUnbounded = occurence.maxOccursUnbounded;
        this.features = features;

        params = xmlSpec.parameters.size() > 1 ? Collections.unmodifiableMap(xmlSpec.parameters) : xmlSpec.parameters;
        idParam = xmlSpec.idParam;
        spotRefs = xmlSpec.references.size() > 1 ? Collections.unmodifiableSet(xmlSpec.references) : xmlSpec.references;
    }

    public ConfigRef getConfigRef(FeatureConfig config) throws ProvisioningDescriptionException {
        if(path == null) {
            throw new ProvisioningDescriptionException("Feature " + spot + " is not associated with a schema path.");
        }
        if(idParam == null) {
            throw new ProvisioningDescriptionException("ID parameter is not defined for feature " + path);
        }
        final String[] values = new String[path.length()];
        int i = 0;
        while(i < values.length - 1) {
            values[i] = config.getParameterValue(parentPath.getName(i++), true);
        }
        values[i] = config.getParameterValue(idParam, true);
        return ConfigRef.create(path, values);
    }

    public ConfigRef getConfigRef(SchemaPath path, String[] params, boolean required, FeatureConfig config) throws ProvisioningDescriptionException {
        final String[] values = new String[params.length];
        int i = 0;
        while(i < values.length) {
            final String value = config.getParameterValue(params[i], required);
            if(value == null) {
                return null;
            }
            values[i++] = value;
        }
        return ConfigRef.create(path, values);
    }
}
