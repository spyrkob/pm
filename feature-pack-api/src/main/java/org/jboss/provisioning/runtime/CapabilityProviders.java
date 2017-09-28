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


/**
 * @author Alexey Loubyansky
 *
 */
class CapabilityProviders {

    // specs providing the capability
    List<SpecFeatures> specs = Collections.emptyList();
    // features providing the capability of specs that don't provide the capability
    List<ResolvedFeature> features = Collections.emptyList();

    private boolean provided;

    void add(SpecFeatures specFeatures) {
        switch(specs.size()) {
            case 0:
                specs = Collections.singletonList(specFeatures);
                break;
            case 1:
                final SpecFeatures first = specs.get(0);
                specs = new ArrayList<>(2);
                specs.add(first);
            default:
                specs.add(specFeatures);
        }
        specFeatures.spec.addCapabilityProviders(this);
    }

    void add(ResolvedFeature feature) {
        switch(features.size()) {
            case 0:
                features = Collections.singletonList(feature);
                break;
            case 1:
                final ResolvedFeature first = features.get(0);
                features = new ArrayList<>(2);
                features.add(first);
            default:
                features.add(feature);
        }
        feature.addCapabilityProviders(this);
    }

    void provided() {
        provided = true;
    }

    boolean isProvided() {
        return provided;
    }
}
