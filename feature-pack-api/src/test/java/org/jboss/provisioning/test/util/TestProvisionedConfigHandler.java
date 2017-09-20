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

package org.jboss.provisioning.test.util;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.runtime.ResolvedFeatureSpec;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.state.ProvisionedFeature;
import org.junit.Assert;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class TestProvisionedConfigHandler implements ProvisionedConfigHandler {

    private static final String BATCH_START = "START BATCH";
    private static final String BATCH_END = "END BATCH";

    protected static String batchStartEvent() {
        return BATCH_START;
    }

    protected static String batchEndEvent() {
        return BATCH_END;
    }

    protected static String featurePackEvent(ArtifactCoords.Gav fpGav) {
        return "feature-pack " + fpGav;
    }

    protected static String specEvent(String spec) {
        return " spec " + spec;
    }

    protected static String featureEvent(ResolvedFeatureId id) {
        return "  " + id;
    }

    protected boolean logEvents = enableLogging();
    private int i = 0;
    private final String[] events;

    protected TestProvisionedConfigHandler() {
        events = initEvents();
    }

    protected boolean enableLogging() {
        return false;
    }

    protected abstract String[] initEvents();

    @Override
    public void prepare(ProvisionedConfig config) {
        i = 0;
    }

    @Override
    public void startBatch() {
        assertNextEvent(batchStartEvent());
    }

    @Override
    public void endBatch() {
        assertNextEvent(batchEndEvent());
    }

    @Override
    public void nextFeaturePack(ArtifactCoords.Gav fpGav) {
        assertNextEvent(featurePackEvent(fpGav));
    }

    @Override
    public void nextSpec(ResolvedFeatureSpec spec) {
        assertNextEvent(specEvent(spec.getName()));
    }

    @Override
    public void nextFeature(ProvisionedFeature feature) {
        assertNextEvent(featureEvent(feature.getId()));
    }

    private void assertNextEvent(String actual) {
        if(logEvents) {
            System.out.println(actual);
        }
        Assert.assertEquals(events[i++], actual);
    }
}
