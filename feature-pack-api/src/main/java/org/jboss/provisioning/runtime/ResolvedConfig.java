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

package org.jboss.provisioning.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningConfigHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedConfig {

    private List<List<ResolvedFeature>> batches = Collections.emptyList();
    private List<ResolvedFeature> batch;

    void newBatch(ResolvedSpecId specId) {
        System.out.println("newBatch " + specId);
        if(batch != null) {
            complete();
        }
        batch = new ArrayList<>();
    }

    void add(ResolvedFeature feature) {
        System.out.println(" + " + (feature.id == null ? feature.spec.id + " config" : feature.id));
        batch.add(feature);
    }

    void complete() {
        if(batch != null) {
            switch(batches.size()) {
                case 0:
                    batches = Collections.singletonList(batch);
                    break;
                case 1:
                    final List<ResolvedFeature> tmp = batches.get(0);
                    batches = new ArrayList<>(2);
                    batches.add(tmp);
                default:
                    batches.add(batch);
            }
        }
        batch = null;
    }

    public boolean hasFeatures() {
        return !batches.isEmpty();
    }

    public void handle(ProvisioningConfigHandler handler) throws ProvisioningException {
        if(batches.isEmpty()) {
            return;
        }
        handler.prepare();
        int batchIndex = 0;
        ArtifactCoords.Gav lastFpGav = null;
        while(batchIndex < batches.size()) {
            final List<ResolvedFeature> batch = batches.get(batchIndex++);
            ResolvedFeature feature = batch.get(0);
            if(feature.spec.id.gav.equals(lastFpGav)) {
                lastFpGav = feature.spec.id.gav;
                handler.nextFeaturePack(lastFpGav);
            }
            handler.nextSpec(feature.spec.id);
            int featureIndex = 1;
            while(featureIndex < batch.size()) {
                handler.nextFeature(batch.get(featureIndex++));
            }
        }
        handler.done();
    }
}
