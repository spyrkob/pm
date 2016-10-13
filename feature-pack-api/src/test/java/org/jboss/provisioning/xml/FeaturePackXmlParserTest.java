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

import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.ArtifactCoords;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class FeaturePackXmlParserTest  {

    private static final XmlParserValidator<FeaturePackDescription> validator = new XmlParserValidator<>(
            Paths.get("src/main/resources/schema/pm-feature-pack-1_0.xsd"), new FeaturePackXmlParser());

    @Test
    public void readBadNamespace() throws Exception {
        /*
         * urn:wildfly:pm-feature-pack:1.0.1 used in feature-pack-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0.1.xml",
                "cvc-elt.1: Cannot find the declaration of element 'feature-pack'.",
                "Message: Unexpected element '{urn:wildfly:pm-feature-pack:1.0.1}feature-pack'");
    }

    @Test
    public void readFeaturePackGroupIdMissing() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-feature-pack-groupId-missing.xml",
                "cvc-complex-type.4: Attribute 'groupId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  groupId");
    }
    @Test
    public void readFeaturePackArtifactIdMissing() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-feature-pack-artifactId-missing.xml",
                "cvc-complex-type.4: Attribute 'artifactId' must appear on element 'feature-pack'.",
                "Message: Missing required attributes  artifactId");
    }

    @Test
    public void readPackageNameMissing() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-package-name-missing.xml",
                "cvc-complex-type.4: Attribute 'name' must appear on element 'package'.",
                "Message: Missing required attributes  name");
    }

//
//    @Test
//    public void readMissingDependencyName() throws Exception {
//        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-missing-dependency-name.xml",
//                "cvc-complex-type.4: Attribute 'name' must appear on element 'dependency'.",
//                "Message: Missing required attributes  name");
//    }
//
    @Test
    public void readEmptyDependencies() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-empty-dependencies.xml",
                "cvc-complex-type.2.4.b: The content of element 'dependencies' is not complete. One of '{\"urn:wildfly:pm-feature-pack:1.0\":dependency}' is expected.",
                "There must be at least one dependency under dependencies");
    }

    @Test
    public void readEmptyPackages() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-empty-packages.xml",
                "cvc-complex-type.2.4.b: The content of element 'packages' is not complete. One of '{\"urn:wildfly:pm-feature-pack:1.0\":package}' is expected.",
                "There must be at least one package under packages");
    }

    @Test
    public void readEmptyProvisioningPlugins() throws Exception {
        validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-empty-provisioning-plugins.xml",
                "cvc-complex-type.2.4.b: The content of element 'provisioning-plugins' is not complete. One of '{\"urn:wildfly:pm-feature-pack:1.0\":artifact}' is expected.",
                "There must be at least one artifact under provisioning-plugins");
    }

    //
//    @Test
//    public void readMissingDependencies() throws Exception {
//        final PackageDescription parsedPkg = validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-missing-dependencies.xml", null, null);
//        final PackageDescription expectedPkg = PackageDescription.builder().setName("feature-pack1").build();
//        Assert.assertEquals(expectedPkg, parsedPkg);
//    }

    @Test
    public void readEmpty() throws Exception {
        FeaturePackDescription found = validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-empty.xml", null, null);
        FeaturePackDescription expected = FeaturePackDescription.builder()
                .setGav(ArtifactCoords.getGavPart("org.jboss.fp.group1", "fp1", "1.0.0")).build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readValid() throws Exception {
        FeaturePackDescription found = validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0.xml", null, null);
        FeaturePackDescription expected = FeaturePackDescription.builder()
                .setGav(ArtifactCoords.getGavPart("org.jboss.fp.group1", "fp1", "1.0.0"))
                .addDependency(ProvisionedFeaturePackDescription.builder().setGav(ArtifactCoords.getGavPart("org.jboss.dep.group1", "dep1", "0.0.1")).build())
                .addDependency(ProvisionedFeaturePackDescription.builder().setGav(ArtifactCoords.getGavPart("org.jboss.dep.group2", "dep2", "0.0.2")).build())
                .markAsDefaultPackage("package1")
                .markAsDefaultPackage("package2")
                //.addTopPackage(PackageDescription.builder().setName("package2").build())
                .addProvisioningPlugin(ArtifactCoords.getGavPart("org.jboss.plugin.group1", "plugin1", "0.1.0"))
                .addProvisioningPlugin(ArtifactCoords.getGavPart("org.jboss.plugin.group2", "plugin2", "0.2.0"))
                .build();
        Assert.assertEquals(expected, found);
    }

    @Test
    public void readVersionOptional() throws Exception {
        FeaturePackDescription found = validator.validateAndParse("src/test/resources/feature-pack/feature-pack-1.0-version-optional.xml", null, null);
        FeaturePackDescription expected = FeaturePackDescription.builder()
                .setGav(ArtifactCoords.getGavPart("org.jboss.fp.group1", "fp1", null))
                .addDependency(ProvisionedFeaturePackDescription.builder().setGav(ArtifactCoords.getGavPart("org.jboss.dep.group1", "dep1", null)).build())
                .addDependency(ProvisionedFeaturePackDescription.builder().setGav(ArtifactCoords.getGavPart("org.jboss.dep.group2", "dep2", null)).build())
                .markAsDefaultPackage("package1")
                .markAsDefaultPackage("package2")
                //.addTopPackage(PackageDescription.builder().setName("package2").build())
                .addProvisioningPlugin(ArtifactCoords.getGavPart("org.jboss.plugin.group1", "plugin1", null))
                .addProvisioningPlugin(ArtifactCoords.getGavPart("org.jboss.plugin.group2", "plugin2", null))
                .build();
        Assert.assertEquals(expected, found);
    }

}
