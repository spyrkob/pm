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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.util.PmCollections;

/**
 * This class describes a layout of feature-packs from which
 * the target installation is provisioned.
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningLayout {

    public static class Builder {

        private Map<ArtifactCoords.Ga, FeaturePackLayout> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        public Builder addFeaturePack(FeaturePackLayout fp) throws ProvisioningDescriptionException {
            assert fp != null : "fp is null";
            final ArtifactCoords.Ga fpGa = fp.getGav().toGa();
            if(featurePacks.containsKey(fpGa)) {
                final Gav existingGav = featurePacks.get(fpGa).getGav();
                if(existingGav.getVersion().equals(fp.getGav().getVersion())) {
                    return this;
                }
                throw new ProvisioningDescriptionException(Errors.featurePackVersionConflict(fp.getGav(), existingGav));
            }
            featurePacks = PmCollections.put(featurePacks, fpGa, fp);
            return this;
        }

        public ProvisioningLayout build() throws ProvisioningDescriptionException {
            for(FeaturePackLayout fp : featurePacks.values()) {
                if(fp.hasExternalPackageDependencies()) {
                    final FeaturePackSpec fpSpec = fp.getSpec();
                    for(PackageSpec pkg : fp.getPackages()) {
                        if(pkg.hasExternalPackageDeps()) {
                            for(String depName : pkg.getExternalPackageSources()) {
                                final FeaturePackDependencySpec fpDepSpec;
                                try {
                                    fpDepSpec = fpSpec.getDependency(depName);
                                } catch(ProvisioningDescriptionException e) {
                                    throw new ProvisioningDescriptionException(Errors.unknownFeaturePackDependencyName(fpSpec.getGav(), pkg.getName(), depName), e);
                                }
                                final FeaturePackConfig fpDepConfig = fpDepSpec.getTarget();
                                final FeaturePackLayout fpDepLayout = featurePacks.get(fpDepConfig.getGav().toGa());
                                if(fpDepLayout == null) {
                                    throw new ProvisioningDescriptionException(Errors.unknownFeaturePack(fpDepConfig.getGav()));
                                }
                                for(PackageDependencySpec pkgDep : pkg.getExternalPackageDeps(depName)) {
                                    final String pkgDepName = pkgDep.getName();
                                    if(!fpDepLayout.hasPackage(pkgDepName)) {
                                        throw new ProvisioningDescriptionException(
                                                Errors.unsatisfiedExternalPackageDependency(fpSpec.getGav(), pkg.getName(), fpDepConfig.getGav(), pkgDep.getName()));
                                    }
                                    if(fpDepConfig.isPackageExcluded(pkgDepName) && !pkgDep.isOptional()) {
                                        throw new ProvisioningDescriptionException(
                                                Errors.unsatisfiedExternalPackageDependency(fpSpec.getGav(), pkg.getName(), fpDepConfig.getGav(), pkgDep.getName()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new ProvisioningLayout(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ArtifactCoords.Ga, FeaturePackLayout> featurePacks;

    private ProvisioningLayout(Builder builder) {
        featurePacks = PmCollections.unmodifiable(builder.featurePacks);
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public boolean contains(ArtifactCoords.Gav fpGav) {
        return featurePacks.containsKey(fpGav.toGa());
    }

    public FeaturePackLayout getFeaturePack(ArtifactCoords.Gav fpGav) {
        return featurePacks.get(fpGav.toGa());
    }

    public Collection<FeaturePackLayout> getFeaturePacks() {
        return featurePacks.values();
    }
}
