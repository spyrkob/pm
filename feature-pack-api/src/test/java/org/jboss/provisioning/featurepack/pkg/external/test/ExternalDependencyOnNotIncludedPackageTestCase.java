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

package org.jboss.provisioning.featurepack.pkg.external.test;


import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExternalDependencyOnNotIncludedPackageTestCase extends PmProvisionConfigTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0.Final"))
            .addDependency("fp2-dep", FeaturePackConfig.builder(ArtifactCoords.newGav("org.pm.test", "fp2", "1.0.0.Final"))
                    .build())
            .newPackage("p1", true)
                .addDependency("fp2-dep", "p2")
                .writeContent("fp1/p1.txt", "p1")
                .getFeaturePack()
            .getInstaller()
        .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp2", "1.0.0.Final"))
            .newPackage("p1", true)
                .writeContent("fp2/p1.txt", "p1")
                .getFeaturePack()
            .newPackage("p2")
                .writeContent("fp2/p2.txt", "p2")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected void testPmMethod(ProvisioningManager pm) throws ProvisioningException {
        try {
            super.testPmMethod(pm);
        } catch(ProvisioningDescriptionException e) {
            Assert.assertEquals(Errors.unsatisfiedExternalPackageDependency(
                    ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0.Final"), "p1", ArtifactCoords.newGav("org.pm.test", "fp2", "1.0.0.Final"), "p2"),
                    e.getMessage());
        }
    }

    @Override
    protected void testRecordedProvisioningConfig(final ProvisioningManager pm) throws ProvisioningException {
        assertProvisioningConfig(pm, null);
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningException {
        return ProvisioningConfig.builder()
                .addFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0.Final"))
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return null;
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.clear().build();
    }
}
