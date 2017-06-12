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

package org.jboss.provisioning.util;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureConfigLoader;
import org.jboss.provisioning.feature.FeatureId;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultFeatureConfigLoader implements FeatureConfigLoader {

    public static DefaultFeatureConfigLoader newInstance(Path baseDir) {
        return new DefaultFeatureConfigLoader(new NamedFeatureConfigLoader(baseDir));
    }

    public static DefaultFeatureConfigLoader newInstance(FeatureConfigLoader defaultLoader) {
        return new DefaultFeatureConfigLoader(defaultLoader);
    }

    private final FeatureConfigLoader defaultLoader;
    private Map<String, FeatureConfigLoader> loadersBySpec;

    private DefaultFeatureConfigLoader(FeatureConfigLoader defaultLoader) {
        this.defaultLoader = defaultLoader;
    }

    public DefaultFeatureConfigLoader addLoader(String spec, FeatureConfigLoader loader) {
        if(loadersBySpec == null) {
            loadersBySpec = Collections.singletonMap(spec, loader);
        } else {
            if(loadersBySpec.size() == 1) {
                loadersBySpec = new HashMap<>(loadersBySpec);
            }
            loadersBySpec.put(spec, loader);
        }
        return this;
    }

    @Override
    public FeatureConfig load(FeatureId featureId) throws ProvisioningDescriptionException {
        if(loadersBySpec == null) {
            return defaultLoader.load(featureId);
        }
        return loadersBySpec.getOrDefault(featureId.getSpec().toString(), defaultLoader).load(featureId);
    }
}
