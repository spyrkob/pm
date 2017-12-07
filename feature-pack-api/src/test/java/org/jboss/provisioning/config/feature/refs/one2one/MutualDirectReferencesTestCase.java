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
public class MutualDirectReferencesTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");

    public static class ConfigHandler extends TestProvisionedConfigHandler {
        @Override
        protected String[] initEvents() {
            return new String[] {
                    batchStartEvent(),
                    featurePackEvent(FP_GAV),
                    specEvent("specB"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specB", "name", "b")),
                    specEvent("specA"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specA", "name", "a")),
                    batchEndEvent(),
                    batchStartEvent(),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specA", "name", "a1")),
                    specEvent("specB"),
                    featureEvent(ResolvedFeatureId.create(FP_GAV, "specB", "name", "b1")),
                    batchEndEvent()
            };
        }
    }

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("a", true))
                    .addParam(FeatureParameterSpec.create("b"))
                    .addFeatureRef(FeatureReferenceSpec.builder("specB")
                            .mapParam("b", "name")
                            .build())
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("a", true))
                    .addFeatureRef(FeatureReferenceSpec.builder("specA")
                            .setName("specA")
                            .setNillable(false)
                            .mapParam("a", "name")
                            .build())
                    .build())
            .addConfig(ConfigModel.builder()
                    .setProperty("prop1", "value1")
                    .setProperty("prop2", "value2")
                    // this is also a test for the order in which the features appear in the config
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("name", "b")
                            .setParam("a", "a"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("name", "a")
                            .setParam("b", "b"))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("name", "a1")
                            .setParam("b", "b1"))
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("name", "b1")
                            .setParam("a", "a1"))
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
                        .setProperty("prop1", "value1")
                        .setProperty("prop2", "value2")
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "name", "b"))
                                .setConfigParam("a", "a")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "name", "a"))
                                .setConfigParam("b", "b")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "name", "a1"))
                                .setConfigParam("b", "b1")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specB", "name", "b1"))
                                .setConfigParam("a", "a1")
                                .build())
                        .build())
                .build();
    }
}
