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

package org.jboss.provisioning.config.xml;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.provisioning.feature.FeatureAnnotation;
import org.jboss.provisioning.feature.FeatureParameterSpec;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.xml.FeatureSpecXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpecXmlParsingTestCase {

    @Test
    public void testSimple() throws Exception {
        assertEquals(FeatureSpec.builder("simple").addParam(FeatureParameterSpec.create("name")).addParam(FeatureParameterSpec.create("other")).build(),
                parseFeature("simple-spec.xml"));
    }

    @Test
    public void testFull() throws Exception {
        assertEquals(FeatureSpec.builder("full")
                .addAnnotation(new FeatureAnnotation("a1").setAttr("e1", "v1").setAttr("e2", "v2"))
                .addAnnotation(new FeatureAnnotation("a2"))
                .addAnnotation(new FeatureAnnotation("a1").setAttr("e1", "v3").setAttr("e2", "v4"))
                .addRef(FeatureReferenceSpec.create("spec1", "spec1", false))
                .addRef(FeatureReferenceSpec.create("spec1-ref", "spec1", false))
                .addRef(FeatureReferenceSpec.builder("spec2")
                        .setName("spec2-ref")
                        .setNillable(true)
                        .mapParam("localParam1", "targetParam1")
                        .mapParam("localParam2", "targetParam2")
                        .build())
                .addParam(FeatureParameterSpec.create("a", false, false, null))
                .addParam(FeatureParameterSpec.create("id1", true, false, null))
                .addParam(FeatureParameterSpec.create("id2", true, false, null))
                .addParam(FeatureParameterSpec.create("b", false, false, "bb"))
                .addParam(FeatureParameterSpec.create("c", false, true, null))
                .addPackageDependency("pkg1")
                .addPackageDependency("fp-dep", "pkg2")
                .addPackageDependency("fp-dep", "pkg3")
                .addPackageDependency("pkg4")
                .build(), parseFeature("full-spec.xml"));
    }

    private static FeatureSpec parseFeature(String xml) throws Exception {
        final Path path = getResource("xml/feature/spec/" + xml);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return FeatureSpecXmlParser.getInstance().parse(reader);
        }
    }

    private static Path getResource(String path) {
        java.net.URL resUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        Assert.assertNotNull("Resource " + path + " is not on the classpath", resUrl);
        try {
            return Paths.get(resUrl.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Failed to get URI from URL", e);
        }
    }
}
