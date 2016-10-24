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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDependencyDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;

/**
 * Installs feature pack content to the target directory.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackInstaller {

    private Path installDir;
    private Path fpPackagesDir;
    private FeaturePackDescription featurePack;
    private Set<String> installedPackages;
    private ProvisionedFeaturePackDescription provisionedFp;

    /**
     * Installs feature pack content to the specified directory.
     *
     * @param featurePack  full feature-pack description
     * @param provisionedFp  provisioned feature-pack description
     * @param fpDir  feature-pack source directory
     * @param installDir  target installation directory
     * @throws FeaturePackInstallException
     */
    public void install(FeaturePackDescription featurePack, ProvisionedFeaturePackDescription provisionedDescr, Path fpDir, Path installDir) throws FeaturePackInstallException {

        fpPackagesDir = fpDir.resolve(Constants.PACKAGES);
        this.installDir = installDir;
        this.featurePack = featurePack;
        installedPackages = new HashSet<String>();
        provisionedFp = provisionedDescr;

        if(provisionedFp.isIncludeDefault()) {
            for (String name : featurePack.getDefaultPackageNames()) {
                if (canBeInstalled(name, true)) {
                    install(featurePack.getPackageDescription(name));
                }
            }
        } else if(provisionedFp.hasIncludedPackages()) {
            for(String name : provisionedDescr.getIncludedPackages()) {
                if(canBeInstalled(name, false)) {
                    install(featurePack.getPackageDescription(name));
                }
            }
        }
    }

    private void install(PackageDescription pkg) throws FeaturePackInstallException {
        installedPackages.add(pkg.getName());
        if(pkg.hasDependencies()) {
            for(PackageDependencyDescription dep : pkg.getDependencies()) {
                if(canBeInstalled(dep.getName(), dep.isOptional())) {
                    final PackageDescription dependency = featurePack.getPackageDescription(dep.getName());
                    if(dependency == null) {
                        throw new FeaturePackInstallException(Errors.packageNotFound(dep.getName()));
                    }
                    install(dependency);
                }
            }
        }
        final Path pkgSrcDir = fpPackagesDir.resolve(pkg.getName()).resolve(Constants.CONTENT);
        if (Files.exists(pkgSrcDir)) {
            try {
                IoUtils.copy(pkgSrcDir, installDir);
            } catch (IOException e) {
                throw new FeaturePackInstallException(Errors.packageContentCopyFailed(pkg.getName()), e);
            }
        }
    }

    private boolean canBeInstalled(String packageName, boolean optional) throws FeaturePackInstallException {
        if(installedPackages.contains(packageName)) {
            return false;
        }
        if(provisionedFp.isExcluded(packageName)) {
            if(optional) {
                return false;
            } else {
                throw new FeaturePackInstallException(Errors.requiredPackageExcluded(packageName));
            }
        }
        return true;
    }
}
