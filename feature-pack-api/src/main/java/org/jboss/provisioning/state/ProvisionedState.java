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

package org.jboss.provisioning.state;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;

/**
 * Represents provisioned installation.
 *
 * @author Alexey Loubyansky
 */
public interface ProvisionedState<F extends ProvisionedFeaturePack<P>, P extends ProvisionedPackage> {

    class Builder {
        private Map<ArtifactCoords.Gav, ProvisionedFeaturePack<ProvisionedPackage>> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        public Builder addFeaturePack(ProvisionedFeaturePack<ProvisionedPackage> fp) {
            switch(featurePacks.size()) {
                case 0:
                    featurePacks = Collections.singletonMap(fp.getGav(), fp);
                    break;
                case 1:
                    featurePacks = new LinkedHashMap<>(featurePacks);
                default:
                    featurePacks.put(fp.getGav(), fp);
            }
            return this;
        }

        public ProvisionedState<ProvisionedFeaturePack<ProvisionedPackage>, ProvisionedPackage> build() {
            return new ProvisionedStateImpl(featurePacks.size() > 1 ? Collections.unmodifiableMap(featurePacks) : featurePacks);
        }
    }

    static Builder builder() {
        return new Builder();
    }

    boolean hasFeaturePacks();

    Set<ArtifactCoords.Gav> getFeaturePackGavs();

    Collection<F> getFeaturePacks();

    F getFeaturePack(ArtifactCoords.Gav gav);
}
