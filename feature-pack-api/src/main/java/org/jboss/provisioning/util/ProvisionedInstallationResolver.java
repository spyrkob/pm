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
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.PackageDependencyDescription;
import org.jboss.provisioning.descr.PackageDependencyGroupDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.descr.ResolvedFeaturePackDescription;
import org.jboss.provisioning.descr.ResolvedInstallationDescription;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedInstallationResolver {

    private ResolvedInstallationDescription.Builder installBuilder;
    private Map<ArtifactCoords.Gav, FeaturePackDescription> fpWithExternalDeps = Collections.emptyMap();

    public ResolvedInstallationDescription resolve(ProvisioningConfig provisioningConfig,
            FeaturePackLayoutDescription fpLayout, Path layoutDir) throws ProvisioningDescriptionException {

        installBuilder = ResolvedInstallationDescription.builder();
        for(FeaturePackConfig fpConfig : provisioningConfig.getFeaturePacks()) {
            final ArtifactCoords.Gav fpGav = fpConfig.getGav();
            final FeaturePackDescription fpDescr = fpLayout.getFeaturePack(fpGav.toGa());
            if(fpDescr == null) {
                throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(fpGav));
            }
            resolveFeaturePack(fpConfig, fpDescr, LayoutUtils.getFeaturePackDir(layoutDir, fpGav));
        }

        final ResolvedInstallationDescription resolvedInstall = installBuilder.build();
        if(!fpWithExternalDeps.isEmpty()) {
            assertExternalDependencies(resolvedInstall, fpLayout);
            fpWithExternalDeps = Collections.emptyMap();
        }
        return resolvedInstall;
    }

    private void assertExternalDependencies(ResolvedInstallationDescription resolvedInstall, FeaturePackLayoutDescription fpLayout)
            throws ProvisioningDescriptionException {
        for(FeaturePackDescription fpDescr : fpWithExternalDeps.values()) {
            final ResolvedFeaturePackDescription resolvedFp = resolvedInstall.getFeaturePack(fpDescr.getGav());
            for(String pkgName : resolvedFp.getPackageNames()) {
                final PackageDescription pkgDescr = fpDescr.getPackage(pkgName);
                if(pkgDescr.hasExternalDependencies()) {
                    for(String depName : pkgDescr.getExternalDependencyNames()) {
                        final FeaturePackDependencyDescription fpDep = fpDescr.getDependency(depName);
                        if(fpDep == null) {
                            throw new ProvisioningDescriptionException(Errors.unknownDependencyName(fpDescr.getGav(), depName));
                        }
                        final ResolvedFeaturePackDescription resolvedTarget = resolvedInstall.getFeaturePack(fpDep.getTarget().getGav());
                        if(resolvedTarget == null) {
                            throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(fpDep.getTarget().getGav()));
                        }
                        final PackageDependencyGroupDescription pkgDeps = pkgDescr.getExternalDependencies(depName);
                        for(PackageDependencyDescription pkgDep : pkgDeps.getDescriptions()) {
                            if(!pkgDep.isOptional() && !resolvedTarget.containsPackage(pkgDep.getName())) {
                                throw new ProvisioningDescriptionException(Errors.requiredPackageExcluded(pkgDep.getName(), fpDep.getTarget().getGav()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void resolveFeaturePack(FeaturePackConfig fpConfig, FeaturePackDescription fpDescr, Path fpDir)
            throws ProvisioningDescriptionException {

        final ResolvedFeaturePackDescription.Builder fpBuilder = ResolvedFeaturePackDescription.builder(fpConfig.getGav());

        if(fpConfig.isInheritPackages()) {
            for (String name : fpDescr.getDefaultPackageNames()) {
                resolvePackage(fpDescr, fpConfig, fpBuilder, name, true);
            }
        }
        if(fpConfig.hasIncludedPackages()) {
            for(String name : fpConfig.getIncludedPackages()) {
                resolvePackage(fpDescr, fpConfig, fpBuilder, name, false);
            }
        }

        installBuilder.addFeaturePack(fpBuilder.build());
    }

    private void resolvePackage(FeaturePackDescription fpDescr, FeaturePackConfig fpConfig,
            ResolvedFeaturePackDescription.Builder fpBuilder, final String pkgName, boolean optional)
            throws ProvisioningDescriptionException {
        if(fpBuilder.hasPackage(pkgName)) {
            return;
        }
        if(fpConfig.isExcluded(pkgName)) {
            if(optional) {
                return;
            } else {
                throw new ProvisioningDescriptionException(Errors.requiredPackageExcluded(pkgName, fpDescr.getGav()));
            }
        }
        final PackageDescription pkgDescr = fpDescr.getPackage(pkgName);
        if (pkgDescr == null) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(pkgName));
        }
        fpBuilder.addPackage(pkgName);
        if (pkgDescr.hasLocalDependencies()) {
            for (PackageDependencyDescription dep : pkgDescr.getLocalDependencies().getDescriptions()) {
                final String depName = dep.getName();
                boolean optional1 = dep.isOptional();
                resolvePackage(fpDescr, fpConfig, fpBuilder, depName, optional1);
            }
        }
        if(pkgDescr.hasExternalDependencies() && !fpWithExternalDeps.containsKey(fpDescr.getGav())) {
            switch(fpWithExternalDeps.size()) {
                case 0:
                    fpWithExternalDeps = Collections.singletonMap(fpDescr.getGav(), fpDescr);
                    break;
                case 1:
                    fpWithExternalDeps = new HashMap<>(fpWithExternalDeps);
                default:
                    fpWithExternalDeps.put(fpDescr.getGav(), fpDescr);
            }
        }
    }
}
