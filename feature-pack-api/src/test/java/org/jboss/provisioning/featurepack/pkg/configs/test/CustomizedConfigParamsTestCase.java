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

package org.jboss.provisioning.featurepack.pkg.configs.test;


import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.parameters.ParameterSet;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class CustomizedConfigParamsTestCase extends PmInstallFeaturePackTestBase {

    private final Gav fp1Gav = ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1");

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(fp1Gav)
            .newPackage("a", true)
                .addParameter("p1", "one")
                .addParameter("p2", "two")
                .addParameter("p3", "three")
                .addConfig("config1")
                    .addParameter("p1", "config1")
                    .addParameter("p2", "config1")
                    .getPackage()
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() throws ProvisioningDescriptionException {
        return FeaturePackConfig.builder(fp1Gav)
                .includePackage(PackageConfig.builder("a")
                        .addConfig(ParameterSet.builder("config1")
                                .addParameter("p1", "user")
                                .build())
                        .build())
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(fp1Gav)
                        .addPackage(ProvisionedPackage.builder("a")
                                .addParameter("p1", "one")
                                .addParameter("p2", "two")
                                .addParameter("p3", "three")
                                .addConfig(ParameterSet.builder("config1")
                                        .addParameter("p1", "user")
                                        .addParameter("p2", "config1")
                                        .addParameter("p3", "three")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.build();
    }
}
