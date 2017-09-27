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

package org.jboss.provisioning.config.this_ref;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeatureGroupSpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.jboss.provisioning.xml.ProvisionedConfigBuilder;
import org.jboss.provisioning.xml.ProvisionedFeatureBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class ThisAsFeatureSpecOriginTestCase extends PmProvisionConfigTestBase {

    private static final Gav FP1_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");
    private static final Gav FP2_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP1_GAV)
            .addDependency("fp2", FP2_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.create("p1", true))
                    .build())
            .addSpec(FeatureSpec.builder("specB")
                    .addRef(FeatureReferenceSpec.builder("specD").setDependency("fp2").build())
                    .addParam(FeatureParameterSpec.createId("b"))
                    .addParam(FeatureParameterSpec.createId("d"))
                    .addParam(FeatureParameterSpec.create("p1", false))
                    .build())
            .addFeatureGroup(FeatureGroupSpec.builder("fg1")
                    .addFeature("fp2", new FeatureConfig("specD")
                            .setParam("d", "dOne")
                            .addFeature("this", new FeatureConfig("specB")
                                    .setParam("b", "bOne")))
                    .build()).getInstaller()
        .newFeaturePack(FP2_GAV)
            .addDependency("fp1", FP1_GAV)
            .addSpec(FeatureSpec.builder("specC")
                    .addRef(FeatureReferenceSpec.builder("specA").setDependency("fp1").build())
                    .addParam(FeatureParameterSpec.createId("a"))
                    .addParam(FeatureParameterSpec.createId("c"))
                    .addParam(FeatureParameterSpec.create("p1", true))
                    .build())
            .addSpec(FeatureSpec.builder("specD")
                    .addParam(FeatureParameterSpec.createId("d"))
                    .addParam(FeatureParameterSpec.create("p1", false))
                    .build())
            .addFeatureGroup(FeatureGroupSpec.builder("fg2")
                    .addFeature("fp1", new FeatureConfig("specA")
                            .setParam("a", "aOne")
                            .addFeature("this", new FeatureConfig("specC")
                                    .setParam("c", "cOne")))
                    .build())
            .addConfig(ConfigSpec.builder()
                    .setProperty("prop1", "value1")
                    .setProperty("prop2", "value2")
                    .addFeatureGroup("fp1", FeatureGroupConfig.forGroup("fg1"))
                    .addFeatureGroup(FeatureGroupConfig.forGroup("fg2"))
                    .build())
            .getInstaller()
        .install();
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningException {
        return ProvisioningConfig.builder()
                .addFeaturePack(FP2_GAV)
                .build();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.test.PmMethodTestBase#provisionedState()
     */
    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(FP2_GAV).build())
                .addConfig(ProvisionedConfigBuilder.builder()
                        .setProperty("prop1", "value1")
                        .setProperty("prop2", "value2")
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP2_GAV, "specD").setParam("d", "dOne").build())
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP1_GAV, "specB")
                                .setParam("d", "dOne").setParam("b", "bOne").build())
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.create(FP1_GAV, "specA", "a", "aOne"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(ResolvedFeatureId.builder(FP2_GAV, "specC")
                                .setParam("a", "aOne").setParam("c", "cOne").build())
                                .build())
                        .build())
                .build();
    }
}
