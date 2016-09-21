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
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.GAV;

/**
 * This class represents user's choice to install a feature-pack
 * with options to exclude undesired content.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedFeaturePackDescription {

    public static class Builder {

        private GAV gav;
        private Set<String> excludedPackages = Collections.emptySet();

        private Builder() {
        }

        public Builder setGAV(GAV gav) {
            this.gav = gav;
            return this;
        }

        public Builder excludePackage(String packageName) {
            if(!excludedPackages.contains(packageName)) {
                switch(excludedPackages.size()) {
                    case 0:
                        excludedPackages = Collections.singleton(packageName);
                        break;
                    case 1:
                        excludedPackages = new HashSet<>(excludedPackages);
                    default:
                        excludedPackages.add(packageName);
                }
            }
            return this;
        }

        public ProvisionedFeaturePackDescription build() {
            return new ProvisionedFeaturePackDescription(gav, Collections.unmodifiableSet(excludedPackages));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final GAV gav;
    private final Set<String> excludedPackages;

    private ProvisionedFeaturePackDescription(GAV gav, Set<String> excludedPackages) {
        assert gav != null : "gav is null";
        this.gav = gav;
        this.excludedPackages = excludedPackages;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean isExcluded(String packageName) {
        return excludedPackages.contains(packageName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((excludedPackages == null) ? 0 : excludedPackages.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
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
        ProvisionedFeaturePackDescription other = (ProvisionedFeaturePackDescription) obj;
        if (excludedPackages == null) {
            if (other.excludedPackages != null)
                return false;
        } else if (!excludedPackages.equals(other.excludedPackages))
            return false;
        if (gav == null) {
            if (other.gav != null)
                return false;
        } else if (!gav.equals(other.gav))
            return false;
        return true;
    }
}
