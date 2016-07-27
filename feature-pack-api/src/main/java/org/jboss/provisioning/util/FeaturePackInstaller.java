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

package org.jboss.provisioning.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;

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

    /**
     * Installs feature pack content to the specified directory.
     *
     * @param featurePack  feature-pack description
     * @param fpDir  feature-pack source directory
     * @param installDir  target installation directory
     * @throws FeaturePackInstallException
     */
    public void install(FeaturePackDescription featurePack, Path fpDir, Path installDir) throws FeaturePackInstallException {

        fpPackagesDir = fpDir.resolve(Constants.PACKAGES);
        this.installDir = installDir;
        this.featurePack = featurePack;
        installedPackages = new HashSet<String>();

        for(String name : featurePack.getTopPackageNames()) {
            if(!isPackageInstalled(name)) {
                install(featurePack.getPackageDescription(name));
            }
        }
    }

    private void install(PackageDescription pkg) throws FeaturePackInstallException {
        installedPackages.add(pkg.getName());
        if(pkg.hasDependencies()) {
            for(String name : pkg.getDependencies()) {
                if(!isPackageInstalled(name)) {
                    final PackageDescription dependency = featurePack.getPackageDescription(name);
                    if(dependency == null) {
                        throw new FeaturePackInstallException(Errors.packageNotFound(name));
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

    private boolean isPackageInstalled(String name) {
        return installedPackages.contains(name);
    }
}
