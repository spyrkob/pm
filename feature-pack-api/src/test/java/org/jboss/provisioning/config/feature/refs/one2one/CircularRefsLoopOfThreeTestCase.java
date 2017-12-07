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

package org.jboss.provisioning.config.feature.refs.one2one;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.TestConfigHandlersProvisioningPlugin;
import org.jboss.provisioning.test.util.TestProvisionedConfigHandler;
import org.jboss.provisioning.xml.ProvisionedConfigBuilder;
import org.jboss.provisioning.xml.ProvisionedFeatureBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class CircularRefsLoopOfThreeTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");

    public static class ConfigHandler extends TestProvisionedConfigHandler {
        @Override
        protected boolean enableLogging() {
            return false;
        }
        @Override
        protected String[] initEvents() {
            return new String[] {
                    batchStartEvent(),
                    featurePackEvent(FP_GAV),
                    specEvent("specA"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a1")),
                    specEvent("specC"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c1")),
                    specEvent("specB"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b1")),
                    batchEndEvent(),
                    batchStartEvent(),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b2")),
                    specEvent("specA"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a2")),
                    specEvent("specC"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c2")),
                    batchEndEvent(),
                    batchStartEvent(),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c3")),
                    specEvent("specB"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b3")),
                    specEvent("specA"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a3")),
                    batchEndEvent()
            };
        }
    }

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.create("b"))
                    .addFeatureRef(FeatureReferenceSpec.create("specB"))
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.create("c"))
                    .addFeatureRef(FeatureReferenceSpec.create("specC"))
                    .build())
            .addSpec(FeatureSpec.builder("specC")
                    .addParam(FeatureParameterSpec.createId("c"))
                    .addParam(FeatureParameterSpec.create("a"))
                    .addFeatureRef(FeatureReferenceSpec.create("specA"))
                    .build())
            .addConfig(ConfigModel.builder()
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("a", "a1")
                            .setParam("b", "b1"))
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("b", "b1")
                            .setParam("c", "c1"))
                    .addFeature(
                            new FeatureConfig("specC")
                            .setParam("c", "c1")
                            .setParam("a", "a1"))

                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("b", "b2")
                            .setParam("c", "c2"))
                    .addFeature(
                            new FeatureConfig("specC")
                            .setParam("c", "c2")
                            .setParam("a", "a2"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("a", "a2")
                            .setParam("b", "b2"))

                    .addFeature(
                            new FeatureConfig("specC")
                            .setParam("c", "c3")
                            .setParam("a", "a3"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("a", "a3")
                            .setParam("b", "b3"))
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("b", "b3")
                            .setParam("c", "c3"))

                    .build())
            .addPlugin(TestConfigHandlersProvisioningPlugin.class)
            .addService(ProvisionedConfigHandler.class, ConfigHandler.class)
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
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a1"))
                                .setConfigParam("b", "b1")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c1"))
                                .setConfigParam("a", "a1")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b1"))
                                .setConfigParam("c", "c1")
                                .build())

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b2"))
                                .setConfigParam("c", "c2")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a2"))
                                .setConfigParam("b", "b2")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c2"))
                                .setConfigParam("a", "a2")
                                .build())

                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specC", "c", "c3"))
                                .setConfigParam("a", "a3")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "b", "b3"))
                                .setConfigParam("c", "c3")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a3"))
                                .setConfigParam("b", "b3")
                                .build())
                        .build())
                .build();
    }
}
