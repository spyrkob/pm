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

package org.jboss.provisioning.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.parameters.ParameterResolver;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackLayoutDescription;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.spec.ParameterSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.state.ProvisionedState;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedStateResolver {

    private final ProvisionedState.Builder stateBuilder;
    private final FeaturePackLayoutDescription fpLayout;
    private final ProvisioningConfig extendedConfig;
    private final PackageParameterResolver paramResolver;

    private Map<ArtifactCoords.Ga, Fp> fps;

    public ProvisionedStateResolver(ProvisioningConfig provisioningConfig,
            FeaturePackLayoutDescription fpLayout,
            PackageParameterResolver paramResolver) {
        this.stateBuilder = ProvisionedState.builder();
        this.fpLayout = fpLayout;
        this.extendedConfig = provisioningConfig;
        this.paramResolver = paramResolver;
    }

    public ProvisionedState resolve() throws ProvisioningException {

        final int fpTotal = extendedConfig.getFeaturePacks().size();
        if(fpTotal == 1) {
            final ArtifactCoords.Ga fpGa = extendedConfig.getFeaturePackGaParts().iterator().next();
            fps = Collections.singletonMap(fpGa, new Fp(fpLayout.getFeaturePack(fpGa), extendedConfig.getFeaturePack(fpGa)));
        } else {
            fps = new HashMap<>(fpTotal);
        }

        for(ArtifactCoords.Ga ga: extendedConfig.getFeaturePackGaParts()) {
            resolveFeaturePack(ga);
        }

        for(ArtifactCoords.Ga fpGa : extendedConfig.getFeaturePackGaParts()) {
            stateBuilder.addFeaturePack(fps.get(fpGa).builder.build());
        }

        return stateBuilder.build();
    }

    private void resolveFeaturePack(ArtifactCoords.Ga ga)
            throws ProvisioningException {

        final Fp fp = getFp(ga);

        if(fp.config.isInheritPackages()) {
            for (String name : fp.spec.getDefaultPackageNames()) {
                resolvePackage(fp, name);
            }
        }
        if(fp.config.hasIncludedPackages()) {
            for(String name : fp.config.getIncludedPackages()) {
                if(!resolvePackage(fp, name)) {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, name));
                }
            }
        }
    }

    private boolean resolvePackage(Fp fp, final String pkgName) throws ProvisioningException {

        if(fp.isSkipResolution(pkgName)) {
            return true;
        }

        if(fp.config.isExcluded(pkgName)) {
            return false;
        }

        final PackageSpec pkgSpec = fp.spec.getPackage(pkgName);
        if (pkgSpec == null) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fp.gav, pkgName));
        }

        boolean hasDependencies = false;
        if (pkgSpec.hasLocalDependencies()) {
            hasDependencies = true;
            fp.setBeingResolved(pkgName);
            for (PackageDependencySpec dep : pkgSpec.getLocalDependencies().getDescriptions()) {
                final boolean resolved;
                try {
                    resolved = resolvePackage(fp, dep.getName());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
                if(!resolved && !dep.isOptional()) {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgName, dep.getName()));
                }
            }
        }
        if(pkgSpec.hasExternalDependencies()) {
            if(!hasDependencies) {
                hasDependencies = true;
                fp.setBeingResolved(pkgName);
            }
            for(String depName : pkgSpec.getExternalDependencyNames()) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final Fp targetFp = getFp(depSpec.getTarget().getGav().toGa());

                final PackageDependencyGroupSpec pkgDeps = pkgSpec.getExternalDependencies(depName);
                for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                    final boolean resolved;
                    try {
                        resolved = resolvePackage(targetFp, pkgDep.getName());
                    } catch(ProvisioningDescriptionException e) {
                        if(pkgDep.isOptional()) {
                            continue;
                        } else {
                            throw e;
                        }
                    }
                    if(!resolved && !pkgDep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedExternalPackageDependency(fp.gav, pkgName, targetFp.gav, pkgDep.getName()));
                    }
                }
            }
        }

        if(pkgSpec.hasParameters()) {
            if(paramResolver == null) {
                throw new ProvisioningException(Errors.packageParameterResolverNotProvided());
            }
            final ParameterResolver pkgParamResolver = paramResolver.getResolver(fp.gav, pkgName);
            if(pkgParamResolver == null) {
                throw new ProvisioningException(Errors.packageParameterResolverNotProvided(fp.gav, pkgName));
            }
            final ProvisionedPackage.Builder pkgBuilder = ProvisionedPackage.builder(pkgName);
            for(ParameterSpec paramSpec : pkgSpec.getParameters()) {
                pkgBuilder.addParameter(paramSpec.getName(), pkgParamResolver.resolve(paramSpec.getName(), paramSpec.getDefaultValue()));
            }
            fp.builder.addPackage(pkgBuilder.build());
        } else {
            fp.builder.addPackage(pkgName);
        }
        if(hasDependencies) {
            fp.setResolved(pkgName);
        }
        return true;
    }

    private Fp getFp(ArtifactCoords.Ga ga) {
        Fp fp = fps.get(ga);
        if(fp == null) {
            fp = new Fp(fpLayout.getFeaturePack(ga), extendedConfig.getFeaturePack(ga));
            fps.put(ga, fp);
        }
        return fp;
    }

    private static class Fp {
        final ArtifactCoords.Gav gav;
        final FeaturePackSpec spec;
        final FeaturePackConfig config;
        final ProvisionedFeaturePack.Builder builder;
        private Set<String> beingResolved = null;

        Fp(FeaturePackSpec fpSpec, FeaturePackConfig fpConfig) {
            gav = fpSpec.getGav();
            spec = fpSpec;
            config = fpConfig;
            builder = ProvisionedFeaturePack.builder(gav);
        }

        boolean isSkipResolution(String pkgName) throws ProvisioningDescriptionException {
            return builder.hasPackage(pkgName) || beingResolved != null && beingResolved.contains(pkgName);
        }

        void setBeingResolved(String pkgName) {
            if(beingResolved == null) {
                beingResolved = new HashSet<>();
            }
            beingResolved.add(pkgName);
        }

        void setResolved(String pkgName) {
            beingResolved.remove(pkgName);
        }
    }
}
