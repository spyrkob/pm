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

package org.jboss.provisioning.config.capability.dynamic;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.xml.ProvisionedConfigBuilder;
import org.jboss.provisioning.xml.ProvisionedFeatureBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class CircularCapabilityRequirementsTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .providesCapability("cap.$p1")
                    .requiresCapability("cap.$p2")
                    .addParam(FeatureParameterSpec.createId("p1"))
                    .addParam(FeatureParameterSpec.create("p2"))
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .providesCapability("cap.$p1")
                    .requiresCapability("cap.$p2")
                    .addParam(FeatureParameterSpec.createId("p1"))
                    .addParam(FeatureParameterSpec.create("p2"))
                    .build())
            .addConfig(ConfigModel.builder()
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("p1", "b1")
                            .setParam("p2", "a1"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("p1", "a1")
                            .setParam("p2", "b1"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("p1", "a2")
                            .setParam("p2", "b2"))
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("p1", "b2")
                            .setParam("p2", "a2"))
                    .build())
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() {
        return FeaturePackConfig.forGav(FP_GAV);
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.forGav(FP_GAV))
                .addConfig(ProvisionedConfigBuilder.builder()
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "p1", "b1")).setConfigParam("p2", "a1").build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "p1", "a1")).setConfigParam("p2", "b1").build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "p1", "a2")).setConfigParam("p2", "b2").build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "p1", "b2")).setConfigParam("p2", "a2").build())
                        .build())
                .build();
    }
}
