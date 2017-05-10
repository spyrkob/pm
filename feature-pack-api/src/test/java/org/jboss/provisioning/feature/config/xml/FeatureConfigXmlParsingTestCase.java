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

package org.jboss.provisioning.feature.config.xml;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureId;
import org.jboss.provisioning.xml.FeatureConfigXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigXmlParsingTestCase {

    @Test
    public void testSimple() throws Exception {
        assertEquals(FeatureConfig.newConfig("feature-spec").setParam("param1", "value1").setParam("param2", "value2"),
                parseFeature("simple-feature.xml"));
    }

    @Test
    public void testFull() throws Exception {
        assertEquals(FeatureConfig.newConfig("feature-spec")
                .setParentRef("parent-spec")
                .addDependency(FeatureId.create("spec1", "p1", "v1"))
                .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                .setParam("param1", "value1")
                .setParam("param2", "value2")
                .addFeature(FeatureConfig.newConfig("child-spec")
                        .setParentRef("feature-spec-ref")
                        .addDependency(FeatureId.fromString("spec3:p1=v1"))
                        .setParam("param3", "value3")
                        .addFeature(FeatureConfig.newConfig("grandchild-spec")
                                .setParam("param4", "value4")))
                .addFeature(FeatureConfig.newConfig("child-spec")
                        .setParentRef("feature-spec-ref")
                        .setParam("param5", "value5"))
                .addFeature(FeatureConfig.newConfig("another-spec")
                        .setParam("param6", "value6")), parseFeature("full-feature.xml"));
    }

    private static FeatureConfig parseFeature(String xml) throws Exception {
        final Path path = getResource("xml/feature/config/" + xml);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return FeatureConfigXmlParser.getInstance().parse(reader);
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
