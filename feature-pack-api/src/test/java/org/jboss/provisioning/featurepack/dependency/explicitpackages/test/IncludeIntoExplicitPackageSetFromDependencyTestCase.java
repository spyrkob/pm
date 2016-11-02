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

package org.jboss.provisioning.featurepack.dependency.explicitpackages.test;

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
public class IncludeIntoExplicitPackageSetFromDependencyTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"), false)
                        .includePackage("p2")
                        .build())
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"))
                        .includePackage("p4")
                        .build())
                .newPackage("p1", true)
                    .addDependency("p2")
                    .writeContent("fp1/p1.txt", "p1")
                    .getFeaturePack()
                .newPackage("p2")
                    .writeContent("fp1/p2.txt", "p2")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"), false)
                        .includePackage("p2")
                        .build())
                .newPackage("p1", true)
                    .addDependency("p2", true)
                    .addDependency("p3")
                    .writeContent("fp2/p1.txt", "p1")
                    .getFeaturePack()
                .newPackage("p2")
                    .addDependency("p21")
                    .writeContent("fp2/p2.txt", "p2")
                    .getFeaturePack()
                .newPackage("p21")
                    .writeContent("fp2/p21.txt", "p21")
                    .getFeaturePack()
                .newPackage("p3")
                    .addDependency("p31")
                    .writeContent("fp2/p3.txt", "p3")
                    .getFeaturePack()
                .newPackage("p31")
                    .writeContent("fp2/p31.txt", "p31")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"))
                .newPackage("p1", true)
                    .addDependency("p2", true)
                    .addDependency("p3")
                    .writeContent("fp3/p1.txt", "p1")
                    .getFeaturePack()
                .newPackage("p2")
                    .addDependency("p21")
                    .writeContent("fp3/p2.txt", "p2")
                    .getFeaturePack()
                .newPackage("p21")
                    .writeContent("fp3/p21.txt", "p21")
                    .getFeaturePack()
                .newPackage("p3")
                    .addDependency("p31")
                    .writeContent("fp3/p3.txt", "p3")
                    .getFeaturePack()
                .newPackage("p31")
                    .writeContent("fp3/p31.txt", "p31")
                    .getFeaturePack()
                .newPackage("p4")
                    .writeContent("fp3/p4.txt", "p4")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies)
            throws ProvisioningDescriptionException {

        final Builder builder = ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder(
                                ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                                .excludePackage("p1")
                                .includePackage("p2")
                                .build());
        if(includeDependencies) {
            builder
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"), false)
                                .includePackage("p2")
                                .build())
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"), false)
                                .includePackage("p2")
                                .includePackage("p4")
                                .build());
        }

        return builder.build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/p2.txt", "p2")
                .addFile("fp2/p2.txt", "p2")
                .addFile("fp2/p21.txt", "p21")
                .addFile("fp3/p2.txt", "p2")
                .addFile("fp3/p21.txt", "p21")
                .addFile("fp3/p4.txt", "p4")
                .build();
    }
}
