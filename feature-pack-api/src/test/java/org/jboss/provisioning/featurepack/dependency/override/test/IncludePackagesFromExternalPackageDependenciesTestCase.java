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

package org.jboss.provisioning.featurepack.dependency.override.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class IncludePackagesFromExternalPackageDependenciesTestCase extends PmProvisionConfigTestBase {

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                .addDependency(FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final"))
                        .build())
                .newPackage("p1", true)
                    .writeContent("fp1/p1.txt", "p1")
                    .getFeaturePack()
                .newPackage("p2")
                    .writeContent("fp1/p2.txt", "p2")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final"))
                .addDependency("fp3", FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "1.0.0.Final"))
                        .setInheritPackages(false)
                        .build())
                .newPackage("p1", true)
                    .addDependency("fp3", "p1")
                    .writeContent("fp2/p1.txt", "p1")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "1.0.0.Final"))
                .addDependency("fp1", FeaturePackConfig
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                        .setInheritPackages(false)
                        .build())
                .newPackage("p1", true)
                    .addDependency("fp1", "p2")
                    .writeContent("fp3/p1.txt", "p1")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningDescriptionException {
        return ProvisioningConfig.builder()
                .addFeaturePack(
                        FeaturePackConfig.forGav(
                                ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final")))
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                        .addPackage("p1")
                        .addPackage("p2")
                        .build())
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final"))
                        .addPackage("p1")
                        .build())
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "1.0.0.Final"))
                        .addPackage("p1")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/p1.txt", "p1")
                .addFile("fp1/p2.txt", "p2")
                .addFile("fp2/p1.txt", "p1")
                .addFile("fp3/p1.txt", "p1")
                .build();
    }
}
