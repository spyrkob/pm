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

package org.jboss.provisioning.test.util;

import java.util.ServiceLoader;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.state.ProvisionedConfig;

/**
 * @author Alexey Loubyansky
 *
 */
public class TestConfigHandlersProvisioningPlugin implements ProvisioningPlugin {

    @Override
    public void postInstall(ProvisioningRuntime ctx) throws ProvisioningException {
        if (ctx.hasConfigs()) {
            final ServiceLoader<ProvisionedConfigHandler> handlers = ServiceLoader.load(ProvisionedConfigHandler.class);
            for (ProvisionedConfigHandler handler : handlers) {
                for (ProvisionedConfig config : ctx.getConfigs()) {
                    config.handle(handler);
                }
            }
        }
    }
}
