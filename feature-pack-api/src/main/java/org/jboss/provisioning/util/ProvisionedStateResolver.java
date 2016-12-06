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

package org.jboss.provisioning.util;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackLayoutDescription;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedStateResolver {

    private ProvisionedState.Builder stateBuilder;
    private Map<ArtifactCoords.Gav, FeaturePackSpec> fpWithExternalDeps = Collections.emptyMap();

    public ProvisionedState resolve(ProvisioningConfig provisioningConfig,
            FeaturePackLayoutDescription fpLayout, Path layoutDir) throws ProvisioningDescriptionException {

        stateBuilder = ProvisionedState.builder();
        for(FeaturePackConfig fpConfig : provisioningConfig.getFeaturePacks()) {
            final ArtifactCoords.Gav fpGav = fpConfig.getGav();
            final FeaturePackSpec fpSpec = fpLayout.getFeaturePack(fpGav.toGa());
            if(fpSpec == null) {
                throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(fpGav));
            }
            resolveFeaturePack(fpConfig, fpSpec, LayoutUtils.getFeaturePackDir(layoutDir, fpGav));
        }

        final ProvisionedState provisionedState = stateBuilder.build();
        if(!fpWithExternalDeps.isEmpty()) {
            assertExternalDependencies(provisionedState, fpLayout);
            fpWithExternalDeps = Collections.emptyMap();
        }
        return provisionedState;
    }

    private void assertExternalDependencies(ProvisionedState provisionedState, FeaturePackLayoutDescription fpLayout)
            throws ProvisioningDescriptionException {
        for(FeaturePackSpec fpSpec : fpWithExternalDeps.values()) {
            final ProvisionedFeaturePack provisionedFp = provisionedState.getFeaturePack(fpSpec.getGav());
            for(String pkgName : provisionedFp.getPackageNames()) {
                final PackageSpec pkgSpec = fpSpec.getPackage(pkgName);
                if(pkgSpec.hasExternalDependencies()) {
                    for(String depName : pkgSpec.getExternalDependencyNames()) {
                        final FeaturePackDependencySpec fpDep = fpSpec.getDependency(depName);
                        if(fpDep == null) {
                            throw new ProvisioningDescriptionException(Errors.unknownDependencyName(fpSpec.getGav(), depName));
                        }
                        final ProvisionedFeaturePack provisionedTarget = provisionedState.getFeaturePack(fpDep.getTarget().getGav());
                        if(provisionedTarget == null) {
                            throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(fpDep.getTarget().getGav()));
                        }
                        final PackageDependencyGroupSpec pkgDeps = pkgSpec.getExternalDependencies(depName);
                        for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                            if(!pkgDep.isOptional() && !provisionedTarget.containsPackage(pkgDep.getName())) {
                                throw new ProvisioningDescriptionException(
                                        Errors.unsatisfiedExternalPackageDependency(fpSpec.getGav(), pkgName, fpDep.getTarget().getGav(), pkgDep.getName()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void resolveFeaturePack(FeaturePackConfig fpConfig, FeaturePackSpec fpSpec, Path fpDir)
            throws ProvisioningDescriptionException {

        final ProvisionedFeaturePack.Builder fpBuilder = ProvisionedFeaturePack.builder(fpConfig.getGav());

        if(fpConfig.isInheritPackages()) {
            for (String name : fpSpec.getDefaultPackageNames()) {
                resolvePackage(fpSpec, fpConfig, fpBuilder, name, true, null);
            }
        }
        if(fpConfig.hasIncludedPackages()) {
            for(String name : fpConfig.getIncludedPackages()) {
                resolvePackage(fpSpec, fpConfig, fpBuilder, name, false, null);
            }
        }

        stateBuilder.addFeaturePack(fpBuilder.build());
    }

    private void resolvePackage(FeaturePackSpec fpSpec, FeaturePackConfig fpConfig,
            ProvisionedFeaturePack.Builder fpBuilder, final String pkgName, boolean optional, String dependingPkg)
            throws ProvisioningDescriptionException {
        if(fpBuilder.hasPackage(pkgName)) {
            return;
        }
        if(fpConfig.isExcluded(pkgName)) {
            if(optional) {
                return;
            } else {
                throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fpSpec.getGav(), dependingPkg, pkgName));
            }
        }
        final PackageSpec pkgSpec = fpSpec.getPackage(pkgName);
        if (pkgSpec == null) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fpSpec.getGav(), pkgName));
        }
        fpBuilder.addPackage(pkgName);
        if (pkgSpec.hasLocalDependencies()) {
            for (PackageDependencySpec dep : pkgSpec.getLocalDependencies().getDescriptions()) {
                resolvePackage(fpSpec, fpConfig, fpBuilder, dep.getName(), dep.isOptional(), pkgSpec.getName());
            }
        }
        if(pkgSpec.hasExternalDependencies() && !fpWithExternalDeps.containsKey(fpSpec.getGav())) {
            switch(fpWithExternalDeps.size()) {
                case 0:
                    fpWithExternalDeps = Collections.singletonMap(fpSpec.getGav(), fpSpec);
                    break;
                case 1:
                    fpWithExternalDeps = new HashMap<>(fpWithExternalDeps);
                default:
                    fpWithExternalDeps.put(fpSpec.getGav(), fpSpec);
            }
        }
    }
}
