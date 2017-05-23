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
package org.jboss.provisioning.xml.test;

import java.nio.file.Paths;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.feature.FeatureGroup;
import org.jboss.provisioning.feature.FeatureGroupConfig;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureId;
import org.jboss.provisioning.test.util.XmlParserValidator;
import org.jboss.provisioning.xml.ProvisioningXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningWithConfigTestCase {

    private static final XmlParserValidator<ProvisioningConfig> validator = new XmlParserValidator<>(
            Paths.get("src/main/resources/schema/pm-provisioning-1_0.xsd"), ProvisioningXmlParser.getInstance());

    @Test
    public void testMain() throws Exception {
        ProvisioningConfig found = validator
                .validateAndParse("xml/provisioning/provisioning-config.xml", null, null);
        ProvisioningConfig expected = ProvisioningConfig.builder()
                .addFeaturePack(FeaturePackConfig.builder(ArtifactCoords.newGav("org.jboss.group1", "fp1", "0.0.1"))
                        .setFeatureGroup(FeatureGroup.builder()
                                .addDependency(FeatureGroupConfig.builder("dep1").setInheritFeatures(true).build())
                                .addDependency(FeatureGroupConfig.builder("dep2").setInheritFeatures(false).build())
                                .addDependency(FeatureGroupConfig.builder("dep3")
                                        .setInheritFeatures(false)
                                        .includeSpec("spec1")
                                        .includeFeature(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                                        .includeFeature(FeatureId.fromString("spec3:p1=v1"),
                                                new FeatureConfig("spec3").addDependency(FeatureId.fromString("spec4:p1=v1,p2=v2"))
                                                .addDependency(FeatureId.fromString("spec5:p1=v1,p2=v2"))
                                                .setParam("p1", "v1")
                                                .setParam("p2", "v2"))
                                        .excludeSpec("spec6")
                                        .excludeSpec("spec7")
                                        .excludeFeature(FeatureId.fromString("spec8:p1=v1"))
                                        .excludeFeature(FeatureId.fromString("spec8:p1=v2"))
                                        .build())
                                .addDependency(FeatureGroupConfig.builder("source4", "dep4").build())
                                .addFeature(new FeatureConfig("spec1")
                                        .setParam("p1", "v1")
                                        .setParam("p2", "v2"))
                                .addFeature(new FeatureConfig("spec1")
                                        .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                                        .addDependency(FeatureId.fromString("spec3:p3=v3"))
                                        .setParam("p1", "v3")
                                        .setParam("p2", "v4"))
                                .build())
                        .build())
                .build();
        Assert.assertEquals(expected, found);
    }
}
