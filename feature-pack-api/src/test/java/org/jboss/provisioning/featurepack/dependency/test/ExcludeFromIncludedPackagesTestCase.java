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

package org.jboss.provisioning.featurepack.dependency.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription.Builder;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.PmProvisionSpecTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.junit.Ignore;

/**
 *
 * @author Alexey Loubyansky
 */
@Ignore
public class ExcludeFromIncludedPackagesTestCase extends PmProvisionSpecTestBase {

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"), false)
                        .includePackage("b")
                        .includePackage("c")
                        .build())
                .newPackage("d", true)
                    .addDependency("e")
                    .writeContent("f/p1/d.txt", "d")
                    .getFeaturePack()
                .newPackage("e")
                    .writeContent("f/p1/e.txt", "e")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                .addDependency(ProvisionedFeaturePackDescription
                        .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"), false)
                        .includePackage("d1")
                        .includePackage("d2")
                        .build())
                .newPackage("a", true)
                    .addDependency("b")
                    .addDependency("c")
                    .writeContent("f/p2/a.txt", "a")
                    .getFeaturePack()
                .newPackage("b")
                    .addDependency("b1")
                    .writeContent("f/p2/b.txt", "b")
                    .getFeaturePack()
                .newPackage("b1")
                    .writeContent("f/p2/b1.txt", "b1")
                    .getFeaturePack()
                .newPackage("c")
                    .addDependency("c1")
                    .writeContent("f/p2/c.txt", "c")
                    .getFeaturePack()
                .newPackage("c1")
                    .writeContent("f/p2/c1.txt", "c1")
                    .getFeaturePack()
                .getInstaller()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"))
                .newPackage("d", true)
                    .addDependency("d1")
                    .addDependency("d2")
                    .writeContent("f/p3/d.txt", "d")
                    .getFeaturePack()
                .newPackage("d1")
                    .writeContent("f/p3/d1.txt", "d1")
                    .getFeaturePack()
                .newPackage("d2")
                    .writeContent("f/p3/d2.txt", "d2")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisionedInstallationDescription provisionedInstallation(boolean includeDependencies)
            throws ProvisioningDescriptionException {

        final Builder builder = ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Alpha-SNAPSHOT"))
                                .excludePackage("e")
                                .build());
        if(!includeDependencies) {
            builder
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                                .excludePackage("b").build());
/*                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"))
                                .excludePackage("d1")
                                .build());*/
        } else {
            builder
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                                .excludePackage("b")
                                .build())
                .addFeaturePack(
                        ProvisionedFeaturePackDescription
                                .builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp3", "3.0.0.Final"))
                                .includePackage("d1")
                                .includePackage("d2")
                                .build());
        }

        return builder.build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("f/p1/d.txt", "d")
                .addFile("f/p2/b.txt", "b") //x
                .addFile("f/p2/b1.txt", "b1")//x
                .addFile("f/p2/c.txt", "c")
                .addFile("f/p2/c1.txt", "c1")
                .addFile("f/p3/d1.txt", "d1")//x
                .addFile("f/p3/d2.txt", "d2")
                .build();
    }
}
