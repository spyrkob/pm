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

import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigSchema {

    public interface ConfigSchemaLoader {

        ConfigSchema loadSchema(String schemaName);
    }

    final ConfigSchema parent;
    final Set<String> specs;
    final ConfigSchemaLoader schemaLoader;

    ConfigSchema(ConfigSchema parent, Set<String> specs, ConfigSchemaLoader schemaLoader) {
        this.parent = parent;
        this.specs = specs;
        this.schemaLoader = schemaLoader;
    }

    public ConfigSchema add(FeatureConfig config) throws ProvisioningDescriptionException {
        if(!specs.contains(config.specName)) {
            throw new ProvisioningDescriptionException("The schema does not contain spec " + config.specName);
        }
        // TODO add the ID param(s)
        parent.add(config);
        return this;
    }

    public ConfigSchema getParent() {
        return parent;
    }

    public ConfigSchema getSchema(String name) throws ProvisioningDescriptionException {
        return schemaLoader.loadSchema(name);
    }
}
