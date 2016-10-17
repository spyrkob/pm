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
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PmMethodTestBase extends FeaturePackRepoTestBase {

    protected abstract void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException;

    protected abstract ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies) throws ProvisioningException;

    protected abstract DirState provisionedHomeDir(DirState.DirBuilder builder);

    protected abstract void testPmMethod(ProvisioningManager pm) throws ProvisioningException;

    @Override
    protected void doBefore() throws Exception {
        super.doBefore();
        setupRepo(getRepoManager());
    }

    @Test
    public void testMain() throws Exception {
        setupRepo(getRepoManager());
        final ProvisioningManager pm = getPm();
        testPmMethod(pm);
        testProvisionedContent();
        testUserSpec(pm);
        testFullSpec(pm);
    }

    private void testFullSpec(final ProvisioningManager pm) throws ProvisioningException {
        assertSpec(pm, provisionedInstallation(true), true);
    }

    private void testUserSpec(final ProvisioningManager pm) throws ProvisioningException {
        assertSpec(pm, provisionedInstallation(false), false);
    }

    private void testProvisionedContent() {
        provisionedHomeDir(DirState.rootBuilder().skip(".pm")).assertState(installHome);
    }
}
