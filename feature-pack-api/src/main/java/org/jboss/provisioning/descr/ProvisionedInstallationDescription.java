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

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.ProvisioningException;

/**
 * This class describes the state of provisioned installation.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedInstallationDescription {

    public static class Builder {

        private Map<Gav.GaPart, ProvisionedFeaturePackDescription> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        private Builder(ProvisionedInstallationDescription installDescr) throws ProvisioningDescriptionException {
            for(ProvisionedFeaturePackDescription fp : installDescr.getFeaturePacks()) {
                addFeaturePack(fp);
            }
        }

        public Builder addFeaturePack(ProvisionedFeaturePackDescription fp) throws ProvisioningDescriptionException {
            final Gav.GaPart gaPart = fp.getGav().getGaPart();
            if(featurePacks.containsKey(gaPart)) {
                throw new ProvisioningDescriptionException(Errors.featurePackVersionConflict(fp.getGav(), featurePacks.get(gaPart).getGav()));
            }
            switch(featurePacks.size()) {
                case 0:
                    featurePacks = Collections.singletonMap(gaPart, fp);
                    break;
                case 1:
                    featurePacks = new LinkedHashMap<>(featurePacks);
                default:
                    featurePacks.put(gaPart, fp);
            }
            return this;
        }

        public Builder removeFeaturePack(Gav gav) throws ProvisioningException {
            if(featurePacks.size() == 1) {
                if(!featurePacks.containsKey(gav)) {
                    throw new ProvisioningException("Installation does not contain feature-pack " + gav);
                }
                featurePacks = Collections.emptyMap();
            } else {
                if(featurePacks.remove(gav) == null) {
                    throw new ProvisioningException("Installation does not contain feature-pack " + gav);
                }
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

    /**
     * Allows to build an installation description starting from the passed in
     * initial state.
     *
     * @param installDescr  initial state of the description to be built
     * @return  this builder instance
     * @throws ProvisioningDescriptionException
     */
    public static Builder builder(ProvisionedInstallationDescription installDescr) throws ProvisioningDescriptionException {
        return new Builder(installDescr);
    }

    private Map<Gav.GaPart, ProvisionedFeaturePackDescription> featurePacks;

    private ProvisionedInstallationDescription(Map<Gav.GaPart, ProvisionedFeaturePackDescription> featurePacks) {
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public boolean containsFeaturePack(Gav.GaPart gaPart) {
        return featurePacks.containsKey(gaPart);
    }

    public Set<Gav.GaPart> getFeaturePackGaParts() {
        return featurePacks.keySet();
    }

    public Collection<ProvisionedFeaturePackDescription> getFeaturePacks() {
        return featurePacks.values();
    }

    public ProvisionedFeaturePackDescription getFeaturePack(Gav.GaPart gaPart) {
        return featurePacks.get(gaPart);
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
