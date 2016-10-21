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

package org.jboss.provisioning.featurepack.descr.test;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConsistencyOfPackageDependenciesTestCase {

    @Test
    public void testInvalidRequiredDependency() throws Exception {

        final FeaturePackDescription.Builder builder = FeaturePackDescription
                .builder(ArtifactCoords.newGav("g", "a", "v"))
                .addDefaultPackage(
                        PackageDescription.builder("p1")
                        .addDependency("p2")
                        .build())
                .addPackage(
                        PackageDescription.builder("p2")
                        .addDependency("p3")
                        .build())
                .addPackage(
                        PackageDescription.builder("p3")
                        .addDependency("p4")
                        .build());

        try {
            builder.build();
            Assert.fail("Cannot build feature-pack description with inconsistent package dependencies.");
        } catch (ProvisioningDescriptionException e) {
            // expected
        }
    }

    @Test
    public void testInvalidOptionalDependency() throws Exception {

        final FeaturePackDescription.Builder builder = FeaturePackDescription
                .builder(ArtifactCoords.newGav("g", "a", "v"))
                .addDefaultPackage(
                        PackageDescription.builder("p1")
                        .addDependency("p2", true)
                        .build())
                .addPackage(
                        PackageDescription.builder("p2")
                        .addDependency("p3", true)
                        .build())
                .addPackage(
                        PackageDescription.builder("p3")
                        .addDependency("p4", true)
                        .build());

        try {
            builder.build();
            Assert.fail("Cannot build feature-pack description with inconsistent package dependencies.");
        } catch (ProvisioningDescriptionException e) {
            // expected
        }
    }
}
