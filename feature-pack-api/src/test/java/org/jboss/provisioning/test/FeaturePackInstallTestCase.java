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

package org.jboss.provisioning.test;

import java.nio.file.Paths;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.test.util.FeaturePackRepoManager;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackInstallTestCase {

    @Test
    public void testMain() throws Exception {

        FeaturePackRepoManager.newInstance(Paths.get("/home/olubyans/pm-test"))
            .installer()
            .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"))
                .addDependency(ArtifactCoords.newGav("org.pm.test", "fp-parent", "1.2.3.Final"))
                .newPackage("p1")
                    .getFeaturePack()
                .newPackage("p2-default", true)
                    .addDependency("p1")
                    .getFeaturePack()
                .getInstaller()
            .install();
    }
}
