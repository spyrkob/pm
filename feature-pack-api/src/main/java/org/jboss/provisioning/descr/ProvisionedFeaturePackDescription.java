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
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;

/**
 * This class represents user's choice to install a feature-pack
 * with options to exclude undesired packages.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedFeaturePackDescription {

    public static class Builder {

        protected ArtifactCoords.GavPart gav;
        protected Set<String> excludedPackages = Collections.emptySet();
        protected Set<String> includedPackages = Collections.emptySet();

        protected Builder() {
        }

        public Builder setGav(ArtifactCoords.GavPart gav) {
            this.gav = gav;
            return this;
        }

        public Builder excludePackage(String packageName) throws ProvisioningException {
            if(!includedPackages.isEmpty()) {
                throw new ProvisioningException(Errors.packageExcludesIncludes());
            }
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

        public Builder excludeAllPackages(Collection<String> packageNames) throws ProvisioningException {
            for(String packageName : packageNames) {
                excludePackage(packageName);
            }
            return this;
        }

        public Builder includePackage(String packageName) throws ProvisioningException {
            if(!excludedPackages.isEmpty()) {
                throw new ProvisioningException(Errors.packageExcludesIncludes());
            }
            if(!includedPackages.contains(packageName)) {
                switch(includedPackages.size()) {
                    case 0:
                        includedPackages = Collections.singleton(packageName);
                        break;
                    case 1:
                        includedPackages = new HashSet<>(includedPackages);
                    default:
                        includedPackages.add(packageName);
                }
            }
            return this;
        }

        public ProvisionedFeaturePackDescription build() {
            return new ProvisionedFeaturePackDescription(gav, Collections.unmodifiableSet(excludedPackages), Collections.unmodifiableSet(includedPackages));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ArtifactCoords.GavPart gav;
    private final Set<String> excludedPackages;
    private final Set<String> includedPackages;

    protected ProvisionedFeaturePackDescription(ArtifactCoords.GavPart gav, Set<String> excludedPackages, Set<String> includedPackages) {
        assert gav != null : "gav is null";
        this.gav = gav;
        this.excludedPackages = excludedPackages;
        this.includedPackages = includedPackages;
    }

    public ArtifactCoords.GavPart getGav() {
        return gav;
    }

    public boolean hasIncludedPackages() {
        return !includedPackages.isEmpty();
    }

    public boolean isIncluded(String packageName) {
        return includedPackages.contains(packageName);
    }

    public Set<String> getIncludedPackages() {
        return includedPackages;
    }

    public boolean hasExcludedPackages() {
        return !excludedPackages.isEmpty();
    }

    public boolean isExcluded(String packageName) {
        return excludedPackages.contains(packageName);
    }

    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((excludedPackages == null) ? 0 : excludedPackages.hashCode());
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((includedPackages == null) ? 0 : includedPackages.hashCode());
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
        if (includedPackages == null) {
            if (other.includedPackages != null)
                return false;
        } else if (!includedPackages.equals(other.includedPackages))
            return false;
        return true;
    }
}
