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

package org.jboss.provisioning.featurepack.config.schema.test;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.provisioning.config.schema.Config;
import org.jboss.provisioning.config.schema.ConfigSchema;
import org.jboss.provisioning.config.schema.FeatureConfig;
import org.jboss.provisioning.xml.FeaturePackSchemaXmlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SandboxTestCase {

    @Test
    public void testMain() throws Exception {

        final ConfigSchema schema;
        try(BufferedReader reader = Files.newBufferedReader(getResource("xml/feature-config/config-schema.xml"))) {
            schema = FeaturePackSchemaXmlParser.getInstance().parse(reader);
        }

        Config.builder(schema)
        .add(FeatureConfig.builder("system-property")
                .addParameter("name", "prop1")
                .addParameter("value", "val1")
                .build())
        .add(FeatureConfig.forName("access-control"))
        .add(FeatureConfig.builder("profile")
                .addParameter("name", "default")
                .build())
        .add(FeatureConfig.builder("profile")
                .addParameter("name", "ha")
                .build())
        .add(FeatureConfig.builder("interface")
                .addParameter("name", "public")
                .build())
        .add(FeatureConfig.builder("socket-binding-group")
                .addParameter("name", "standard-sockets")
                .addParameter("default-interface", "public")
                .build())
        .add(FeatureConfig.builder("socket-binding-group")
                .addParameter("name", "ha-sockets")
                .addParameter("default-interface", "public")
                .build())
        .add(FeatureConfig.builder("socket-binding")
                .addParameter("name", "http")
                .addParameter("socket-binding-group", "standard-sockets")
                .build())
        .add(FeatureConfig.builder("socket-binding")
                .addParameter("name", "https")
                .addParameter("socket-binding-group", "standard-sockets")
                .build())
        .add(FeatureConfig.builder("socket-binding")
                .addParameter("name", "http")
                .addParameter("socket-binding-group", "ha-sockets")
                .build())
        .add(FeatureConfig.builder("socket-binding")
                .addParameter("name", "https")
                .addParameter("socket-binding-group", "ha-sockets")
                .build())
        .add(FeatureConfig.builder("server-group")
                .addParameter("name", "main-server-group")
                .addParameter("socket-binding-group", "standard-sockets")
                .addParameter("profile", "default")
                .build())
        .add(FeatureConfig.builder("server-group")
                .addParameter("name", "other-server-group")
                .addParameter("socket-binding-group", "ha-sockets")
                .addParameter("profile", "ha")
                .build())
        .build();
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
