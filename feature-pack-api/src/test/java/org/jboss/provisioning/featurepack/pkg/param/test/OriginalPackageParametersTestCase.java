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
public class OriginalPackageParametersTestCase extends PmInstallFeaturePackTestBase {

    private final Gav fp1Gav = ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1");

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
        .newFeaturePack(fp1Gav)
            .newPackage("a", true)
                .addDependency("b")
                .addParameter("param.a", "def.a")
                .writeContent("a.txt", "a")
                .getFeaturePack()
            .newPackage("b")
                .addDependency("c")
                .addParameter("param.b", "def.b")
                .addParameter("param.bb", "def.bb")
                .writeContent("b/b.txt", "b")
                .getFeaturePack()
            .newPackage("c")
                .writeContent("c/c/c.txt", "c")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() {
        return FeaturePackConfig.forGav(fp1Gav);
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(fp1Gav)
                        .addPackage(ProvisionedPackage.builder("a")
                                .addParameter("param.a", "def.a")
                                .build())
                        .addPackage(ProvisionedPackage.builder("b")
                                .addParameter("param.b", "def.b")
                                .addParameter("param.bb", "def.bb")
                                .build())
                        .addPackage("c")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("a.txt", "a")
                .addFile("b/b.txt", "b")
                .addFile("c/c/c.txt", "c")
                .build();
    }
}
