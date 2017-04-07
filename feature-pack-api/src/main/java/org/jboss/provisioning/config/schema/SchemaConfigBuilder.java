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

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.schema.FeatureConfigType.Occurence;

/**
 *
 * @author Alexey Loubyansky
 */
public class SchemaConfigBuilder {

    public class FeatureBuilder {
        private final FeatureBuilder parent;
        private final FeatureConfig.Builder builder;

        private FeatureBuilder(FeatureBuilder parent, FeatureConfig.Builder builder) {
            this.parent = parent;
            this.builder = builder;
        }

        public FeatureBuilder configureNew(ConfigTypeId typeId) throws ProvisioningException {
            return configureNew(typeId, null);
        }

        public FeatureBuilder configureNew(ConfigTypeId typeId, String name) throws ProvisioningException {
            final Occurence occurence = builder.type.occurences.get(typeId);
            if(occurence == null) {
                throw new ProvisioningDescriptionException(typeId + " is not allowed as a child of " + builder.type.id);
            }
            final FeatureConfigType type = schema.getType(typeId);
            return new FeatureBuilder(this, type.configBuilder(name));
        }

        public FeatureBuilder dependsOn(String depId, ConfigTypeId typeId, String name) throws ProvisioningDescriptionException {
            return dependsOn(depId, ConfigRef.create(typeId, name));
        }

        public FeatureBuilder dependsOn(String depId, ConfigRef ref) throws ProvisioningDescriptionException {
            builder.addDependency(depId, ref);
            return this;
        }

        public FeatureBuilder done() throws ProvisioningDescriptionException {
            if(parent != null) {
                parent.builder.addFeatureConfig(builder.build());
            }
            return parent;
        }

        public FeatureConfig build() throws ProvisioningDescriptionException {
            return builder.build();
        }
    }

    public static FeatureBuilder builder(FeatureConfigSchema schema, ConfigTypeId rootId) throws ProvisioningDescriptionException {
        return new SchemaConfigBuilder(schema, rootId, null).builder;
    }

    private final FeatureConfigSchema schema;
    private final FeatureBuilder builder;

    private SchemaConfigBuilder(FeatureConfigSchema schema, ConfigTypeId rootId, String name) throws ProvisioningDescriptionException {
        this.schema = schema;
        builder = new FeatureBuilder(null, schema.getType(rootId).configBuilder(name));
    }
}
