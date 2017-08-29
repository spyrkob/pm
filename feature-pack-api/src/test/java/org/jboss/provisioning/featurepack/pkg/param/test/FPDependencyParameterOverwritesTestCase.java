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

package org.jboss.provisioning.featurepack.pkg.param.test;


import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class FPDependencyParameterOverwritesTestCase extends PmProvisionConfigTestBase {

    private final Gav fp1Gav = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");
    private final Gav fp2Gav = ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final");
    private final Gav fp3Gav = ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(fp1Gav)
            .addDependency(fp2Gav)
            .addDependency("fp3dep", fp3Gav)
            .newPackage("fp1.a", true)
                .addDependency("fp3dep", PackageDependencySpec.builder("fp3.a")
                        .addParameter("param.fp3.a1", "param.fp3.a1.fp1.a")
                        .build())
                .getFeaturePack()
            .getInstaller()
        .newFeaturePack(fp2Gav)
            .addDependency(FeaturePackConfig.builder(fp3Gav)
                    .includePackage(PackageConfig.builder("fp3.a")
                            .addParameter("param.fp3.a1", "param.fp3.a1.fp2")
                            .addParameter("param.fp3.a2", "param.fp3.a2.fp2")
                            .build())
                    .build())
            .getInstaller()
        .newFeaturePack(fp3Gav)
            .newPackage("fp3.a", true)
                .addParameter("param.fp3.a1", "param.fp3.a1.a")
                .addParameter("param.fp3.a2", "param.fp3.a2.a")
                .addParameter("param.fp3.a3", "param.fp3.a3.a")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningDescriptionException {
        return ProvisioningConfig.builder()
                .addFeaturePack(FeaturePackConfig.builder(fp1Gav).build())
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(fp3Gav)
                        .addPackage(ProvisionedPackage.builder("fp3.a")
                                .addParameter("param.fp3.a1", "param.fp3.a1.fp1.a")
                                .addParameter("param.fp3.a2", "param.fp3.a2.fp2")
                                .addParameter("param.fp3.a3", "param.fp3.a3.a")
                                .build())
                        .build())
                //.addFeaturePack(ProvisionedFeaturePack.builder(fp2Gav)
                //        .build())
                .addFeaturePack(ProvisionedFeaturePack.builder(fp1Gav)
                        .addPackage("fp1.a")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.build();
    }
}
