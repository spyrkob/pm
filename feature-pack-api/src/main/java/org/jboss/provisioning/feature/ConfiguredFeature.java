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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
class ConfiguredFeature {
    final FeatureId id;
    final FeatureSpec spec;
    Map<String, String> params = Collections.emptyMap();
    Set<FeatureId> dependencies = Collections.emptySet();
    boolean liningUp;

    ConfiguredFeature(FeatureId id, FeatureSpec spec, FeatureConfig config) {
        this.id = id;
        this.spec = spec;
        this.params = config.params;
        this.dependencies = config.dependencies;
    }

    void merge(FeatureConfig config) {
        if (!config.params.isEmpty()) {
            if (params.isEmpty()) {
                params = config.params;
            } else {
                if (params.size() == 1) {
                    params = new HashMap<>(params);
                }
                for (Map.Entry<String, String> param : config.params.entrySet()) {
                    params.put(param.getKey(), param.getValue());
                }
            }
        }
        if (!config.dependencies.isEmpty()) {
            if (dependencies.isEmpty()) {
                dependencies = config.dependencies;
            } else {
                if (dependencies.size() == 1) {
                    dependencies = new LinkedHashSet<>(dependencies);
                }
                dependencies.addAll(config.dependencies);
            }
        }
    }
}