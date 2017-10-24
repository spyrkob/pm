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
class DomainConfigGenerator extends ScriptCollector {

    DomainConfigGenerator(ProvisioningRuntime ctx) throws ProvisioningException {
        super(ctx);
        init("domain", "embed-host-controller --empty-host-config --empty-domain-config --remove-existing-host-config --remove-existing-domain-config --temp-host-controller-name=master");
    }

    @Override
    protected void collect(final PackageScripts scripts,
            final FeaturePackRuntime fp,
            final PackageRuntime pkg,
            final Path wfDir,
            final boolean includeStatic) throws ProvisioningException {
        addScripts(fp, pkg, wfDir, includeStatic, scripts.getDomain());
        addScripts(fp, pkg, wfDir, includeStatic, scripts.getHost());
    }
}