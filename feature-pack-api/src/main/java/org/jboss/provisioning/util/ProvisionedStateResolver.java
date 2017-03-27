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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.parameters.ParameterResolver;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackLayoutDescription;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
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
    private final ProvisioningConfig userConfig;
    private final ProvisioningConfig expandedConfig;
    private final PackageParameterResolver paramResolver;

    private Map<ArtifactCoords.Ga, Fp> fps;

    public ProvisionedStateResolver(ProvisioningConfig userConfig,
            ProvisioningConfig expandedConfig,
            FeaturePackLayoutDescription fpLayout,
            PackageParameterResolver paramResolver) {
        this.stateBuilder = ProvisionedState.builder();
        this.fpLayout = fpLayout;
        this.userConfig = userConfig;
        this.expandedConfig = expandedConfig;
        this.paramResolver = paramResolver;
    }

    public ProvisionedState resolve() throws ProvisioningException {

        final int fpTotal = expandedConfig.getFeaturePacks().size();
        if(fpTotal == 1) {
            final ArtifactCoords.Ga fpGa = expandedConfig.getFeaturePackGaParts().iterator().next();
            fps = Collections.singletonMap(fpGa, new Fp(fpLayout.getFeaturePack(fpGa), expandedConfig.getFeaturePack(fpGa)));
        } else {
            fps = new HashMap<>(fpTotal);
        }

        for(ArtifactCoords.Ga ga: expandedConfig.getFeaturePackGaParts()) {
            resolveFeaturePack(ga);
        }

        // set parameters set by the user in feature-pack configs
        if(userConfig.hasFeaturePacks()) {
            for(FeaturePackConfig userFpConfig : userConfig.getFeaturePacks()) {
                if(userFpConfig.hasIncludedPackages()) {
                    Fp fp = null;
                    for(PackageConfig userPkg : userFpConfig.getIncludedPackages()) {
                        if(userPkg.hasParams()) {
                            if(fp == null) {
                                fp = fps.get(userFpConfig.getGav().toGa());
                            }
                            final PackageConfig.Builder pkgBuilder = fp.pkgConfigs.get(userPkg.getName());
                            for(PackageParameter param : userPkg.getParameters()) {
                                pkgBuilder.addParameter(param);
                            }
                        }
                    }
                }
            }
        }

        for(ArtifactCoords.Ga fpGa : expandedConfig.getFeaturePackGaParts()) {
            final Fp fp = fps.get(fpGa);
            final ProvisionedFeaturePack.Builder fpBuilder = ProvisionedFeaturePack.builder(fp.gav);
            for(String pkgName : fp.pkgOrder) {
                final PackageConfig pkgConfig = fp.pkgConfigs.get(pkgName).build();
                if(pkgConfig.hasParams()) {
                    final ParameterResolver pkgParamResolver = paramResolver.getResolver(fp.gav, pkgName);
                    if(pkgParamResolver == null) {
                        throw new ProvisioningException(Errors.packageParameterResolverNotProvided(fp.gav, pkgName));
                    }
                    final ProvisionedPackage.Builder ppBuilder = ProvisionedPackage.builder(pkgConfig.getName());
                    for(PackageParameter param : pkgConfig.getParameters()) {
                        ppBuilder.addParameter(param.getName(), pkgParamResolver.resolve(param.getName(), param.getValue()));
                    }
                    fpBuilder.addPackage(ppBuilder.build());
                } else {
                    fpBuilder.addPackage(pkgConfig.getName());
                }
            }
            stateBuilder.addFeaturePack(fpBuilder.build());
        }

        return stateBuilder.build();
    }

    private void resolveFeaturePack(ArtifactCoords.Ga ga)
            throws ProvisioningException {

        final Fp fp = getFp(ga);

        if(fp.config.isInheritPackages()) {
            for (String name : fp.spec.getDefaultPackageNames()) {
                if(!fp.config.isIncluded(name)) {
                    // spec parameters are set first
                    resolvePackage(fp, name, Collections.emptyList());
                }
            }
        }
        if(fp.config.hasIncludedPackages()) {
            for(PackageConfig packageConfig : fp.config.getIncludedPackages()) {
                // user parameters are set last
                if(!resolvePackage(fp, packageConfig.getName(), Collections.emptyList())) {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, packageConfig.getName()));
                }
            }
        }
        // set parameters set in feature-pack dependencies
        if(fp.spec.hasDependencies()) {
            for(FeaturePackDependencySpec depSpec : fp.spec.getDependencies()) {
                final FeaturePackConfig depConfig = depSpec.getTarget();
                Fp dep = null;
                if(depConfig.hasIncludedPackages()) {
                    for(PackageConfig pkgConfig : depConfig.getIncludedPackages()) {
                        if(pkgConfig.hasParams()) {
                            if(dep == null) {
                                dep = getFp(depConfig.getGav().toGa());
                            }
                            final PackageConfig.Builder pkgBuilder = dep.pkgConfigs.get(pkgConfig.getName());
                            for(PackageParameter param : pkgConfig.getParameters()) {
                                pkgBuilder.addParameter(param);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean resolvePackage(Fp fp, final String pkgName, Collection<PackageParameter> params) throws ProvisioningException {

        if(fp.config.isExcluded(pkgName)) {
            return false;
        }

        PackageConfig.Builder pkgBuilder = fp.pkgConfigs.get(pkgName);
        if(pkgBuilder != null) {
            if(!params.isEmpty()) {
                for(PackageParameter param : params) {
                    pkgBuilder.addParameter(param);
                }
            }
            return true;
        }

        final PackageSpec pkgSpec = fp.spec.getPackage(pkgName);
        if (pkgSpec == null) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fp.gav, pkgName));
        }
        pkgBuilder = PackageConfig.builder(pkgName);
        // set parameters set in the package spec first
        if(pkgSpec.hasParameters()) {
            for(PackageParameter param : pkgSpec.getParameters()) {
                pkgBuilder.addParameter(param);
            }
        }
        fp.registerBuilder(pkgName, pkgBuilder);

        if (pkgSpec.hasLocalDependencies()) {
            for (PackageDependencySpec dep : pkgSpec.getLocalDependencies().getDescriptions()) {
                final boolean resolved;
                try {
                    resolved = resolvePackage(fp, dep.getName(), dep.getParameters());
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
            for(String depName : pkgSpec.getExternalDependencyNames()) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final Fp targetFp = getFp(depSpec.getTarget().getGav().toGa());

                final PackageDependencyGroupSpec pkgDeps = pkgSpec.getExternalDependencies(depName);
                for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                    final boolean resolved;
                    try {
                        resolved = resolvePackage(targetFp, pkgDep.getName(), pkgDep.getParameters());
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

        if(!params.isEmpty()) {
            for(PackageParameter param : params) {
                pkgBuilder.addParameter(param);
            }
        }
        fp.addPackage(pkgName);
        return true;
    }

    private Fp getFp(ArtifactCoords.Ga ga) {
        Fp fp = fps.get(ga);
        if(fp == null) {
            fp = new Fp(fpLayout.getFeaturePack(ga), expandedConfig.getFeaturePack(ga));
            fps.put(ga, fp);
        }
        return fp;
    }

    private static class Fp {
        final ArtifactCoords.Gav gav;
        final FeaturePackSpec spec;
        final FeaturePackConfig config;
        Map<String, PackageConfig.Builder> pkgConfigs = Collections.emptyMap();
        List<String> pkgOrder = new ArrayList<>();

        Fp(FeaturePackSpec fpSpec, FeaturePackConfig fpConfig) {
            gav = fpSpec.getGav();
            spec = fpSpec;
            config = fpConfig;
        }

        void registerBuilder(String name, PackageConfig.Builder pkgBuilder) {
            switch(pkgConfigs.size()) {
                case 0:
                    pkgConfigs = Collections.singletonMap(name, pkgBuilder);
                    break;
                case 1:
                    pkgConfigs = new HashMap<>(pkgConfigs);
                default:
                    pkgConfigs.put(name, pkgBuilder);
            }
        }

        void addPackage(String name) {
            pkgOrder.add(name);
        }
    }
}
