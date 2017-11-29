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

package org.jboss.provisioning.test;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PmTestBase extends FeaturePackRepoTestBase {

    protected abstract void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException;

    protected abstract ProvisioningConfig provisionedConfig() throws ProvisioningException;

    protected abstract ProvisionedState provisionedState() throws ProvisioningException;

    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.build();
    }

    protected abstract void testPm(ProvisioningManager pm) throws ProvisioningException;

    @Override
    protected void doBefore() throws Exception {
        super.doBefore();
        setupRepo(getRepoManager());
    }

    @Test
    public void main() throws Exception {
        final ProvisioningManager pm = getPm();
        try {
            testPm(pm);
            pmSuccess();
        } catch(ProvisioningException e) {
            pmFailure(e);
        }
        assertProvisionedConfig(pm);
        assertProvisionedState(pm);
        assertProvisionedContent();
    }

    protected void pmSuccess() {
    }


    protected void pmFailure(ProvisioningException e) throws ProvisioningException {
        throw e;
    }

    protected void assertProvisionedState(final ProvisioningManager pm) throws ProvisioningException {
        assertProvisionedState(pm, provisionedState());
    }

    protected void assertProvisionedConfig(final ProvisioningManager pm) throws ProvisioningException {
        assertProvisioningConfig(pm, provisionedConfig());
    }

    protected void assertProvisionedContent() {
        provisionedHomeDir(DirState.rootBuilder().skip(Constants.PROVISIONED_STATE_DIR)).assertState(installHome);
    }

    protected void assertErrors(Throwable t, String... msgs) {
        int i = 0;
        while(t != null && i < msgs.length) {
            Assert.assertEquals(msgs[i++], t.getLocalizedMessage());
            t = t.getCause();
        }
        if(t != null) {
            Assert.fail("Unexpected error: " + t.getLocalizedMessage());
        }
        if(i < msgs.length - 1) {
            Assert.fail("Not reported error: " + msgs[i]);
        }
    }
}
