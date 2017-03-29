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

package org.jboss.provisioning.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.parameters.ParameterResolver;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackRuntime implements ProvisionedFeaturePack<PackageRuntime> {

    static class Builder {
        final ArtifactCoords.Gav gav;
        final Path dir;
        FeaturePackSpec spec;
        FeaturePackConfig config;

        Map<String, PackageRuntime.Builder> pkgBuilders = Collections.emptyMap();
        private List<String> pkgOrder = new ArrayList<>();

        private Builder(ArtifactCoords.Gav gav, Path dir) {
            this.gav = gav;
            this.dir = dir;
        }

        PackageRuntime.Builder newPackage(PackageSpec spec, Path dir) {
            final PackageRuntime.Builder pkgBuilder = PackageRuntime.builder(spec, dir);
            switch(pkgBuilders.size()) {
                case 0:
                    pkgBuilders = Collections.singletonMap(spec.getName(), pkgBuilder);
                    break;
                case 1:
                    pkgBuilders = new HashMap<>(pkgBuilders);
                default:
                    pkgBuilders.put(spec.getName(), pkgBuilder);
            }
            return pkgBuilder;
        }

        void addPackage(String name) {
            pkgOrder.add(name);
        }

        FeaturePackRuntime build(PackageParameterResolver paramResolver) throws ProvisioningException {
            return new FeaturePackRuntime(this, paramResolver);
        }
    }

    static Builder builder(ArtifactCoords.Gav gav, Path dir) {
        return new Builder(gav, dir);
    }

    private final FeaturePackSpec spec;
    private final FeaturePackConfig config;
    private final Path dir;
    private final Map<String, PackageRuntime> packages;

    private FeaturePackRuntime(Builder builder, PackageParameterResolver paramResolver) throws ProvisioningException {
        this.spec = builder.spec;
        this.config = builder.config;
        this.dir = builder.dir;

        Map<String, PackageRuntime> tmpPackages = new LinkedHashMap<>();
        for(String pkgName : builder.pkgOrder) {
            final PackageRuntime.Builder pkgRtBuilder = builder.pkgBuilders.get(pkgName);
            final PackageConfig pkgConfig = pkgRtBuilder.configBuilder.build();
            if(pkgConfig.hasParams()) {
                final ParameterResolver pkgParamResolver = paramResolver.getResolver(builder.gav, pkgName);
                if(pkgParamResolver == null) {
                    throw new ProvisioningException(Errors.packageParameterResolverNotProvided(builder.gav, pkgName));
                }
                for(PackageParameter param : pkgConfig.getParameters()) {
                    final String resolved = pkgParamResolver.resolve(param.getName(), param.getValue());
                    if(!param.getValue().equals(resolved)) {
                        param = PackageParameter.newInstance(param.getName(), resolved);
                    }
                    pkgRtBuilder.addParameter(param);
                }
            }
            tmpPackages.put(pkgName, pkgRtBuilder.build());
        }

        packages = Collections.unmodifiableMap(tmpPackages);
    }

    public FeaturePackSpec getSpec() {
        return spec;
    }

    public FeaturePackConfig getConfig() {
        return config;
    }

    @Override
    public Gav getGav() {
        return spec.getGav();
    }

    @Override
    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    @Override
    public boolean containsPackage(String name) {
        return packages.containsKey(name);
    }

    @Override
    public Set<String> getPackageNames() {
        return packages.keySet();
    }

    @Override
    public Collection<PackageRuntime> getPackages() {
        return packages.values();
    }

    @Override
    public PackageRuntime getPackage(String name) {
        return packages.get(name);
    }

    /**
     * Returns a resource path for a feature-pack.
     *
     * @param fpGav  GAV of the feature-pack
     * @param path  path to the resource relative to the feature-pack resources directory
     * @return  file-system path for the resource
     * @throws ProvisioningDescriptionException  in case the feature-pack was not found in the layout
     */
    public Path getResource(String... path) throws ProvisioningDescriptionException {
        if(path.length == 0) {
            throw new IllegalArgumentException("Resource path is null");
        }
        if(path.length == 1) {
            return dir.resolve(Constants.RESOURCES).resolve(path[0]);
        }
        Path p = dir.resolve(Constants.RESOURCES);
        for(String name : path) {
            p = p.resolve(name);
        }
        return p;
    }
}
