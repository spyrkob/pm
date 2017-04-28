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

import org.jboss.provisioning.feature.ConfigSchema;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureParameterSpec;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.feature.FullConfig;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SandboxTestCase {

    @Test
    public void testMain() throws Exception {

        final ConfigSchema schema = ConfigSchema.builder()
                .addFeatureSpec(FeatureSpec.builder("system-property")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .addParam(FeatureParameterSpec.create("value", false))
                        .build())
                .addFeatureSpec(FeatureSpec.builder("profile")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .build())
                .addFeatureSpec(FeatureSpec.builder("interface")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .addParam(FeatureParameterSpec.create("inet-address"))
                        .build())
                .addFeatureSpec(FeatureSpec.builder("socket-binding-group")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .addParam(FeatureParameterSpec.create("default-interface", false))
                        .addRef(FeatureReferenceSpec.builder("interface")
                                .mapParam("default-interface", "name")
                                .build())
                        .build())
                .addFeatureSpec(FeatureSpec.builder("socket-binding")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .addParam(FeatureParameterSpec.createId("socket-binding-group"))
                        .addParam(FeatureParameterSpec.create("interface", true))
                        .addRef(FeatureReferenceSpec.create("socket-binding-group"))
                        .addRef(FeatureReferenceSpec.create("interface", true))
                        .build())
                .addFeatureSpec(FeatureSpec.builder("server-group")
                        .addParam(FeatureParameterSpec.createId("name"))
                        .addParam(FeatureParameterSpec.createId("profile"))
                        .addParam(FeatureParameterSpec.createId("socket-binding-group"))
                        .addRef(FeatureReferenceSpec.create("profile"))
                        .addRef(FeatureReferenceSpec.create("socket-binding-group"))
                        .build())
                .build();

        final FullConfig config = FullConfig.builder(schema)
                .addFeature(FeatureConfig.newConfig("system-property")
                        .setParam("name", "prop1")
                        .setParam("value", "value1"))
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
                .addFeature(FeatureConfig.newConfig("system-property")
                        .setParam("name", "prop2")
                        .setParam("value", "value2"))
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
    }
}
