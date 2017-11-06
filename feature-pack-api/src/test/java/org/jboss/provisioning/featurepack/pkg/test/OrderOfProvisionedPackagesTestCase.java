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

package org.jboss.provisioning.featurepack.pkg.test;

import java.util.Iterator;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class OrderOfProvisionedPackagesTestCase extends PmInstallFeaturePackTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"))
            .newPackage("a")
                .writeContent("a.txt", "a")
                .addDependency("e")
                .getFeaturePack()
            .newPackage("b")
                .writeContent("b.txt", "b")
                .addDependency("a")
                .getFeaturePack()
            .newPackage("c", true)
                .writeContent("c.txt", "c")
                .addDependency("d")
                .getFeaturePack()
            .newPackage("d")
                .writeContent("d.txt", "d")
                .addDependency("b")
                .getFeaturePack()
            .newPackage("e")
                .writeContent("e.txt", "e")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() {
        return FeaturePackConfig.forGav(ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"));
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"))
                        .addPackage("a")
                        .addPackage("b")
                        .addPackage("c")
                        .addPackage("d")
                        .addPackage("e")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("a.txt", "a")
                .addFile("b.txt", "b")
                .addFile("c.txt", "c")
                .addFile("d.txt", "d")
                .addFile("e.txt", "e")
                .build();
    }

    @Override
    protected void testPm(ProvisioningManager pm) throws ProvisioningException {
        super.testPm(pm);
        final ProvisionedState state = pm.getProvisionedState();
        final Iterator<String> packageNames = state.getFeaturePack(
                ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"))
                .getPackageNames().iterator();
        Assert.assertTrue(packageNames.hasNext());
        Assert.assertEquals("e", packageNames.next());
        Assert.assertTrue(packageNames.hasNext());
        Assert.assertEquals("a", packageNames.next());
        Assert.assertTrue(packageNames.hasNext());
        Assert.assertEquals("b", packageNames.next());
        Assert.assertTrue(packageNames.hasNext());
        Assert.assertEquals("d", packageNames.next());
        Assert.assertTrue(packageNames.hasNext());
        Assert.assertEquals("c", packageNames.next());
        Assert.assertFalse(packageNames.hasNext());
    }
}
