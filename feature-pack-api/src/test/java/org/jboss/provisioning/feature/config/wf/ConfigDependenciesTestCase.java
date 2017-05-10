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

package org.jboss.provisioning.feature.config.wf;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.provisioning.feature.MainConfigBuilder;
import org.jboss.provisioning.util.DefaultConfigLoader;
import org.jboss.provisioning.util.DefaultFeatureConfigLoader;
import org.jboss.provisioning.util.DefaultFeatureSpecLoader;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigDependenciesTestCase {

    @Test
    public void testMain() throws Exception {

        final Path baseDir = getResource("xml/config/wf");
        MainConfigBuilder.newInstance(
                new DefaultFeatureSpecLoader(baseDir),
                new DefaultConfigLoader(baseDir),
                DefaultFeatureConfigLoader.newInstance(baseDir))
                .addConfig("full_domain")
                .build();

/*        final Config expected = Config.builder("configName")
                .addDependency(ConfigDependency.builder("dep1").build())
                .addDependency(ConfigDependency.builder("dep2").setInheritFeatures(false).build())
                .addDependency(ConfigDependency.builder("dep3")
                        .setInheritFeatures(false)
                        .includeSpec("spec1")
                        .includeFeature(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .includeFeature(
                                FeatureId.fromString("spec3:p1=v1"),
                                new FeatureConfig()
                                .addDependency(FeatureId.fromString("spec4:p1=v1,p2=v2"))
                                .addDependency(FeatureId.fromString("spec5:p1=v1,p2=v2"))
                                .setParam("p1", "v1")
                                .setParam("p2", "v2"))
                        .excludeSpec("spec6")
                        .excludeSpec("spec7")
                        .excludeFeature(FeatureId.fromString("spec8:p1=v1"))
                        .excludeFeature(FeatureId.fromString("spec8:p1=v2"))
                        .build())
                .addFeature(
                        new FeatureConfig("spec1")
                        .addDependency(FeatureId.fromString("spec2:p1=v1,p2=v2"))
                        .addDependency(FeatureId.fromString("spec3:p3=v3"))
                        .setParam("p1", "v1")
                        .setParam("p2", "v2"))
                .addFeature(
                        new FeatureConfig("spec4")
                        .setParam("p1", "v1"))
                .build();
        assertEquals(expected, xmlConfig);
*/    }

    @Test
    public void testNested() throws Exception {

        final Path baseDir = getResource("xml/config/wf");
        MainConfigBuilder.newInstance(
                new DefaultFeatureSpecLoader(baseDir),
                new DefaultConfigLoader(baseDir),
                DefaultFeatureConfigLoader.newInstance(baseDir))
                .addConfig("full_domain_nested")
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
