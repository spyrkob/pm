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

package org.jboss.provisioning.featurepack.dependency.simple.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleDependencyTestCase extends PmInstallFeaturePackTestBase {

    @Override
    protected ProvisionedFeaturePackDescription provisionedFeaturePack()
            throws ProvisioningDescriptionException {
        return ProvisionedFeaturePackDescription.forGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"));
    }

    @Override
    protected void provisionedDependencies(ProvisionedInstallationDescription.Builder builder) throws ProvisioningDescriptionException {
        builder.addFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"));
    }

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .newPackage("main", true)
                    .addDependency("d")
                    .writeContent("c", "f/p1/c.txt")
                    .getFeaturePack()
                .newPackage("d")
                    .writeContent("d", "f/p1/d.txt")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .newPackage("main", true)
                    .addDependency("b")
                    .writeContent("a", "f/p2/a.txt")
                    .getFeaturePack()
                .newPackage("b")
                    .writeContent("b", "f/p2/b.txt")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("f/p1/c.txt", "c")
                .addFile("f/p1/d.txt", "d")
                .addFile("f/p2/a.txt", "a")
                .addFile("f/p2/b.txt", "b")
                .build();
    }

}
