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

package org.jboss.provisioning.util.analyzer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;

/**
 *
 * @author Alexey Loubyansky
 */
class FeaturePackPackageView {

    static class ResolvedPackage {
        private final String name;
        private final GAV fpGav;
        private final PackageDescription descr;

        ResolvedPackage(String name, GAV fpGav, PackageDescription descr) {
            super();
            this.name = name;
            this.fpGav = fpGav;
            this.descr = descr;
        }

        String getName() {
            return name;
        }

        GAV getGAV() {
            return fpGav;
        }

        PackageDescription getDescription() {
            return descr;
        }
    }

    static Map<String, ResolvedPackage> resolve(Path fpLayoutDir, GAV fpGav) throws InstallationDescriptionException {
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, fpGav);
        return resolve(fpLayoutDir, FeaturePackLayoutDescriber.describeFeaturePack(fpDir));
    }

    static Map<String, ResolvedPackage> resolve(Path fpLayoutDir, FeaturePackDescription fpDescr) throws InstallationDescriptionException {
        final HashMap<String, ResolvedPackage> packages = new HashMap<String, ResolvedPackage>();
        resolveFeaturePack(fpLayoutDir, fpDescr, packages, Collections.emptySet());
        return packages;
    }

    private static void resolveFeaturePack(Path fpLayoutDir, FeaturePackDescription fpDescr,
            Map<String, ResolvedPackage> collectedPackages, Set<String> excludePackages) throws InstallationDescriptionException {
        if (fpDescr.hasDependencies()) {
            for (FeaturePackDependencyDescription dep : fpDescr.getDependencies()) {
                final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, dep.getGAV());
                resolveFeaturePack(fpLayoutDir, FeaturePackLayoutDescriber.describeFeaturePack(fpDir), collectedPackages, dep.getExcludedPackages());
            }
        }
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, fpDescr.getGAV());
        for (String name : fpDescr.getPackageNames()) {
            if (!excludePackages.contains(name)) {
                //if(Files.exists(LayoutUtils.getPackageContentDir(fpDir, name))) {
                    collectedPackages.put(name, new ResolvedPackage(name, fpDescr.getGAV(), fpDescr.getPackageDescription(name)));
                //}
            }
        }
    }
}
