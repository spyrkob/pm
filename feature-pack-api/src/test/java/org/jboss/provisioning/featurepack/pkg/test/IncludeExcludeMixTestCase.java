/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.featurepack.pkg.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmInstallFeaturePackTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class IncludeExcludeMixTestCase extends PmInstallFeaturePackTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) {
        repoManager.installer()
        .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0"))
            .newPackage("p1", true)
                .writeContent("p1.txt", "p1")
                .getFeaturePack()
            .newPackage("p2", true)
                .addDependency("p21")
                .writeContent("p2.txt", "p2")
                .getFeaturePack()
            .newPackage("p21")
                .writeContent("p21.txt", "p21")
                .getFeaturePack()
            .newPackage("p3")
                .addDependency("p31", true)
                .writeContent("p3.txt", "p3")
                .getFeaturePack()
            .newPackage("p31")
                .writeContent("p31.txt", "p31")
                .getFeaturePack()
            .getInstaller()
        .install();
    }

    @Override
    protected FeaturePackConfig featurePackConfig() throws ProvisioningDescriptionException {
        return FeaturePackConfig
                .builder(ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0"))
                .excludePackage("p1")
                .excludePackage("p2")
                .includePackage("p21")
                .includePackage("p3")
                .excludePackage("p31")
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() throws ProvisioningException {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.pm.test", "fp1", "1.0.0"))
                        .addPackage("p21")
                        .addPackage("p3")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("p21.txt", "p21")
                .addFile("p3.txt", "p3")
                .build();
    }
}
