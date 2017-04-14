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

package org.jboss.provisioning.featurepack.pkg.configs.test;


import java.nio.file.Path;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.jboss.provisioning.util.IoUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class UndefinedParametersInConfigTestCase {

    @Test
    public void setupRepo() throws Exception {
        final Path tmpDir = TestUtils.mkRandomTmpDir();
        try {
            FeaturePackRepoManager.newInstance(tmpDir).installer()
            .newFeaturePack(ArtifactCoords.newGav("org.pm.test", "fp-install", "1.0.0.Beta1"))
                .newPackage("a", true)
                    .addParameter("param.a", "def.a")
                    .addConfig("config1")
                        .addParameter("param.x", "x")
                        .getPackage()
                    .getFeaturePack()
                .getInstaller()
            .install();
            Assert.fail("param.x is not defined in the package a");
        } catch(IllegalStateException e) {
            // expected
        } finally {
            IoUtils.recursiveDelete(tmpDir);
        }
    }
}
