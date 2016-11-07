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
package org.jboss.provisioning.util.analyzer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.LayoutUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class FeaturePackPackageView {

    static class ResolvedPackage {
        private final String name;
        private final ArtifactCoords.Gav fpGav;
        private final PackageSpec spec;

        ResolvedPackage(String name, ArtifactCoords.Gav fpGav, PackageSpec spec) {
            super();
            this.name = name;
            this.fpGav = fpGav;
            this.spec = spec;
        }

        String getName() {
            return name;
        }

        ArtifactCoords.Gav getGav() {
            return fpGav;
        }

        PackageSpec getSpec() {
            return spec;
        }
    }

    static Map<String, ResolvedPackage> resolve(Path fpLayoutDir, String encoding, ArtifactCoords.Gav fpGav) throws ProvisioningDescriptionException {
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, fpGav);
        return resolve(fpLayoutDir, encoding, FeaturePackLayoutDescriber.describeFeaturePack(fpDir, encoding));
    }

    static Map<String, ResolvedPackage> resolve(Path fpLayoutDir, String encoding, FeaturePackSpec fpSpec) throws ProvisioningDescriptionException {
        final HashMap<String, ResolvedPackage> packages = new HashMap<String, ResolvedPackage>();
        resolveFeaturePack(fpLayoutDir, encoding, fpSpec, packages, Collections.emptySet());
        return packages;
    }

    private static void resolveFeaturePack(Path fpLayoutDir, String encoding, FeaturePackSpec fpSpec,
            Map<String, ResolvedPackage> collectedPackages, Set<String> excludePackages) throws ProvisioningDescriptionException {
        if (fpSpec.hasDependencies()) {
            for (FeaturePackDependencySpec dep : fpSpec.getDependencies()) {
                final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, dep.getTarget().getGav());
                resolveFeaturePack(fpLayoutDir, encoding,
                        FeaturePackLayoutDescriber.describeFeaturePack(fpDir, encoding),
                        collectedPackages, dep.getTarget().getExcludedPackages());
            }
        }
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, fpSpec.getGav());
        for (String name : fpSpec.getPackageNames()) {
            if (!excludePackages.contains(name)) {
                //if(Files.exists(LayoutUtils.getPackageContentDir(fpDir, name))) {
                    collectedPackages.put(name, new ResolvedPackage(name, fpSpec.getGav(), fpSpec.getPackage(name)));
                //}
            }
        }
    }
}
