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
 * Describes a feature-pack as it was provisioned.
 *
 * @author Alexey Loubyansky
 */
public interface ProvisionedFeaturePack<P extends ProvisionedPackage> {

    class Builder {
        private ArtifactCoords.Gav gav;
        private Map<String, ProvisionedPackage> packages = Collections.emptyMap();

        private Builder(ArtifactCoords.Gav gav) {
            this.gav = gav;
        }

        public Builder addPackage(String name) {
            return addPackage(ProvisionedPackage.newInstance(name));
        }

        public Builder addPackage(ProvisionedPackage provisionedPkg) {
            switch(packages.size()) {
                case 0:
                    packages = Collections.singletonMap(provisionedPkg.getName(), provisionedPkg);
                    break;
                case 1:
                    packages = new LinkedHashMap<>(packages);
                default:
                    packages.put(provisionedPkg.getName(), provisionedPkg);
            }
            return this;
        }

        public boolean hasPackage(String name) {
            return packages.containsKey(name);
        }

        public ProvisionedFeaturePack<ProvisionedPackage> build() {
            return new ProvisionedFeaturePackImpl(gav, packages.size() > 1 ? Collections.unmodifiableMap(packages) : packages);
        }
    }

    static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    ArtifactCoords.Gav getGav();

    boolean hasPackages();

    boolean containsPackage(String name);

    Set<String> getPackageNames();

    Collection<P> getPackages();

    P getPackage(String name);
}
