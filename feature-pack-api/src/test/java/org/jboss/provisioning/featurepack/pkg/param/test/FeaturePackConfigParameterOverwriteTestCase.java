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
import org.jboss.provisioning.test.ProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackConfigParameterOverwriteTestCase extends ProvisionConfigTestBase {

    private final Gav fp1Gav = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");
    private final Gav fp2Gav = ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(fp1Gav)
            .addDependency("fp2dep", FeaturePackConfig.builder(fp2Gav)
                    .build())
            .newPackage("fp1.a", true)
                .addDependency("fp1.b", true)
                .addDependency("fp1.c", true)
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.a")
                        .addParameter("param.fp2.a1", "param.fp2.a1.fp1.a1")
                        .build())
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.b", true)
                        .addParameter("param.fp2.b1", "param.fp2.b1.fp1.a1")
                        .build())
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.c")
                        .addParameter("param.fp2.c1", "param.fp2.c1.fp1.a1")
                        .build())
                .getFeaturePack()
            .newPackage("fp1.b")
                .addDependency("fp1.c")
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.b")
                        .addParameter("param.fp2.b1", "param.fp2.b1.fp1.b1")
                        .addParameter("param.fp2.b2", "param.fp2.b2.fp1.b2")
                        .build())
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.c")
                        .addParameter("param.fp2.c1", "param.fp2.c1.fp1.b1")
                        .addParameter("param.fp2.c2", "param.fp2.c2.fp1.b2")
                        .build())
                .getFeaturePack()
            .newPackage("fp1.c")
                .addDependency("fp2dep", PackageDependencySpec.builder("fp2.c")
                        .addParameter("param.fp2.c1", "param.fp2.c1.fp1.c1")
                        .addParameter("param.fp2.c2", "param.fp2.c2.fp1.c2")
                        .addParameter("param.fp2.c3", "param.fp2.c3.fp1.c3")
                        .build())
                .getFeaturePack()
            .getInstaller()
        .newFeaturePack(fp2Gav)
            .newPackage("fp2.a", true)
                .addParameter("param.fp2.a1", "param.fp2.a1.a")
                .addParameter("param.fp2.a2", "param.fp2.a2.a")
                .getFeaturePack()
            .newPackage("fp2.b")
                .addParameter("param.fp2.b1", "param.fp2.b1.b")
                .addParameter("param.fp2.b2", "param.fp2.b2.b")
                .addParameter("param.fp2.b3", "param.fp2.b3.b")
                .getFeaturePack()
            .newPackage("fp2.c")
                .addParameter("param.fp2.c1", "param.fp2.c1.c")
                .addParameter("param.fp2.c2", "param.fp2.c2.c")
                .addParameter("param.fp2.c3", "param.fp2.c3.c")
                .addParameter("param.fp2.c4", "param.fp2.c4.c")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningDescriptionException {
        return ProvisioningConfig.builder()
                .addFeaturePack(FeaturePackConfig.builder(fp1Gav)
                        .excludePackage("fp1.b")
                        .build())
                .addFeaturePack(FeaturePackConfig.builder(fp2Gav, false)
                        .includePackage(PackageConfig.builder("fp2.a")
                                .addParameter("param.fp2.a1", "param.fp2.a1.user")
                                .build())
                        .excludePackage("fp2.b")
                        .includePackage(PackageConfig.builder("fp2.c")
                                .addParameter("param.fp2.c3", "param.fp2.c3.user")
                                .build())
                        .build())
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(fp2Gav)
                        .addPackage(ProvisionedPackage.builder("fp2.a")
                                .addParameter("param.fp2.a1", "param.fp2.a1.user")
                                .addParameter("param.fp2.a2", "param.fp2.a2.a")
                                .build())
                        .addPackage(ProvisionedPackage.builder("fp2.c")
                                .addParameter("param.fp2.c1", "param.fp2.c1.fp1.a1")
                                .addParameter("param.fp2.c2", "param.fp2.c2.fp1.c2")
                                .addParameter("param.fp2.c3", "param.fp2.c3.user")
                                .addParameter("param.fp2.c4", "param.fp2.c4.c")
                                .build())
                        .build())
                .addFeaturePack(ProvisionedFeaturePack.builder(fp1Gav)
                        .addPackage("fp1.c")
                        .addPackage("fp1.a")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.build();
    }
}
