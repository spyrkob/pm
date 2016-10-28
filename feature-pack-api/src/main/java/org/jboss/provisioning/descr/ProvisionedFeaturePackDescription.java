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

        protected final ArtifactCoords.Gav gav;
        protected boolean includeDefault;
        protected Set<String> excludedPackages = Collections.emptySet();
        protected Set<String> includedPackages = Collections.emptySet();
        protected FeaturePackDescription fpDescr;

        protected Builder(ArtifactCoords.Gav gav) {
            this(gav, true);
        }

        protected Builder(ArtifactCoords.Gav gav, boolean includeDefault) {
            this.gav = gav;
            this.includeDefault = includeDefault;
        }

        protected Builder(FeaturePackDescription fpDescr, ProvisionedFeaturePackDescription provisionedDescr) {
            this.gav = provisionedDescr.getGav();
            this.fpDescr = fpDescr;
            includeDefault = provisionedDescr.includeDefault;

            switch(provisionedDescr.excludedPackages.size()) {
                case 0:
                    break;
                case 1:
                    excludedPackages = Collections.singleton(provisionedDescr.excludedPackages.iterator().next());
                    break;
                default:
                    excludedPackages = new HashSet<String>(provisionedDescr.excludedPackages);
            }

            switch(provisionedDescr.includedPackages.size()) {
                case 0:
                    break;
                case 1:
                    includedPackages = Collections.singleton(provisionedDescr.includedPackages.iterator().next());
                    break;
                default:
                    includedPackages = new HashSet<String>(provisionedDescr.includedPackages);
            }
        }

        public Builder setIncludeDefaultPackages(boolean includeDefault) {
            this.includeDefault = includeDefault;
            return this;
        }

        public Builder excludePackage(String packageName) throws ProvisioningDescriptionException {
            if(includedPackages.contains(packageName)) {
                throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageName));
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
            if(excludedPackages.contains(packageName)) {
                throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageName));
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

        private void removeFromIncluded(String packageName) {
            if(!includedPackages.contains(packageName)) {
                return;
            }
            if(includedPackages.size() == 1) {
                includedPackages = Collections.emptySet();
            } else {
                includedPackages.remove(packageName);
            }
        }

        private void removeFromExcluded(String packageName) {
            if(!excludedPackages.contains(packageName)) {
                return;
            }
            if(excludedPackages.size() == 1) {
                excludedPackages = Collections.emptySet();
            } else {
                excludedPackages.remove(packageName);
            }
        }

        public Builder include(ProvisionedFeaturePackDescription other) throws ProvisioningDescriptionException {
            assertSameGav(other);

            if(includeDefault == other.includeDefault) {
                // this.includes + other.includes
                // this.excludes - other.includes
                // common excludes stay
                if(other.hasIncludedPackages()) {
                    for(String name : other.includedPackages) {
                        includePackage(name);
                        removeFromIncluded(name);
                    }
                }
                if (!excludedPackages.isEmpty()) {
                    if (other.hasExcludedPackages()) {
                        Set<String> tmp = Collections.emptySet();
                        for (String name : other.excludedPackages) {
                            if (excludedPackages.contains(name)) {
                                switch(tmp.size()) {
                                    case 0:
                                        tmp = Collections.singleton(name);
                                        break;
                                    case 1:
                                        tmp = new HashSet<>(tmp);
                                    default:
                                        tmp.add(name);
                                }
                            }
                        }
                        excludedPackages = tmp;
                    } else {
                        excludedPackages = Collections.emptySet();
                    }
                }
            } else if (includeDefault) {
                //this.excludes - other.includes
                if(other.hasIncludedPackages()) {
                    for(String name : other.includedPackages) {
                        removeFromExcluded(name);
                    }
                }
            } else {
                // this.excludes = other.excludes - this.includes
                // includeDefault = true
                excludedPackages = new HashSet<>(other.excludedPackages);
                if(!includedPackages.isEmpty()) {
                    for(String name : includedPackages) {
                        removeFromExcluded(name);
                    }
                }
                includeDefault = true;
            }
            return this;
        }

        public Builder exclude(ProvisionedFeaturePackDescription other) throws ProvisioningDescriptionException {
            assertSameGav(other);

            if(other.includeDefault) {
                // this.include - other.exclude
                // this.include + other.include

                if(!includedPackages.isEmpty() && other.hasExcludedPackages()) {
                    for(String name : other.excludedPackages) {
                        removeFromIncluded(name);
                    }
                }
                if(other.hasIncludedPackages()) {
                    for(String name : other.includedPackages) {
                        includePackage(name);
                    }
                }

                if(includeDefault) {
                    // this.exclude - other.include
                    // this.exclude + other.exclude
                    if(!excludedPackages.isEmpty() && other.hasIncludedPackages()) {
                        for(String name : other.includedPackages) {
                            removeFromExcluded(name);
                        }
                    }
                    if(other.hasExcludedPackages()) {
                        for(String name : other.excludedPackages) {
                            excludePackage(name);
                        }
                    }
                } else {
                    // this.includeDefault = other.includeDefault
                    // this.exclude = other.exclude
                    this.includeDefault = other.includeDefault;
                    this.excludedPackages = Collections.emptySet();
                    for(String name : other.excludedPackages) {
                        excludePackage(name);
                    }
                }
            } else {
                // this.includeDefault = other.includeDefault
                // this.include = other.include
                // this.exclude = other.exclude
                this.includeDefault = other.includeDefault;
                this.includedPackages = Collections.emptySet();
                if (!other.includedPackages.isEmpty()) {
                    for (String name : other.includedPackages) {
                        includePackage(name);
                    }
                }
                this.excludedPackages = Collections.emptySet();
                if (!other.excludedPackages.isEmpty()) {
                    for (String name : other.excludedPackages) {
                        excludePackage(name);
                    }
                }
            }
            return this;
        }

        private void assertSameGav(ProvisionedFeaturePackDescription other) {
            if(!gav.equals(other.gav)) {
                throw new IllegalArgumentException("Feature pack GAVs don't match " + gav + " vs " + other.gav);
            }
        }

        public ProvisionedFeaturePackDescription build() {
            if(fpDescr != null) {
                // remove redundant explicit excludes/includes
                if(includeDefault) {
                    if(!includedPackages.isEmpty() && fpDescr.hasDefaultPackages()) {
                        for(String name : fpDescr.getDefaultPackageNames()) {
                            if(includedPackages.contains(name)) {
                                removeFromIncluded(name);
                            }
                        }
                    }
                } else {
                    if(!excludedPackages.isEmpty() && fpDescr.hasDefaultPackages()) {
                        for(String name : fpDescr.getDefaultPackageNames()) {
                            if(excludedPackages.contains(name)) {
                                removeFromExcluded(name);
                            }
                        }
                    }
                }
            }
            return new ProvisionedFeaturePackDescription(gav, includeDefault,
                    Collections.unmodifiableSet(excludedPackages),
                    Collections.unmodifiableSet(includedPackages));
        }
    }

    public static Builder builder(FeaturePackDescription fpDescr, ProvisionedFeaturePackDescription provisionedDescr) {
        return new Builder(fpDescr, provisionedDescr);
    }

    public static Builder builder(ArtifactCoords.Gav gav) {
        return new Builder(gav);
    }

    public static Builder builder(ArtifactCoords.Gav gav, boolean includeDefault) {
        return new Builder(gav, includeDefault);
    }

    public static ProvisionedFeaturePackDescription forGav(ArtifactCoords.Gav gav) {
        return new ProvisionedFeaturePackDescription(gav, true, Collections.emptySet(), Collections.emptySet());
    }

    private final ArtifactCoords.Gav gav;
    private final boolean includeDefault;
    private final Set<String> excludedPackages;
    private final Set<String> includedPackages;

    protected ProvisionedFeaturePackDescription(ArtifactCoords.Gav gav, boolean includeDefault,
            Set<String> excludedPackages, Set<String> includedPackages) {
        assert gav != null : "gav is null";
        this.gav = gav;
        this.includeDefault = includeDefault;
        this.excludedPackages = excludedPackages;
        this.includedPackages = includedPackages;
    }

    public ArtifactCoords.Gav getGav() {
        return gav;
    }

    public boolean isIncludeDefault() {
        return includeDefault;
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
        if(!includeDefault) {
            builder.append(" includeDefault=false");
        }
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
