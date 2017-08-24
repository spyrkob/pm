/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;

/**
 * This class combines the feature-pack and the package specs that belong
 * to the feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayout {

    public static class Builder {

        private final FeaturePackSpec.Builder spec;
        private Map<String, PackageSpec> packages = Collections.emptyMap();

        private Builder(FeaturePackSpec.Builder spec) {
            this.spec = spec;
        }

        public Builder addPackage(PackageSpec pkg) {
            switch(packages.size()) {
                case 0:
                    packages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    final Map.Entry<String, PackageSpec> first = packages.entrySet().iterator().next();
                    packages = new HashMap<>();
                    packages.put(first.getKey(), first.getValue());
                default:
                    packages.put(pkg.getName(), pkg);
            }
            return this;
        }

        public FeaturePackSpec.Builder getSpecBuilder() {
            return spec;
        }

        public FeaturePackLayout build() throws ProvisioningDescriptionException {
            final FeaturePackSpec builtSpec = spec.build();
            for(String name : builtSpec.getDefaultPackageNames()) {
                if(!packages.containsKey(name)) {
                    throw new ProvisioningDescriptionException(Errors.unknownPackage(builtSpec.getGav(), name));
                }
            }
            boolean externalPackageDependencies = false;
            // package dependency consistency check
            if (!packages.isEmpty()) {
                for (PackageSpec pkg : packages.values()) {
                    if (pkg.dependsOnLocalPackages()) {
                        List<String> notFound = null;
                        for(PackageDependencySpec pkgDep : pkg.getLocalPackageDependencies().getDescriptions()) {
                            final PackageSpec depSpec = packages.get(pkgDep.getName());
                            if(depSpec == null) {
                                if(notFound == null) {
                                    notFound = new ArrayList<>();
                                }
                                notFound.add(pkgDep.getName());
                            } else if(pkgDep.hasParams()) {
                                for(PackageParameter depParam : pkgDep.getParameters()) {
                                    if(!depSpec.hasParameter(depParam.getName())) {
                                        throw new ProvisioningDescriptionException(
                                                Errors.unknownParameterInDependency(builtSpec.getGav(), pkg.getName(), pkgDep.getName(), depParam.getName()));
                                    }
                                }
                            }
                        }
                        if (notFound != null) {
                            throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependencies(builtSpec.getGav(), pkg.getName(), notFound));
                        }
                    }
                    if(pkg.dependsOnExternalPackages()) {
                        for(String depName : pkg.getPackageDependencySources()) {
                            try {
                                builtSpec.getDependency(depName);
                            } catch(ProvisioningDescriptionException e) {
                                throw new ProvisioningDescriptionException(Errors.unknownFeaturePackDependencyName(builtSpec.getGav(), pkg.getName(), depName), e);
                            }
                        }
                        externalPackageDependencies = true;
                    }
                }
            }
            return new FeaturePackLayout(builtSpec, packages.size() > 1 ? Collections.unmodifiableMap(packages) : packages, externalPackageDependencies);
        }
    }

    public static Builder builder(FeaturePackSpec.Builder spec) {
        return new Builder(spec);
    }

    private final FeaturePackSpec spec;
    private final Map<String, PackageSpec> packages;
    private final boolean externalPackageDependencies;

    private FeaturePackLayout(FeaturePackSpec spec, Map<String, PackageSpec> packages, boolean externalPackageDependencies) {
        this.spec = spec;
        this.packages = packages;
        this.externalPackageDependencies = externalPackageDependencies;
    }

    public ArtifactCoords.Gav getGav() {
        return spec.getGav();
    }

    public FeaturePackSpec getSpec() {
        return spec;
    }

    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    public boolean hasPackage(String name) {
        return packages.containsKey(name);
    }

    public PackageSpec getPackage(String name) {
        return packages.get(name);
    }

    public Set<String> getPackageNames() {
        return packages.keySet();
    }

    public Collection<PackageSpec> getPackages() {
        return packages.values();
    }

    public boolean hasExternalPackageDependencies() {
        return externalPackageDependencies;
    }
}
