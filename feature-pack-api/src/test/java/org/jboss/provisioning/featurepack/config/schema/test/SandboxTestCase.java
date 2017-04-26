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
import org.jboss.provisioning.xml.FeatureConfigXmlParser;
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
        .add(FeatureConfig.forName("system-property")
                .addParameter("name", "prop1")
                .addParameter("value", "val1"))
        .add(FeatureConfig.forName("access-control"))

        .add(FeatureConfig.forName("profile").addParameter("name", "default"))
        .add(featureConfig("logging-feature.xml").addParameter("profile", "default"))
        .add(featureConfig("jmx-feature.xml").addParameter("profile", "default"))

        .add(FeatureConfig.forName("profile").addParameter("name", "ha"))
        .add(featureConfig("jmx-feature.xml").addParameter("profile", "ha"))
        .add(featureConfig("logging-feature.xml").addParameter("profile", "ha"))

        .add(FeatureConfig.forName("interface").addParameter("name", "public"))
        .add(FeatureConfig.forName("socket-binding-group")
                .addParameter("name", "standard-sockets")
                .addParameter("default-interface", "public"))
        .add(FeatureConfig.forName("socket-binding-group")
                .addParameter("name", "ha-sockets")
                .addParameter("default-interface", "public"))
        .add(FeatureConfig.forName("socket-binding")
                .addParameter("name", "http")
                .addParameter("socket-binding-group", "standard-sockets"))
        .add(FeatureConfig.forName("socket-binding")
                .addParameter("name", "https")
                .addParameter("socket-binding-group", "standard-sockets"))
        .add(FeatureConfig.forName("socket-binding")
                .addParameter("name", "http")
                .addParameter("socket-binding-group", "ha-sockets"))
        .add(FeatureConfig.forName("socket-binding")
                .addParameter("name", "https")
                .addParameter("socket-binding-group", "ha-sockets"))
        .add(FeatureConfig.forName("server-group")
                .addParameter("name", "main-server-group")
                .addParameter("socket-binding-group", "standard-sockets")
                .addParameter("profile", "default"))
        .add(FeatureConfig.forName("server-group")
                .addParameter("name", "other-server-group")
                .addParameter("socket-binding-group", "ha-sockets")
                .addParameter("profile", "ha"))
        .build();
    }

    private static FeatureConfig featureConfig(String xml) throws Exception {
        final Path path = getResource("xml/feature-config/" + xml);
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
