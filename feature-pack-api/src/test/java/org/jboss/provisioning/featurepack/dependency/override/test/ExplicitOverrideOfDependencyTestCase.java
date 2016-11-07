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

package org.jboss.provisioning.featurepack.dependency.override.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ResolvedFeaturePackDescription;
import org.jboss.provisioning.descr.ResolvedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.PmProvisionSpecTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExplicitOverrideOfDependencyTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .excludePackage("b")
                        .build())
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                        .excludePackage("c")
                        .build())
                .newPackage("d", true)
                    .addDependency("e")
                    .writeContent("fp1/d.txt", "d")
                    .getFeaturePack()
                .newPackage("e")
                    .writeContent("fp1/e.txt", "e")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                        .excludePackage("a")
                        .build())
                .newPackage("a", true)
                    .addDependency("b", true)
                    .addDependency("c")
                    .writeContent("fp2/a.txt", "a")
                    .getFeaturePack()
                .newPackage("b")
                    .addDependency("b1")
                    .writeContent("fp2/b.txt", "b")
                    .getFeaturePack()
                .newPackage("b1")
                    .writeContent("fp2/b1.txt", "b1")
                    .getFeaturePack()
                .newPackage("c")
                    .addDependency("c1")
                    .writeContent("fp2/c.txt", "c")
                    .getFeaturePack()
                .newPackage("c1")
                    .writeContent("fp2/c1.txt", "c1")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                .newPackage("a", true)
                    .writeContent("fp3/a.txt", "a")
                    .getFeaturePack()
                .newPackage("b", true)
                    .addDependency("b1")
                    .writeContent("fp3/b.txt", "b")
                    .getFeaturePack()
                .newPackage("b1")
                    .writeContent("fp3/b1.txt", "b1")
                    .getFeaturePack()
                .newPackage("c", true)
                    .addDependency("c1")
                    .writeContent("fp3/c.txt", "c")
                    .getFeaturePack()
                .newPackage("c1")
                    .writeContent("fp3/c1.txt", "c1")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisioningConfig() throws ProvisioningDescriptionException {

        return ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.forGav(
                                ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT")))
                .build();
    }

    @Override
    protected ResolvedInstallationDescription provisionedState() throws ProvisioningException {
        return ResolvedInstallationDescription.builder()
                .addFeaturePack(ResolvedFeaturePackDescription.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                        .addPackage("d")
                        .addPackage("e")
                        .build())
                .addFeaturePack(ResolvedFeaturePackDescription.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .addPackage("a")
                        .addPackage("c")
                        .addPackage("c1")
                        .build())
                .addFeaturePack(ResolvedFeaturePackDescription.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                        .addPackage("a")
                        .addPackage("b")
                        .addPackage("b1")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/d.txt", "d")
                .addFile("fp1/e.txt", "e")
                .addFile("fp2/a.txt", "a")
                .addFile("fp2/c.txt", "c")
                .addFile("fp2/c1.txt", "c1")
                .addFile("fp3/a.txt", "a")
                .addFile("fp3/b.txt", "b")
                .addFile("fp3/b1.txt", "b1")
                .build();
    }
}
