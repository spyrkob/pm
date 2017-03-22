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
import org.jboss.provisioning.parameters.ParameterResolver;
import org.jboss.provisioning.util.ProvisioningParameters;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningParametersTestCase {

    @Test
    public void testMain() throws Exception {

        final Gav fp1Gav = ArtifactCoords.newGav("g1", "a1", "v1");
        final Gav fp2Gav = ArtifactCoords.newGav("g2", "a2", "v2");
        final Gav fp3Gav = ArtifactCoords.newGav("g3", "a3", "v3");
        final ProvisioningParameters params = ProvisioningParameters.builder()
                .add("param1", "installation1")
                .add(fp1Gav, "param1", "param1_fp1")
                .add(fp1Gav, "pkg1", "param1", "package1")

                .add("param2", "installation2")
                .add(fp1Gav, "param2", "param2_fp1")

                .add("param3", "installation3")

                .add(fp2Gav, "param4", "param_fp2")
                .add(fp2Gav, "pkg2", "param4", "package4")

                .add(fp3Gav, "pkg2", "param5", "package5")
                .build();

        ParameterResolver resolver = params.getResolver(fp1Gav, "pkg1");
        Assert.assertEquals("package1", resolver.resolve("param1", "default"));
        Assert.assertEquals("param2_fp1", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("default", resolver.resolve("param4", "default"));

        resolver = params.getResolver(fp1Gav, "pkg2");
        Assert.assertEquals("param1_fp1", resolver.resolve("param1", "default"));
        Assert.assertEquals("param2_fp1", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("default", resolver.resolve("param4", "default"));

        resolver = params.getResolver(fp2Gav, "pkg2");
        Assert.assertEquals("installation1", resolver.resolve("param1", "default"));
        Assert.assertEquals("installation2", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("package4", resolver.resolve("param4", "default"));

        resolver = params.getResolver(fp2Gav, "pkg1");
        Assert.assertEquals("installation1", resolver.resolve("param1", "default"));
        Assert.assertEquals("installation2", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("param_fp2", resolver.resolve("param4", "default"));

        resolver = params.getResolver(fp3Gav, "pkg1");
        Assert.assertEquals("installation1", resolver.resolve("param1", "default"));
        Assert.assertEquals("installation2", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("default", resolver.resolve("param4", "default"));

        resolver = params.getResolver(fp3Gav, "pkg2");
        Assert.assertEquals("installation1", resolver.resolve("param1", "default"));
        Assert.assertEquals("installation2", resolver.resolve("param2", "default"));
        Assert.assertEquals("installation3", resolver.resolve("param3", "default"));
        Assert.assertEquals("default", resolver.resolve("param4", "default"));
        Assert.assertEquals("package5", resolver.resolve("param5", "default"));
    }
}
