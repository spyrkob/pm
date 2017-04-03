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

package org.jboss.provisioning.plugin.wildfly;

import java.nio.file.Path;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.wildfly.config.PackageScripts;
import org.jboss.provisioning.runtime.FeaturePackRuntime;
import org.jboss.provisioning.runtime.PackageRuntime;
import org.jboss.provisioning.runtime.ProvisioningRuntime;

/**
 * Collects the CLI scripts from the packages and runs them to produce the configuration.
 *
 * @author Alexey Loubyansky
 */
class StandaloneConfigGenerator extends ScriptCollector {

    StandaloneConfigGenerator(ProvisioningRuntime ctx) throws ProvisioningException {
        super(ctx);
    }

    void init(String configName) throws ProvisioningException {
        super.init(configName, "embed-server --empty-config --remove-existing --server-config=" + configName);
    }

    @Override
    protected void collect(PackageScripts scripts, FeaturePackRuntime fp, final PackageRuntime pkg, final Path wfDir, final boolean includeStatic)
            throws ProvisioningException {
        addScripts(fp, pkg, wfDir, includeStatic, scripts.getStandalone());
    }
}