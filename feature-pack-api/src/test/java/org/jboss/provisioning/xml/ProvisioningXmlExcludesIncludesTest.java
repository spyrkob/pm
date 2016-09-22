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
package org.jboss.provisioning.xml;

import java.nio.file.Paths;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningXmlExcludesIncludesTest {

    private static final XmlParserValidator<ProvisionedInstallationDescription> validator = new XmlParserValidator<>(
            Paths.get("src/main/resources/schema/pm-provisioning-1_0.xsd"), new ProvisioningXmlParser());

    @Test
    public void readExcludes() throws Exception {
        ProvisionedInstallationDescription found = validator
                .validateAndParse("src/test/resources/provisioning/provisioning-excludes.xml", null, null);
        ProvisionedInstallationDescription expected = ProvisionedInstallationDescription.builder()
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group1", "fp1", "0.0.1"))
                        .excludePackage("p1")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group1", "fp2", "0.0.2"))
                        .excludePackage("p2")
                        .excludePackage("p3")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group2", "fp3", "0.0.3")).build())
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readIncludes() throws Exception {
        ProvisionedInstallationDescription found = validator
                .validateAndParse("src/test/resources/provisioning/provisioning-includes.xml", null, null);
        ProvisionedInstallationDescription expected = ProvisionedInstallationDescription.builder()
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group1", "fp1", "0.0.1"))
                        .includePackage("p1")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group1", "fp2", "0.0.2"))
                        .includePackage("p2")
                        .includePackage("p3")
                        .build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder()
                        .setGav(new Gav("org.jboss.group2", "fp3", "0.0.3")).build())
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readExcludesAndIncludes() throws Exception {
        /*
         * urn:wildfly:pm-provisioning:1.0.1 used in provisioning-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validator.validateAndParse("src/test/resources/provisioning/provisioning-excludes-includes.xml",
                "cvc-complex-type.2.4.d: Invalid content was found starting with element 'excludes'. No child element is expected at this point.",
                Errors.packageExcludesIncludes());
    }

}
