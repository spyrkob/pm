/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.test;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription.Builder;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PmInstallFeaturePackTestBase extends PmMethodTestBase {

    @Override
    protected ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies) throws ProvisioningDescriptionException {
        Builder builder = ProvisionedInstallationDescription.builder();
        if(includeDependencies) {
            provisionedDependencies(builder);
        }
        return builder.addFeaturePack(provisionedFeaturePack()).build();
    }

    protected void provisionedDependencies(ProvisionedInstallationDescription.Builder builder) throws ProvisioningDescriptionException {
    }

    protected abstract ProvisionedFeaturePackDescription provisionedFeaturePack() throws ProvisioningDescriptionException;

    @Override
    protected void testPmMethod(ProvisioningManager pm) throws ProvisioningException {
        pm.install(provisionedFeaturePack());
    }
}
