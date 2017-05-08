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

package org.jboss.provisioning.feature.schema;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.feature.FeatureSpecLoader;
import org.jboss.provisioning.feature.FullConfigBuilder;
import org.jboss.provisioning.xml.FeatureSpecXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SandboxTestCase {

    @Test
    public void testMain() throws Exception {

        final FeatureSpecLoader loader = new FeatureSpecLoader() {
            @Override
            public FeatureSpec load(String name) throws ProvisioningDescriptionException {
                try {
                    return featureSpec(name + "-spec.xml");
                } catch (Exception e) {
                    throw new ProvisioningDescriptionException("Failed to load schema " + name, e);
                }
            }
        };

        FullConfigBuilder.newInstance(loader)
            .addFeature(FeatureConfig.newConfig("socket-binding")
                .setParam("name", "http")
                .setParam("socket-binding-group", "standard-sockets"))
            .addFeature(FeatureConfig.newConfig("socket-binding")
                .setParam("name", "https")
                .setParam("socket-binding-group", "standard-sockets"))
            .addFeature(FeatureConfig.newConfig("socket-binding")
                .setParam("name", "http")
                .setParam("socket-binding-group", "ha-sockets"))
            .addFeature(FeatureConfig.newConfig("socket-binding")
                .setParam("name", "https")
                .setParam("socket-binding-group", "ha-sockets")
                .setParam("interface", "public"))
            .addFeature(FeatureConfig.newConfig("server-group")
                .setParam("name", "main-server-group")
                .setParam("profile", "default")
                .setParam("socket-binding-group", "standard-sockets"))
            .addFeature(FeatureConfig.newConfig("server-group")
                .setParam("name", "other-server-group")
                .setParam("profile", "ha")
                .setParam("socket-binding-group", "ha-sockets"))
            .addFeature(FeatureConfig.newConfig("interface").setParam("name", "public"))
            .addFeature(FeatureConfig.newConfig("socket-binding-group")
                .setParam("name", "standard-sockets")
                .setParam("default-interface", "public"))
            .addFeature(FeatureConfig.newConfig("socket-binding-group")
               .setParam("name", "ha-sockets")
               .setParam("default-interface", "public"))
            .addFeature(FeatureConfig.newConfig("profile").setParam("name", "default"))
            .addFeature(FeatureConfig.newConfig("profile").setParam("name", "ha"))
            .build();

/*        final ConfigSchema schema = ConfigSchema.builder()
                .addFeatureSpec(featureSpec("system-property-spec.xml"))
                .addFeatureSpec(featureSpec("profile-spec.xml"))
                .addFeatureSpec(featureSpec("interface-spec.xml"))
                .addFeatureSpec(featureSpec("socket-binding-group-spec.xml"))
                .addFeatureSpec(featureSpec("socket-binding-spec.xml"))
                .addFeatureSpec(featureSpec("server-group-spec.xml"))
                .build();

        final FullConfig.Builder configBuilder = FullConfig.builder(schema);
        addFeatures(configBuilder, "system-property");

        final FullConfig config = configBuilder
                .addFeature(FeatureConfig.newConfig("socket-binding")
                        .setParam("name", "http")
                        .setParam("socket-binding-group", "standard-sockets"))
                .addFeature(FeatureConfig.newConfig("socket-binding")
                        .setParam("name", "https")
                        .setParam("socket-binding-group", "standard-sockets"))
                .addFeature(FeatureConfig.newConfig("socket-binding")
                        .setParam("name", "http")
                        .setParam("socket-binding-group", "ha-sockets"))
                .addFeature(FeatureConfig.newConfig("socket-binding")
                        .setParam("name", "https")
                        .setParam("socket-binding-group", "ha-sockets")
                        .setParam("interface", "public"))
                .addFeature(FeatureConfig.newConfig("server-group")
                        .setParam("name", "main-server-group")
                        .setParam("profile", "default")
                        .setParam("socket-binding-group", "standard-sockets"))
                .addFeature(FeatureConfig.newConfig("server-group")
                        .setParam("name", "other-server-group")
                        .setParam("profile", "ha")
                        .setParam("socket-binding-group", "ha-sockets"))
                .addFeature(FeatureConfig.newConfig("interface").setParam("name", "public"))
                .addFeature(FeatureConfig.newConfig("socket-binding-group")
                        .setParam("name", "standard-sockets")
                        .setParam("default-interface", "public"))
                .addFeature(FeatureConfig.newConfig("socket-binding-group")
                        .setParam("name", "ha-sockets")
                        .setParam("default-interface", "public"))
                .addFeature(FeatureConfig.newConfig("profile").setParam("name", "default"))
                .addFeature(FeatureConfig.newConfig("profile").setParam("name", "ha"))
                .build();
*/    }

    private static FeatureSpec featureSpec(String xml) throws Exception {
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
