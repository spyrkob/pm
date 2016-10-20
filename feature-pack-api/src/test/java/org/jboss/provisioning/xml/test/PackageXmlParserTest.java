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
package org.jboss.provisioning.xml.test;

import java.nio.file.Paths;

import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.PackageXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class PackageXmlParserTest  {

    private static final XmlParserValidator<PackageDescription> validator = new XmlParserValidator<>(
            Paths.get("src/main/resources/schema/pm-package-1_0.xsd"), new PackageXmlParser());

    @Test
    public void readBadNamespace() throws Exception {
        /*
         * urn:wildfly:pm-provisioning:1.0.1 used in provisioning-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validator.validateAndParse("xml/package/package-1.0.1.xml",
                "cvc-elt.1: Cannot find the declaration of element 'package'.",
                "Message: Unexpected element '{urn:wildfly:pm-package:1.0.1}package'");
    }

    @Test
    public void readMissingPackageName() throws Exception {
        validator.validateAndParse("xml/package/package-1.0-missing-package-name.xml",
                "cvc-complex-type.4: Attribute 'name' must appear on element 'package'.",
                "Message: Missing required attributes  name");
    }

    @Test
    public void readMissingDependencyName() throws Exception {
        validator.validateAndParse("xml/package/package-1.0-missing-dependency-name.xml",
                "cvc-complex-type.4: Attribute 'name' must appear on element 'dependency'.",
                "Message: Missing required attributes  name");
    }

    @Test
    public void readEmptyDependencies() throws Exception {
        validator.validateAndParse("xml/package/package-1.0-empty-dependencies.xml",
                "cvc-complex-type.2.4.b: The content of element 'dependencies' is not complete. One of '{\"urn:wildfly:pm-package:1.0\":dependency}' is expected.",
                "There must be at least one dependency under dependencies");
    }

    @Test
    public void readMissingDependencies() throws Exception {
        final PackageDescription parsedPkg = validator.validateAndParse("xml/package/package-1.0-missing-dependencies.xml", null, null);
        final PackageDescription expectedPkg = PackageDescription.builder().setName("package1").build();
        Assert.assertEquals(expectedPkg, parsedPkg);
    }

    @Test
    public void readValid() throws Exception {
        PackageDescription found = validator.validateAndParse("xml/package/package-1.0.xml", null, null);
        PackageDescription expected = PackageDescription.builder()
                .setName("package1")
                .addDependency("dep1")
                .addDependency("dep2")
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readOptionalDependencies() throws Exception {
        PackageDescription found = validator.validateAndParse("xml/package/package-1.0-optional-dependencies.xml", null, null);
        PackageDescription expected = PackageDescription.builder()
                .setName("package1")
                .addDependency("dep1")
                .addDependency("dep2")
                .addDependency("dep3", true)
                .build();
        Assert.assertEquals(expected, found);
    }
}
