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

package org.jboss.provisioning.config.feature.group.pkg;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupDependsOnExcludedRequiredPackageTestCase extends PmInstallFeaturePackTestBase {

    private static final Gav FP_GAV = ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final");

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(FP_GAV)
            .addSpec(FeatureSpec.builder("specA")
                    .addParam(FeatureParameterSpec.createId("name"))
                    .addParam(FeatureParameterSpec.create("a", true))
                    .build())
            .addFeatureGroup(FeatureGroup.builder("fg1")
                    .addFeature(new FeatureConfig("specA")
                            .setParam("name", "afg1"))
                    .addPackageDep("fg1.pkg1")
                    .build())
            .addFeatureGroup(FeatureGroup.builder("fg2")
                    .addFeatureGroup(FeatureGroup.forGroup("fg1"))
                    .addFeature(new FeatureConfig("specA")
                            .setParam("name", "afg2"))
                    .addPackageDep("fg2.pkg1")
                    .build())
            .addConfig(ConfigModel.builder()
                    .addFeatureGroup(FeatureGroup.forGroup("fg2"))
                    .build())
            .newPackage("fg1.pkg1")
                .getFeaturePack()
            .newPackage("fg2.pkg1")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() throws ProvisioningDescriptionException {
        return FeaturePackConfig.builder(FP_GAV)
                .excludePackage("fg1.pkg1")
                .build();
    }

    @Override
    protected void pmSuccess() {
        Assert.fail();
    }

    @Override
    protected void pmFailure(Throwable e) {
        Assert.assertEquals(Errors.failedToResolveConfigSpec(null, null), e.getLocalizedMessage());
        Throwable t = e.getCause();
        Assert.assertNotNull(t);
        Assert.assertEquals(Errors.failedToProcess(FP_GAV, "fg2"), t.getLocalizedMessage());
        t = t.getCause();
        Assert.assertNotNull(t);
        Assert.assertEquals(Errors.failedToProcess(FP_GAV, "fg1"), t.getLocalizedMessage());
        t = t.getCause();
        Assert.assertNotNull(t);
        Assert.assertEquals(Errors.unsatisfiedPackageDependency(FP_GAV, "fg1.pkg1"), t.getLocalizedMessage());
    }

    @Override
    protected ProvisioningConfig provisionedConfig() {
        return null;
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return null;
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder.clear().build();
    }
}
