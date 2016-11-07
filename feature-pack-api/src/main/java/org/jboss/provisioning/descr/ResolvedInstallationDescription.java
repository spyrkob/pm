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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedInstallationDescription {

    public static class Builder {
        private Map<ArtifactCoords.Gav, ResolvedFeaturePackDescription> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        public Builder addFeaturePack(ResolvedFeaturePackDescription fp) {
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

        public ResolvedInstallationDescription build() {
            return new ResolvedInstallationDescription(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ArtifactCoords.Gav, ResolvedFeaturePackDescription> featurePacks;

    private ResolvedInstallationDescription(Builder builder) {
        this.featurePacks = Collections.unmodifiableMap(builder.featurePacks);
    }

    public Set<ArtifactCoords.Gav> getFeaturePackGavs() {
        return featurePacks.keySet();
    }

    public Collection<ResolvedFeaturePackDescription> getFeaturePacks() {
        return featurePacks.values();
    }

    public ResolvedFeaturePackDescription getFeaturePack(ArtifactCoords.Gav gav) {
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
        ResolvedInstallationDescription other = (ResolvedInstallationDescription) obj;
        if (featurePacks == null) {
            if (other.featurePacks != null)
                return false;
        } else if (!featurePacks.equals(other.featurePacks))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[featurePacks=" + featurePacks + "]";
    }
}
