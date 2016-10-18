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
public class IncludePackageFromDependencyTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription.builder()
                        .setGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .includePackage("b")
                        .build())
                .newPackage("d", true)
                    .addDependency("e")
                    .writeContent("d", "f/p1/d.txt")
                    .getFeaturePack()
                .newPackage("e")
                    .writeContent("e", "f/p1/e.txt")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .newPackage("a", true)
                    .addDependency("b")
                    .addDependency("c")
                    .writeContent("a", "f/p2/a.txt")
                    .getFeaturePack()
                .newPackage("b")
                    .addDependency("b1")
                    .writeContent("b", "f/p2/b.txt")
                    .getFeaturePack()
                .newPackage("b1")
                    .writeContent("b1", "f/p2/b1.txt")
                    .getFeaturePack()
                .newPackage("c")
                    .addDependency("c1")
                    .writeContent("c", "f/p2/c.txt")
                    .getFeaturePack()
                .newPackage("c1")
                    .writeContent("c1", "f/p2/c1.txt")
                    .getFeaturePack()
                .newPackage("d")
                    .addDependency("d1")
                    .writeContent("d", "f/p2/d.txt")
                    .getFeaturePack()
                .newPackage("d1")
                    .writeContent("d1", "f/p2/d1.txt")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies)
            throws ProvisioningDescriptionException {

        final Builder builder = ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                                .setGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                                .includePackage("e")
                                .build());
        if(!includeDependencies) {
            builder
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                                .setGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                                .includePackage("d")
                                .build());
        } else {
            builder
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                                .setGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                                .includePackage("d")
                                .build());
        }

        return builder.build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("f/p1/e.txt", "e")
                .addFile("f/p2/d.txt", "d")
                .addFile("f/p2/d1.txt", "d1")
                .build();
    }
}
