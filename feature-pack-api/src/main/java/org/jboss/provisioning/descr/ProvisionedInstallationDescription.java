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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;

/**
 * This class describes the state of provisioned installation.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedInstallationDescription {

    public static class Builder {

        private Map<GAV, ProvisionedFeaturePackDescription> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        public Builder addFeaturePack(ProvisionedFeaturePackDescription fp) {
            switch(featurePacks.size()) {
                case 0:
                    featurePacks = Collections.singletonMap(fp.getGAV(), fp);
                    break;
                case 1:
                    featurePacks = new LinkedHashMap<>(featurePacks);
                default:
                    featurePacks.put(fp.getGAV(), fp);
            }
            return this;
        }

        public ProvisionedInstallationDescription build() {
            return new ProvisionedInstallationDescription(Collections.unmodifiableMap(featurePacks));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private Map<GAV, ProvisionedFeaturePackDescription> featurePacks;

    private ProvisionedInstallationDescription(Map<GAV, ProvisionedFeaturePackDescription> featurePacks) {
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public Set<GAV> getFeaturePackGAVs() {
        return featurePacks.keySet();
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
        ProvisionedInstallationDescription other = (ProvisionedInstallationDescription) obj;
        if (featurePacks == null) {
            if (other.featurePacks != null)
                return false;
        } else if (!featurePacks.equals(other.featurePacks))
            return false;
        return true;
    }
}
