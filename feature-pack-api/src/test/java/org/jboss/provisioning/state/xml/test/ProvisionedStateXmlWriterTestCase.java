/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.state.xml.test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.runtime.ResolvedSpecId;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.state.ProvisionedFeature;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.util.XmlParserValidator;
import org.jboss.provisioning.xml.ProvisionedConfigBuilder;
import org.jboss.provisioning.xml.ProvisionedFeatureBuilder;
import org.jboss.provisioning.xml.ProvisionedStateXmlParser;
import org.jboss.provisioning.xml.ProvisionedStateXmlWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class ProvisionedStateXmlWriterTestCase {

    private static final String SCHEMA = "schema/pm-provisioned-state-1_0.xsd";

    private Logger log = Logger.getLogger(getClass().getName());
    private XmlParserValidator<ProvisionedState> validator;

    @Before
    public void before() throws URISyntaxException {
        URL xsd = getClass().getClassLoader().getResource(SCHEMA);
        Assert.assertNotNull(xsd);
        validator = new XmlParserValidator<>(Paths.get(xsd.toURI()), ProvisionedStateXmlParser.getInstance());
    }

    @Test
    public void testMarshallUnmarshall() throws Exception {
        ProvisionedState originalState = ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"))
                        .addPackage("package1")
                        .addPackage(ProvisionedPackage.newInstance("package2"))
                        .build())
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.group", "fp2", "1.0"))
                        .addPackage("package3")
                        .build())
                .addConfig(ProvisionedConfigBuilder.builder()
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec1", "create-param", "a"))
                                .setParam("param", "config", "resolved")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec1", "create-param", "b"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec2", "create-param", "c"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                new ResolvedSpecId(ArtifactCoords.newGav("org.jboss.group", "fp2", "1.0"), "spec3"))
                                .setIdParam("id-param", "config", "resolved")
                                .build())
                        .setModel("model")
                        .setName("name")
                        .setProperty("prop", "value")
                        .setProperty("prop2", "value2")
                        .build())
                .addConfig(ProvisionedConfigBuilder.builder()
                        .build())
                .build();

        // marshall to XML and then parse again
        Path path = marshallToTempFile(originalState);
        ProvisionedState newState = validator.validateAndParse(path);

        // compare parsed state with the original
        // feature packs
        Assert.assertEquals(originalState.getFeaturePacks().size(), newState.getFeaturePacks().size());
        Iterator<ProvisionedFeaturePack> origFeaturePackIterator = originalState.getFeaturePacks().iterator();
        Iterator<ProvisionedFeaturePack> parsedFeaturePackIterator = newState.getFeaturePacks().iterator();
        while(origFeaturePackIterator.hasNext()) {
            Assert.assertEquals(origFeaturePackIterator.next(), parsedFeaturePackIterator.next());
        }
        // configs
        Assert.assertEquals(originalState.getConfigs().size(), newState.getConfigs().size());
        for(int i = 0; i < originalState.getConfigs().size(); i++) {
            assertConfigsEqual(originalState.getConfigs().get(i), newState.getConfigs().get(i));
        }
    }

    @Test
    public void testFeatureOrder() throws Exception {
        ProvisionedState originalState = ProvisionedState.builder()
                .addConfig(ProvisionedConfigBuilder.builder()
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec1", "create-param", "a"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec2", "create-param", "b"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec1", "create-param", "c"))
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                new ResolvedSpecId(ArtifactCoords.newGav("org.jboss.group", "fp2", "1.0"), "spec3"))
                                .setIdParam("id-param", "b")
                                .build())
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                new ResolvedSpecId(ArtifactCoords.newGav("org.jboss.group", "fp2", "1.0"), "spec3"))
                                .setIdParam("id-param", "a")
                                .build()
                        )
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                ResolvedFeatureId.create(ArtifactCoords.newGav("org.jboss.group", "fp1", "1.0"),
                                        "spec1", "create-param", "c"))
                                .build())
                        .build())
                .build();

        // marshall and unmarshall
        Path path = marshallToTempFile(originalState);
        ProvisionedState newState = validator.validateAndParse(path);

        // read features and check the order
        ReadFeaturesHandler handler = new ReadFeaturesHandler();
        newState.getConfigs().iterator().next().handle(handler);
        Iterator<ProvisionedFeature> iterator = handler.features.iterator();

        Assert.assertEquals("org.jboss.group:fp1:1.0#spec1:create-param=a", iterator.next().getId().toString());
        Assert.assertEquals("org.jboss.group:fp1:1.0#spec2:create-param=b", iterator.next().getId().toString());
        Assert.assertEquals("org.jboss.group:fp1:1.0#spec1:create-param=c", iterator.next().getId().toString());
        Assert.assertEquals("org.jboss.group:fp2:1.0#spec3:id-param=b", iterator.next().getId().toString());
        Assert.assertEquals("org.jboss.group:fp2:1.0#spec3:id-param=a", iterator.next().getId().toString());
        Assert.assertEquals("org.jboss.group:fp1:1.0#spec1:create-param=c", iterator.next().getId().toString());
    }

    @Test
    public void testParams() throws Exception {
        ProvisionedState originalState = ProvisionedState.builder()
                .addConfig(ProvisionedConfigBuilder.builder()
                        .addFeature(ProvisionedFeatureBuilder.builder(
                                new ResolvedSpecId(ArtifactCoords.newGav("org.jboss.group", "fp", "1.0"), "spec"))
                                .setIdParam("id-param", "config", "resolved")
                                .setIdParam("id-param2", "config", new Object())
                                .setParam("param", "config", "resolved")
                                .build())
                        .build())
                .build();

        // marshall and unmarshall
        Path path = marshallToTempFile(originalState);
        ProvisionedState newState = validator.validateAndParse(path);

        // retrieve the parsedFeature
        ReadFeaturesHandler readFeaturesHandler = new ReadFeaturesHandler();
        newState.getConfigs().get(0).handle(readFeaturesHandler);
        ProvisionedFeature parsedFeature = readFeaturesHandler.features.get(0);

        // only checking config params here
        Assert.assertEquals("config", parsedFeature.getConfigParam("id-param"));
        Assert.assertEquals("config", parsedFeature.getConfigParam("id-param2"));
        Assert.assertEquals("config", parsedFeature.getConfigParam("param"));
    }

    @Test
    public void testEmpty() throws Exception {
        ProvisionedState originalState = ProvisionedState.builder().build();

        Path path = marshallToTempFile(originalState);

        ProvisionedState newState = validator.parse(path);
        Assert.assertEquals(originalState, newState);
    }

    private Path marshallToTempFile(ProvisionedState state) throws Exception {
        final Path path = Files.createTempFile("provisioned-state-", ".xml").toAbsolutePath();
        log.fine("Config written to " + path.toString());
        ProvisionedStateXmlWriter.getInstance().write(state, path);
        return path;
    }

    private void assertConfigsEqual(ProvisionedConfig origConfig, ProvisionedConfig newConfig) throws ProvisioningException {
        Assert.assertEquals(origConfig.getModel(), newConfig.getModel());
        Assert.assertEquals(origConfig.getName(), newConfig.getName());
        Assert.assertEquals(origConfig.getProperties(), newConfig.getProperties());

        final ReadFeaturesHandler origFeaturesHandler = new ReadFeaturesHandler();
        origConfig.handle(origFeaturesHandler);

        final ReadFeaturesHandler parsedFeaturesHandler = new ReadFeaturesHandler();
        newConfig.handle(parsedFeaturesHandler);

        Assert.assertEquals(origFeaturesHandler.features.size(), parsedFeaturesHandler.features.size());
        for(int i = 0; i < origFeaturesHandler.features.size(); i++) {
            assertFeaturesEqual(origFeaturesHandler.features.get(i), parsedFeaturesHandler.features.get(i));
        }
    }

    private void assertFeaturesEqual(ProvisionedFeature origFeature, ProvisionedFeature newFeature) throws ProvisioningException {
        Assert.assertEquals(origFeature.getId(), newFeature.getId());
        Assert.assertEquals(origFeature.getSpecId(), newFeature.getSpecId());

        // compare config values only
        for(String param : origFeature.getParamNames()) {
            Assert.assertEquals(origFeature.getConfigParam(param), newFeature.getConfigParam(param));
        }
    }


    /**
     * Simple handler that extracts list of {@link ProvisionedFeature}s from a {@link ProvisionedConfig}.
     */
    private static class ReadFeaturesHandler implements ProvisionedConfigHandler {

        private List<ProvisionedFeature> features = new ArrayList<>();

        @Override
        public void nextFeature(ProvisionedFeature feature) {
            features.add(feature);
        }

    }
}
