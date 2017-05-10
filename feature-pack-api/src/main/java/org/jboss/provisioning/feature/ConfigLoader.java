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

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public interface ConfigLoader {

    ConfigLoader NOT_CONFIGURED = new ConfigLoader() {
        @Override
        public Config load(String configSource, String configName) throws ProvisioningDescriptionException {
            throw new ProvisioningDescriptionException("Failed to load config " + configName + " from " + configSource + ". Config loading has not been setup.");
        }
    };

    Config load(String configSource, String configName) throws ProvisioningDescriptionException;
}
