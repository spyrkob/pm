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

package org.jboss.provisioning.descr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.ProvisioningException;

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
        private Set<String> includedPackages = Collections.emptySet();
        private Map<GAV, ProvisionedFeaturePackDescription> dependencies = Collections.emptyMap();

        private Builder() {
        }

        public Builder setGAV(GAV gav) {
            this.gav = gav;
            return this;
        }

        public Builder excludePackage(String packageName) throws ProvisioningException {
            if(!includedPackages.isEmpty()) {
                throw new ProvisioningException("There already are included packages.");
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
                throw new ProvisioningException("There already are excluded packages.");
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

        public Builder addDependency(ProvisionedFeaturePackDescription dependency) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(dependency.getGAV(), dependency);
                    break;
                case 1:
                    dependencies = new LinkedHashMap<>(dependencies);
                default:
                    dependencies.put(dependency.getGAV(), dependency);
            }
            return this;
        }

        public ProvisionedFeaturePackDescription build() {
            return new ProvisionedFeaturePackDescription(gav, Collections.unmodifiableSet(excludedPackages), Collections.unmodifiableSet(includedPackages),
                    Collections.unmodifiableMap(dependencies));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final GAV gav;
    private final Set<String> excludedPackages;
    private final Set<String> includedPackages;
    private final Map<GAV, ProvisionedFeaturePackDescription> dependencies;

    private ProvisionedFeaturePackDescription(GAV gav, Set<String> excludedPackages, Set<String> includedPackages,
            Map<GAV, ProvisionedFeaturePackDescription> dependencies) {
        assert gav != null : "gav is null";
        this.gav = gav;
        this.excludedPackages = excludedPackages;
        this.includedPackages = includedPackages;
        this.dependencies = dependencies;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasIncludedPackages() {
        return !includedPackages.isEmpty();
    }

    public boolean isIncluded(String packageName) {
        return includedPackages.contains(packageName);
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

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Collection<ProvisionedFeaturePackDescription> getDependencies() {
        return dependencies.values();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
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
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
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
