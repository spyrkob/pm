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

package org.jboss.provisioning.featurepack.config.schema.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.schema.ConfigRef;
import org.jboss.provisioning.config.schema.FeatureConfig;
import org.jboss.provisioning.config.schema.FeatureConfigResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class MapFeatureConfigResolver implements FeatureConfigResolver {

    public static class Builder {

        private Builder() {
        }

        private Map<ConfigRef, FeatureConfig> configs = Collections.emptyMap();

        public Builder add(FeatureConfig config) {
            switch(configs.size()) {
                case 0:
                    configs = Collections.singletonMap(config.getRef(), config);
                    break;
                case 1:
                    configs = new HashMap<>(configs);
                default:
                    configs.put(config.getRef(), config);
            }
            return this;
        }

        public FeatureConfigResolver build() {
            return new MapFeatureConfigResolver(configs.size() > 1 ? Collections.unmodifiableMap(configs) : configs);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ConfigRef, FeatureConfig> configs;

    private MapFeatureConfigResolver(Map<ConfigRef, FeatureConfig> configs) {
        this.configs = configs;
    }

    @Override
    public FeatureConfig resolve(ConfigRef configId) throws ProvisioningException {
        return configs.get(configId);
    }
}
