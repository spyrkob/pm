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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;

/**
 * This class represents user's choice to install a feature-pack
 * with options to exclude undesired packages.
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedFeaturePackDescription {

    public static class Builder {

        protected ArtifactCoords.Gav gav;
        protected Set<String> excludedPackages = Collections.emptySet();
        protected Set<String> includedPackages = Collections.emptySet();

        protected Builder() {
        }

        protected Builder(ProvisionedFeaturePackDescription descr) {
            this.gav = descr.getGav();

            switch(descr.excludedPackages.size()) {
                case 0:
                    break;
                case 1:
                    excludedPackages = Collections.singleton(descr.excludedPackages.iterator().next());
                    break;
                default:
                    excludedPackages = new HashSet<String>(descr.excludedPackages);
            }

            switch(descr.includedPackages.size()) {
                case 0:
                    break;
                case 1:
                    includedPackages = Collections.singleton(descr.includedPackages.iterator().next());
                    break;
                default:
                    includedPackages = new HashSet<String>(descr.includedPackages);
            }
        }

        public Builder setGav(ArtifactCoords.Gav gav) {
            this.gav = gav;
            return this;
        }

        public Builder excludePackage(String packageName) throws ProvisioningDescriptionException {
            if(!includedPackages.isEmpty()) {
                throw new ProvisioningDescriptionException(Errors.packageExcludesIncludes());
            }
            if(!excludedPackages.contains(packageName)) {
                switch(excludedPackages.size()) {
                    case 0:
                        excludedPackages = Collections.singleton(packageName);
                        break;
                    case 1:
                        if(excludedPackages.contains(packageName)) {
                            return this;
                        }
                        excludedPackages = new HashSet<>(excludedPackages);
                    default:
                        excludedPackages.add(packageName);
                }
            }
            return this;
        }

        public Builder excludeAllPackages(Collection<String> packageNames) throws ProvisioningDescriptionException {
            for(String packageName : packageNames) {
                excludePackage(packageName);
            }
            return this;
        }

        public Builder includePackage(String packageName) throws ProvisioningDescriptionException {
            if(!excludedPackages.isEmpty()) {
                throw new ProvisioningDescriptionException(Errors.packageExcludesIncludes());
            }
            if(!includedPackages.contains(packageName)) {
                switch(includedPackages.size()) {
                    case 0:
                        includedPackages = Collections.singleton(packageName);
                        break;
                    case 1:
                        if(includedPackages.contains(packageName)) {
                            return this;
                        }
                        includedPackages = new HashSet<>(includedPackages);
                    default:
                        includedPackages.add(packageName);
                }
            }
            return this;
        }

        public Builder include(ProvisionedFeaturePackDescription other) throws ProvisioningDescriptionException {
            if(!gav.equals(other.gav)) {
                throw new IllegalArgumentException("Feature pack GAVs don't match " + gav + " vs " + other.gav);
            }

            if(!excludedPackages.isEmpty()) {
                if(other.excludedPackages.isEmpty()) {
                    if(other.includedPackages.isEmpty()) {
                        // nothing included or excluded
                        excludedPackages = Collections.emptySet();
                    } else {
                        // remove included from the excluded
                        final Iterator<String> includedIterator = other.includedPackages.iterator();
                        while(includedIterator.hasNext() && !excludedPackages.isEmpty()) {
                            final String included = includedIterator.next();
                            if(excludedPackages.contains(included)) {
                                if(excludedPackages.size() == 1) {
                                    excludedPackages = Collections.emptySet();
                                } else {
                                    excludedPackages.remove(included);
                                }
                            }
                        }
                    }
                } else {
                    if(excludedPackages.size() == 1) {
                        if(!other.excludedPackages.containsAll(excludedPackages)) {
                            excludedPackages = Collections.emptySet();
                        }
                    } else {
                        excludedPackages.retainAll(other.excludedPackages);
                    }
                }
            } else if(!includedPackages.isEmpty()) {
                if(other.includedPackages.isEmpty()) {
                    if(other.excludedPackages.isEmpty()) {
                        includedPackages = Collections.emptySet();
                    } else {
                        // remove included from the excluded
                        final Set<String> tmpIncluded = includedPackages;
                        this.excludedPackages = other.excludedPackages;
                        final Iterator<String> includedIterator = tmpIncluded.iterator();
                        while(includedIterator.hasNext() && !excludedPackages.isEmpty()) {
                            final String included = includedIterator.next();
                            if(excludedPackages.contains(included)) {
                                if(excludedPackages.size() == 1) {
                                    excludedPackages = Collections.emptySet();
                                } else {
                                    excludedPackages.remove(included);
                                }
                            }
                        }
                    }
                } else {
                    if(!includedPackages.containsAll(other.includedPackages)) {
                        if (includedPackages.size() == 1) {
                            includedPackages = new HashSet<>(includedPackages);
                        }
                        includedPackages.addAll(other.includedPackages);
                    }
                }
            }
            return this;
        }

        public Builder exclude(ProvisionedFeaturePackDescription other) {
            if(!gav.equals(other.gav)) {
                throw new IllegalArgumentException("Feature pack GAVs don't match " + gav + " vs " + other.gav);
            }

            if(other.excludedPackages.isEmpty()) {
                if(other.includedPackages.isEmpty()) {
                    return this;
                } else {
                    excludedPackages = Collections.emptySet();
                    includedPackages = other.includedPackages;
                }
            } else {
                if(excludedPackages.isEmpty()) {
                    if(includedPackages.isEmpty()) {
                        this.excludedPackages = other.excludedPackages;
                    } else {
                        final Set<String> tmpIncluded = new HashSet<>(includedPackages);
                        for(String excluded : other.excludedPackages) {
                            tmpIncluded.remove(excluded);
                        }
                        if(tmpIncluded.isEmpty()) {
                            includedPackages = Collections.emptySet();
                        }
                    }
                } else {
                    if(excludedPackages.containsAll(other.excludedPackages)) {
                        return this;
                    }
                    if(excludedPackages.size() == 1) {
                        excludedPackages = new HashSet<String>(excludedPackages);
                    }
                    excludedPackages.addAll(other.excludedPackages);
                }
            }
            return this;
        }

        public ProvisionedFeaturePackDescription build() {
            return new ProvisionedFeaturePackDescription(gav, Collections.unmodifiableSet(excludedPackages), Collections.unmodifiableSet(includedPackages));
        }
    }

    public static Builder builder(ProvisionedFeaturePackDescription descr) {
        return new Builder(descr);
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ArtifactCoords.Gav gav;
    private final Set<String> excludedPackages;
    private final Set<String> includedPackages;

    protected ProvisionedFeaturePackDescription(ArtifactCoords.Gav gav, Set<String> excludedPackages, Set<String> includedPackages) {
        assert gav != null : "gav is null";
        this.gav = gav;
        this.excludedPackages = excludedPackages;
        this.includedPackages = includedPackages;
    }

    public ArtifactCoords.Gav getGav() {
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(gav.toString());
        if(!excludedPackages.isEmpty()) {
            final String[] array = excludedPackages.toArray(new String[excludedPackages.size()]);
            Arrays.sort(array);
            builder.append(" excluded ").append(Arrays.asList(array));
        }
        if(!includedPackages.isEmpty()) {
            final String[] array = includedPackages.toArray(new String[includedPackages.size()]);
            Arrays.sort(array);
            builder.append(" included ").append(Arrays.asList(array));
        }
        return builder.append("]").toString();
    }
}
