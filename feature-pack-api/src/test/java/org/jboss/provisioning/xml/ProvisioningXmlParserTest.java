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

import java.io.IOException;
import java.nio.file.Paths;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ProvisioningXmlParserTest  {

    private static final XmlParserValidator validator = new XmlParserValidator(Paths.get("src/main/resources/schema/pm-provisioning-1_0.xsd"));


    @Test
    public void readBadNamespace() throws IOException {
        /*
         * urn:wildfly:pm-provisioning:1.0.1 used in provisioning-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validator.validateAndParse("src/test/resources/provisioning/provisioning-1.0.1.xml",
                "cvc-elt.1: Cannot find the declaration of element 'installation'.",
                "Message: Unexpected element '{urn:wildfly:pm-provisioning:1.0.1}installation'");
    }

    @Test
    public void readMissingGroupId() throws XMLStreamException, IOException {
        validator.validateAndParse("src/test/resources/provisioning/provisioning-1.0-missing-groupId.xml",
                "cvc-complex-type.4: Attribute 'groupId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  groupId");
    }

    @Test
    public void readMissingArtifactId() throws XMLStreamException, IOException {
        validator.validateAndParse("src/test/resources/provisioning/provisioning-1.0-missing-artifactId.xml",
                "cvc-complex-type.4: Attribute 'artifactId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  artifactId");
    }

    @Test
    public void readNoFp() throws IOException {
        validator.validateAndParse("src/test/resources/provisioning/provisioning-1.0-no-fp.xml",
                "cvc-complex-type.2.4.b: The content of element 'installation' is not complete. One of '{\"urn:wildfly:pm-provisioning:1.0\":feature-pack}' is expected.",
                "There must be at least one feature-pack under installation");
    }

    @Test
    public void readValid() throws IOException {
        ProvisionedInstallationDescription found = validator.validateAndParse("src/test/resources/provisioning/provisioning-1.0.xml", null, null);
        ProvisionedInstallationDescription expected = ProvisionedInstallationDescription.builder()
                .addFeaturePack(ProvisionedFeaturePackDescription.builder().setGAV(new GAV("org.jboss.group1", "fp1", "0.0.1")).build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder().setGAV(new GAV("org.jboss.group1", "fp2", "0.0.2")).build())
                .addFeaturePack(ProvisionedFeaturePackDescription.builder().setGAV(new GAV("org.jboss.group2", "fp3", "0.0.3")).build())
                .build();
        Assert.assertEquals(expected, found);
    }

}
