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

package org.jboss.provisioning.config.feature.group;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
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
public class MultipleInclusionOfFeatureGroupConfigTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.create("prop", true))
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .addFeatureRef(FeatureReferenceSpec.create("specA"))
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.create("prop", true))
                    .build())
            .addSpec(FeatureSpec.builder("specC")
                    .addFeatureRef(FeatureReferenceSpec.create("specA"))
                    .addFeatureRef(FeatureReferenceSpec.create("specB"))
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.createId("c"))
                    .addParam(FeatureParameterSpec.create("prop", true))
                    .build())
            .addFeatureGroup(FeatureGroup.builder("fgC")
                    .addFeature(
                            new FeatureConfig("specC")
                            .setParam("c", "c1")
                            .setParam("prop", "fgC"))
                    .addFeature(
                            new FeatureConfig("specC")
                            .setParam("c", "c2")
                            .setParam("prop", "fgC"))
                    .build())
            .addFeatureGroup(FeatureGroup.builder("fgB")
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("b", "b1")
                            .setParam("prop", "fgB")
                            .addFeatureGroup(FeatureGroup.builder("fgC")
                                    .setInheritFeatures(true)
                                    .includeFeature(FeatureId.create("specC", "c", "c1"), new FeatureConfig().setParam("prop", "fgB"))
                                    .build()))
                    .addFeature(
                            new FeatureConfig("specB")
                            .setParam("b", "b2")
                            .setParam("prop", "fgB")
                            .addFeatureGroup(FeatureGroup.builder("fgC")
                                    .setInheritFeatures(true)
                                    .includeFeature(FeatureId.create("specC", "c", "c1"), new FeatureConfig().setParam("prop", "fgB"))
                                    .build()))
                    .build())
            .addFeatureGroup(FeatureGroup.builder("fgA")
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("a", "a1")
                            .setParam("prop", "fgA")
                            .addFeatureGroup(FeatureGroup.builder("fgB")
                                    .setInheritFeatures(true)
                                    .includeFeature(FeatureId.create("specB", "b", "b1"), new FeatureConfig().setParam("prop", "fgA"))
                                    .includeFeature(FeatureId.builder("specC").setParam("b", "b1").setParam("c", "c1").build(), new FeatureConfig().setParam("prop", "fgA"))
                                    .build()))
                    .addFeature(
                            new FeatureConfig("specA")
                            .setParam("a", "a2")
                            .setParam("prop", "fgA")
                            .addFeatureGroup(FeatureGroup.builder("fgB")
                                    .setInheritFeatures(true)
                                    .includeFeature(FeatureId.create("specB", "b", "b1"), new FeatureConfig().setParam("prop", "fgA"))
                                    .includeFeature(FeatureId.builder("specC").setParam("b", "b1").setParam("c", "c1").build(), new FeatureConfig().setParam("prop", "fgA"))
                                    .build()))
                    .build())
            .addConfig(ConfigModel.builder()
                    .addFeatureGroup(FeatureGroup.forGroup("fgA"))
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
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a1"))
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP_GAV, "specA", "a", "a2"))
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP_GAV, "specB").setParam("a", "a1").setParam("b", "b1").build())
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP_GAV, "specB").setParam("a", "a1").setParam("b", "b2").build())
                                .setConfigParam("prop", "fgB")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP_GAV, "specB").setParam("a", "a2").setParam("b", "b1").build())
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP_GAV, "specB").setParam("a", "a2").setParam("b", "b2").build())
                                .setConfigParam("prop", "fgB")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a1").setParam("b", "b1").setParam("c", "c1").build())
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a1").setParam("b", "b1").setParam("c", "c2").build())
                                .setConfigParam("prop", "fgC")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a1").setParam("b", "b2").setParam("c", "c1").build())
                                .setConfigParam("prop", "fgB")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a1").setParam("b", "b2").setParam("c", "c2").build())
                                .setConfigParam("prop", "fgC")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a2").setParam("b", "b1").setParam("c", "c1").build())
                                .setConfigParam("prop", "fgA")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a2").setParam("b", "b1").setParam("c", "c2").build())
                                .setConfigParam("prop", "fgC")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a2").setParam("b", "b2").setParam("c", "c1").build())
                                .setConfigParam("prop", "fgB")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.builder(FP_GAV, "specC").setParam("a", "a2").setParam("b", "b2").setParam("c", "c2").build())
                                .setConfigParam("prop", "fgC")
                                .build())

                        .build())
                .build();
    }
}
