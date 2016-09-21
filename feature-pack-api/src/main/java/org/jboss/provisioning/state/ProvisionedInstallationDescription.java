/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provisioning.state;

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
