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

package org.jboss.provisioning.state;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;

/**
 * Describes a feature-pack as it was provisioned.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedFeaturePack {

    public static class Builder {
        private ArtifactCoords.Gav gav;
        private Set<String> packages = Collections.emptySet();

        private Builder(ArtifactCoords.Gav gav) {
            this.gav = gav;
        }

        public Builder addPackage(String name) {
            switch(packages.size()) {
                case 0:
                    packages = Collections.singleton(name);
                    break;
                case 1:
                    packages = new HashSet<>(packages);
                default:
                    packages.add(name);
            }
            return this;
        }

        public boolean hasPackage(String name) {
            return packages.contains(name);
        }

        public ProvisionedFeaturePack build() {
            return new ProvisionedFeaturePack(this);
        }
    }

    public static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    private final ArtifactCoords.Gav gav;
    private final Set<String> packages;

    private ProvisionedFeaturePack(Builder builder) {
        this.gav = builder.gav;
        this.packages = Collections.unmodifiableSet(builder.packages);
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    public boolean containsPackage(String name) {
        return packages.contains(name);
    }

    public Set<String> getPackageNames() {
        return packages;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((packages == null) ? 0 : packages.hashCode());
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
        ProvisionedFeaturePack other = (ProvisionedFeaturePack) obj;
        if (gav == null) {
            if (other.gav != null)
                return false;
        } else if (!gav.equals(other.gav))
            return false;
        if (packages == null) {
            if (other.packages != null)
                return false;
        } else if (!packages.equals(other.packages))
            return false;
        return true;
    }

    public String toString() {
        return new StringBuilder().append('[').append(gav).append(' ').append(packages).append(']').toString();
    }
}
