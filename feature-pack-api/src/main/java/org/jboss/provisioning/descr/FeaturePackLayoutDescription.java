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
package org.jboss.provisioning.descr;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.jboss.provisioning.Gav;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes a layout of feature-packs from which
 * the target installation is provisioned.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutDescription {

    private final Map<Gav, FeaturePackDescription> featurePacks;

    FeaturePackLayoutDescription(Map<Gav, FeaturePackDescription> featurePacks) {
        assert featurePacks != null : "featurePacks is null";
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public FeaturePackDescription getFeaturePack(Gav gav) {
        return featurePacks.get(gav);
    }

    public Collection<FeaturePackDescription> getFeaturePacks() {
        return featurePacks.values();
    }

    public String logContent() throws IOException {
        final DescrFormatter logger = new DescrFormatter();
        logger.println("Feature-pack layout");
        logger.increaseOffset();
        for(FeaturePackDescription fp : featurePacks.values()) {
            fp.logContent(logger);
        }
        logger.decreaseOffset();
        return logger.toString();
    }
}
