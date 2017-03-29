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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;

/**
 * Represents provisioned installation.
 *
 * @author Alexey Loubyansky
 */
class ProvisionedStateImpl implements ProvisionedState<ProvisionedFeaturePack<ProvisionedPackage>, ProvisionedPackage> {

    private final Map<ArtifactCoords.Gav, ProvisionedFeaturePack<ProvisionedPackage>> featurePacks;

    ProvisionedStateImpl(Map<ArtifactCoords.Gav, ProvisionedFeaturePack<ProvisionedPackage>> featurePacks) {
        this.featurePacks = featurePacks;
    }

    @Override
    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    @Override
    public Set<ArtifactCoords.Gav> getFeaturePackGavs() {
        return featurePacks.keySet();
    }

    @Override
    public Collection<ProvisionedFeaturePack<ProvisionedPackage>> getFeaturePacks() {
        return featurePacks.values();
    }

    @Override
    public ProvisionedFeaturePack<ProvisionedPackage> getFeaturePack(ArtifactCoords.Gav gav) {
        return featurePacks.get(gav);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featurePacks == null) ? 0 : featurePacks.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProvisionedStateImpl other = (ProvisionedStateImpl) obj;
        if (featurePacks == null) {
            if (other.featurePacks != null)
                return false;
        } else if (!featurePacks.equals(other.featurePacks))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[state [");
        if(!featurePacks.isEmpty()) {
            final Iterator<ProvisionedFeaturePack<ProvisionedPackage>> i = featurePacks.values().iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(", ").append(i.next());
            }
        }
        return buf.append("]]").toString();
    }
}
