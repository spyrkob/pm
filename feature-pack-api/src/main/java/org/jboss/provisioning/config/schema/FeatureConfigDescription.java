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

    final String configName;
    final String parentConfigName;
    final ConfigPath path;
    final ConfigPath parentPath;
    final boolean required;
    final boolean maxOccursUnbounded;
    final List<FeatureConfigDescription> features;
    final Map<String, FeatureParameter> params;
    final String idParam;
    final Set<ConfigRef> configRefs;

    FeatureConfigDescription(String configName, String parentConfigName, ConfigPath configPath, ConfigPath parentPath,
            List<FeatureConfigDescription> features, XmlFeatureSpec xmlSpec, XmlFeatureOccurence occurence) {
        this.configName = configName;
        this.parentConfigName = parentConfigName;
        this.path = configPath;
        this.parentPath = parentPath;
        this.required = occurence.required;
        this.maxOccursUnbounded = occurence.maxOccursUnbounded;
        this.features = features;

        params = xmlSpec.parameters.size() > 1 ? Collections.unmodifiableMap(xmlSpec.parameters) : xmlSpec.parameters;
        idParam = xmlSpec.idParam;
        configRefs = xmlSpec.references.size() > 1 ? Collections.unmodifiableSet(xmlSpec.references) : xmlSpec.references;
    }

    public ConfigId getConfigId(FeatureConfig config) throws ProvisioningDescriptionException {
        if(path == null) {
            throw new ProvisioningDescriptionException("Feature " + configName + " is not associated with a schema path.");
        }
        if(idParam == null) {
            throw new ProvisioningDescriptionException("ID parameter is not defined for feature " + path);
        }
        final String[] values = new String[path.length()];
        int i = 0;
        while(i < values.length - 1) {
            values[i] = config.getParameterValue(parentPath.names[i++], true);
        }
        values[i] = config.getParameterValue(idParam, true);
        return ConfigId.create(path, values);
    }

    public ConfigId getConfigId(ConfigPath path, String[] params, boolean required, FeatureConfig config) throws ProvisioningDescriptionException {
        final String[] values = new String[params.length];
        int i = 0;
        while(i < values.length) {
            final String value = config.getParameterValue(params[i], required);
            if(value == null) {
                return null;
            }
            values[i++] = value;
        }
        return ConfigId.create(path, values);
    }
}
