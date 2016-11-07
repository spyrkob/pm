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

package org.jboss.provisioning.installation.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.descr.ResolvedFeaturePackDescription;
import org.jboss.provisioning.descr.ResolvedInstallationDescription;
import org.jboss.provisioning.test.PmProvisionSpecTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class UserChosenPackagesOverrideDependencyChosenPackagesTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"), false)
                        .includePackage("p3")
                        .build())
                .newPackage("p1", true)
                    .writeContent("fp1/p1.txt", "p1")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .newPackage("p1", true)
                    .addDependency("p2", true)
                    .writeContent("fp2/p1.txt", "p1")
                    .getFeaturePack()
                .newPackage("p2")
                    .writeContent("fp2/p2.txt", "p2")
                    .getFeaturePack()
                .newPackage("p3", true)
                    .writeContent("fp2/p3.txt", "p3")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisioningConfig()
            throws ProvisioningDescriptionException {
        return ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.forGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT")))
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                                .excludePackage("p1")
                                .build())
                .build();
    }

    @Override
    protected ResolvedInstallationDescription provisionedState() {
        return ResolvedInstallationDescription.builder()
                .addFeaturePack(ResolvedFeaturePackDescription.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                        .addPackage("p1")
                        .build())
                .addFeaturePack(ResolvedFeaturePackDescription.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .addPackage("p3")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/p1.txt", "p1")
                .addFile("fp2/p3.txt", "p3")
                .build();
    }
}
