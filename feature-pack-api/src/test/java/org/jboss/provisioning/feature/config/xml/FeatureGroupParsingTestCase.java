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

import org.jboss.provisioning.feature.FeatureGroupSpec;
import org.jboss.provisioning.feature.FeatureGroupConfig;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureId;
import org.jboss.provisioning.xml.FeatureGroupXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupParsingTestCase {

    @Test
    public void testMain() throws Exception {
        final FeatureGroupSpec xmlConfig = parseConfig("feature-group.xml");
        final FeatureGroupSpec expected = FeatureGroupSpec.builder("groupName")
                .addFeatureGroup(FeatureGroupConfig.builder("dep1").setInheritFeatures(true).build())
                .addFeatureGroup(FeatureGroupConfig.builder("dep2").setInheritFeatures(false).build())
                .addFeatureGroup(FeatureGroupConfig.builder("dep3")
                        .setInheritFeatures(false)
                        .includeSpec("spec1")
                        .includeFeature(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .includeFeature(
                                FeatureId.fromString("spec3:p1=v1"),
                                new FeatureConfig("spec3")
                                .addDependency(FeatureId.fromString("spec4:p1=v1,p2=v2"))
                                .addDependency(FeatureId.fromString("spec5:p1=v1,p2=v2"))
                                .setParam("p1", "v1")
                                .setParam("p2", "v2")
                                .addFeature(
                                        new FeatureConfig("spec9")
                                        .setParam("p1", "v1")
                                        .addFeature(FeatureConfig.newConfig("spec10")
                                                .addFeature(FeatureConfig.newConfig("spec11")
                                                        .setParentRef("spec10-ref")
                                                        .setParam("p1", "v1")))))
                        .excludeSpec("spec6")
                        .excludeSpec("spec7")
                        .excludeFeature(FeatureId.fromString("spec8:p1=v1"))
                        .excludeFeature(FeatureId.fromString("spec8:p1=v2"))
                        .build())
                .addFeatureGroup(FeatureGroupConfig.builder("source4", "dep4").build())
                .addFeature(
                        new FeatureConfig("spec1")
                        .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .addDependency(FeatureId.fromString("spec3:p3=v3"))
                        .setParam("p1", "v1")
                        .setParam("p2", "v2"))
                .addFeature(
                        new FeatureConfig("spec4")
                        .setParam("p1", "v1")
                        .addFeature(FeatureConfig.newConfig("spec5")
                                .addFeature(FeatureConfig.newConfig("spec6")
                                        .setParentRef("spec5-ref")
                                        .setParam("p1", "v1"))))
                .build();
        assertEquals(expected, xmlConfig);
    }

    private static FeatureGroupSpec parseConfig(String xml) throws Exception {
        final Path path = getResource("xml/config/" + xml);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return FeatureGroupXmlParser.getInstance().parse(reader);
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
