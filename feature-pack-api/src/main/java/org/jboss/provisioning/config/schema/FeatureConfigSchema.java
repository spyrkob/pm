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
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigSchema {

    public static class Builder {

        private Map<ConfigTypeId, FeatureConfigType> types = Collections.emptyMap();

        private Builder() {
        }

        public Builder add(FeatureConfigType type) {
            switch(types.size()) {
                case 0:
                    types = Collections.singletonMap(type.id, type);
                    break;
                case 1:
                    types = new HashMap<>(types);
                default:
                    types.put(type.id, type);
            }
            return this;
        }

        public FeatureConfigSchema build() {
            return new FeatureConfigSchema(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ConfigTypeId, FeatureConfigType> types;

    private FeatureConfigSchema(Builder builder) {
        types = builder.types.size() > 1 ? Collections.unmodifiableMap(builder.types) : builder.types;
    }

    public FeatureConfigType getType(ConfigTypeId id) throws ProvisioningDescriptionException {
        final FeatureConfigType type = types.get(id);
        if(type == null) {
            throw new ProvisioningDescriptionException("type not found " + id);
        }
        return type;
    }
}
