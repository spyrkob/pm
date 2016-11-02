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

package org.jboss.provisioning.featurepack.dependency.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription.Builder;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.PmProvisionSpecTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependencyBranchesPackageAccumulationTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .excludePackage("b1")
                        .build())
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                        .excludePackage("c1")
                        .build())
                .newPackage("d", true)
                    .addDependency("e")
                    .writeContent("f/p1/d.txt", "d")
                    .getFeaturePack()
                .newPackage("e")
                    .writeContent("f/p1/e.txt", "e")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp4", "2.0.0.Final"), false)
                        .includePackage("d1")
                        .build())
                .newPackage("b1", true)
                    .writeContent("f/p2/b1.txt", "b1")
                    .getFeaturePack()
                .newPackage("b2", true)
                    .writeContent("f/p2/b2.txt", "b2")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp4", "2.0.0.Final"))
                        .excludePackage("d1")
                        .excludePackage("d3")
                        .build())
                .newPackage("c1", true)
                    .writeContent("f/p3/c1.txt", "c1")
                    .getFeaturePack()
                .newPackage("c2", true)
                    .writeContent("f/p3/c2.txt", "c2")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp4", "2.0.0.Final"))
                .newPackage("d1", true)
                    .writeContent("f/p4/d1.txt", "d1")
                    .getFeaturePack()
                .newPackage("d2", true)
                    .writeContent("f/p4/d2.txt", "d2")
                    .getFeaturePack()
                .newPackage("d3", true)
                    .writeContent("f/p4/d3.txt", "d3")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies)
            throws ProvisioningDescriptionException {

        final Builder builder = ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"), false)
                                .includePackage("e")
                                .build());
        if(includeDependencies) {
            builder
                .addFeaturePack(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .excludePackage("b1")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "2.0.0.Final"))
                        .excludePackage("c1")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp4", "2.0.0.Final"))
                        .excludePackage("d3")
                        .build());
        }

        return builder.build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("f/p1/e.txt", "e")
                .addFile("f/p2/b2.txt", "b2")
                .addFile("f/p3/c2.txt", "c2")
                .addFile("f/p4/d1.txt", "d1")
                .addFile("f/p4/d2.txt", "d2")
                .build();
    }
}
