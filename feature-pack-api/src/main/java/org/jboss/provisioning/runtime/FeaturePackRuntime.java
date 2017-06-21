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

import java.io.BufferedReader;
import java.nio.file.Files;
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
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.FeatureGroupSpec;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.parameters.ParameterResolver;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.FeaturePack;
import org.jboss.provisioning.xml.FeatureGroupXmlParser;
import org.jboss.provisioning.xml.FeatureSpecXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackRuntime implements FeaturePack<PackageRuntime> {

    static class Builder {
        final ArtifactCoords.Gav gav;
        final Path dir;
        final FeaturePackSpec spec;
        boolean ordered;
        private Map<String, FeatureSpec> featureSpecs = null;
        private Map<String, FeatureGroupSpec> fgSpecs = null;

        Map<String, PackageRuntime.Builder> pkgBuilders = Collections.emptyMap();
        private List<String> pkgOrder = new ArrayList<>();

        private List<FeaturePackConfig> stack = Collections.emptyList();
        private FeaturePackConfig blockedPackageInheritance;
        private FeaturePackConfig blockedConfigInheritance;

        private Builder(ArtifactCoords.Gav gav, FeaturePackSpec spec, Path dir) {
            this.gav = gav;
            this.dir = dir;
            this.spec = spec;
        }

        PackageRuntime.Builder newPackage(String name, Path dir) {
            final PackageRuntime.Builder pkgBuilder = PackageRuntime.builder(name, dir);
            switch(pkgBuilders.size()) {
                case 0:
                    pkgBuilders = Collections.singletonMap(name, pkgBuilder);
                    break;
                case 1:
                    pkgBuilders = new HashMap<>(pkgBuilders);
                default:
                    pkgBuilders.put(name, pkgBuilder);
            }
            return pkgBuilder;
        }

        void addPackage(String name) {
            pkgOrder.add(name);
        }

        FeatureGroupSpec getFeatureGroupSpec(String name) throws ProvisioningException {
            FeatureGroupSpec fgSpec = null;
            if(fgSpecs == null) {
                fgSpecs = new HashMap<>();
            } else {
                fgSpec = fgSpecs.get(name);
            }
            if(fgSpec == null) {
                final Path specXml = dir.resolve(Constants.FEATURE_GROUPS).resolve(name + ".xml");
                if(!Files.exists(specXml)) {
                    throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(specXml));
                }
                try(BufferedReader reader = Files.newBufferedReader(specXml)) {
                    fgSpec = FeatureGroupXmlParser.getInstance().parse(reader);
                } catch (Exception e) {
                    throw new ProvisioningException(Errors.parseXml(specXml));
                }
                fgSpecs.put(name, fgSpec);
            }
            return fgSpec;
        }

        FeatureSpec getFeatureSpec(String name) throws ProvisioningException {
            FeatureSpec spec = null;
            if(featureSpecs == null) {
                featureSpecs = new HashMap<>();
            } else {
                spec = featureSpecs.get(name);
            }
            if(spec == null) {
                final Path specXml = dir.resolve(Constants.FEATURES).resolve(name).resolve(Constants.SPEC_XML);
                if(!Files.exists(specXml)) {
                    throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(specXml));
                }
                try(BufferedReader reader = Files.newBufferedReader(specXml)) {
                    spec = FeatureSpecXmlParser.getInstance().parse(reader);
                } catch (Exception e) {
                    throw new ProvisioningException(Errors.parseXml(specXml));
                }
                featureSpecs.put(name, spec);
            }
            return spec;
        }

        boolean isInheritPackages() {
            return blockedPackageInheritance == null;
        }

        boolean isInheritConfigs() {
            return blockedConfigInheritance == null;
        }

        boolean isStackEmpty() {
            return stack.isEmpty();
        }

        void push(FeaturePackConfig fpConfig) {
            if(stack.isEmpty()) {
                stack = Collections.singletonList(fpConfig);
            } else {
                if(stack.size() == 1) {
                    final FeaturePackConfig first = stack.get(0);
                    stack = new ArrayList<>(2);
                    stack.add(first);
                }
                stack.add(fpConfig);
            }
            if(blockedPackageInheritance == null && !fpConfig.isInheritPackages()) {
                blockedPackageInheritance = fpConfig;
            }
            if(blockedConfigInheritance == null && !fpConfig.isInheritConfigs()) {
                blockedConfigInheritance = fpConfig;
            }
        }

        FeaturePackConfig pop() {
            final FeaturePackConfig popped;
            if (stack.size() == 1) {
                popped = stack.get(0);
                stack = Collections.emptyList();
            } else {
                popped = stack.remove(stack.size() - 1);
                if (stack.size() == 1) {
                    stack = Collections.singletonList(stack.get(0));
                }
            }
            if(popped == blockedPackageInheritance) {
                blockedPackageInheritance = null;
            }
            if(popped == blockedConfigInheritance) {
                blockedConfigInheritance = null;
            }
            return popped;
        }

        boolean isPackageIncluded(String packageName, Collection<PackageParameter> params) {
            int i = stack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = stack.get(i--);
                final PackageConfig stackedPkg = fpConfig.getIncludedPackage(packageName);
                if(stackedPkg != null) {
                    if(!params.isEmpty()) {
                        boolean allParamsOverwritten = true;
                        for(PackageParameter param : params) {
                            if(stackedPkg.getParameter(param.getName()) == null) {
                                allParamsOverwritten = false;
                                break;
                            }
                        }
                        if(allParamsOverwritten) {
                            return true;
                        }
                    } else {
                       return true;
                    }
                }
            }
            return false;
        }

        boolean isPackageExcluded(String packageName) {
            int i = stack.size() - 1;
            while(i >= 0) {
                if(stack.get(i--).isExcluded(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean isConfigExcluded(Config config) {
            int i = stack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = stack.get(i--);
                if (fpConfig.isConfigExcluded(config.getModel(), config.getName())) {
                    return true;
                }
                if(fpConfig.isFullModelExcluded(config.getModel())) {
                    return !fpConfig.isConfigIncluded(config.getModel(), config.getName());
                }
                if (!fpConfig.isInheritConfigs()) {
                    return !fpConfig.isFullModelIncluded(config.getModel()) && !fpConfig.isConfigIncluded(config.getModel(), config.getName());
                }
            }
            return false;
        }

        boolean isConfigIncluded(Config config) {
            int i = stack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = stack.get(i--);
                if(fpConfig.isConfigIncluded(config.getModel(), config.getName())) {
                    return true;
                }
                if(fpConfig.isFullModelIncluded(config.getModel())) {
                    return !fpConfig.isConfigExcluded(config.getModel(), config.getName());
                }
                if(fpConfig.isInheritConfigs()) {
                    return !fpConfig.isFullModelExcluded(config.getModel()) && !fpConfig.isConfigExcluded(config.getModel(), config.getName());
                }
            }
            return false;
        }

        FeaturePackRuntime build(PackageParameterResolver paramResolver) throws ProvisioningException {
            return new FeaturePackRuntime(this, paramResolver);
        }
    }

    static Builder builder(ArtifactCoords.Gav gav, FeaturePackSpec spec, Path dir) {
        return new Builder(gav, spec, dir);
    }

    private final FeaturePackSpec spec;
    private final Path dir;
    private final Map<String, PackageRuntime> packages;

    private FeaturePackRuntime(Builder builder, PackageParameterResolver paramResolver) throws ProvisioningException {
        this.spec = builder.spec;
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
