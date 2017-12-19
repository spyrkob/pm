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
import org.jboss.provisioning.config.ConfigId;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.state.FeaturePack;
import org.jboss.provisioning.type.ParameterTypeProvider;
import org.jboss.provisioning.type.builtin.BuiltInParameterTypeProvider;
import org.jboss.provisioning.util.PmCollections;
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
        private Map<String, ResolvedFeatureSpec> featureSpecs = null;
        private Map<String, FeatureGroup> fgSpecs = null;

        Map<String, PackageRuntime.Builder> pkgBuilders = Collections.emptyMap();
        private List<String> pkgOrder = new ArrayList<>();

        private List<FeaturePackConfig> fpConfigStack = Collections.emptyList();
        private List<List<FeaturePackConfig>> recordedStacks = Collections.emptyList();
        private FeaturePackConfig blockedPackageInheritance;
        private FeaturePackConfig blockedConfigInheritance;

        ParameterTypeProvider featureParamTypeProvider = BuiltInParameterTypeProvider.getInstance();

        private Builder(ArtifactCoords.Gav gav, FeaturePackSpec spec, Path dir) {
            this.gav = gav;
            this.dir = dir;
            this.spec = spec;
        }

        PackageRuntime.Builder newPackage(String name, Path dir) {
            final PackageRuntime.Builder pkgBuilder = PackageRuntime.builder(name, dir);
            pkgBuilders = PmCollections.put(pkgBuilders, name, pkgBuilder);
            return pkgBuilder;
        }

        void addPackage(String name) {
            pkgOrder.add(name);
        }

        FeatureGroup getFeatureGroupSpec(String name) throws ProvisioningException {
            FeatureGroup fgSpec = null;
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
                    throw new ProvisioningException(Errors.parseXml(specXml), e);
                }
                fgSpecs.put(name, fgSpec);
            }
            return fgSpec;
        }

        ResolvedFeatureSpec getFeatureSpec(String name) throws ProvisioningException {
            ResolvedFeatureSpec resolvedSpec = null;
            if(featureSpecs == null) {
                featureSpecs = new HashMap<>();
            } else {
                resolvedSpec = featureSpecs.get(name);
            }
            if(resolvedSpec == null) {
                final Path specXml = dir.resolve(Constants.FEATURES).resolve(name).resolve(Constants.SPEC_XML);
                if(!Files.exists(specXml)) {
                    throw new ProvisioningDescriptionException("Failed to locate feature spec '" + name + "' in " + gav);
                }
                final FeatureSpec xmlSpec;
                try(BufferedReader reader = Files.newBufferedReader(specXml)) {
                    xmlSpec = FeatureSpecXmlParser.getInstance().parse(reader);
                } catch (Exception e) {
                    throw new ProvisioningDescriptionException(Errors.parseXml(specXml), e);
                }

                resolvedSpec = new ResolvedFeatureSpec(new ResolvedSpecId(gav, xmlSpec.getName()), featureParamTypeProvider, xmlSpec);
                featureSpecs.put(name, resolvedSpec);
            }
            return resolvedSpec;
        }

        boolean isInheritPackages() {
            return blockedPackageInheritance == null;
        }

        boolean isInheritConfigs() {
            return blockedConfigInheritance == null;
        }

        boolean isStackEmpty() {
            return fpConfigStack.isEmpty();
        }

        void push(FeaturePackConfig fpConfig) {
            fpConfigStack = PmCollections.add(fpConfigStack, fpConfig);
            if(blockedPackageInheritance == null && !fpConfig.isInheritPackages()) {
                blockedPackageInheritance = fpConfig;
            }
            if(blockedConfigInheritance == null && !fpConfig.isInheritConfigs()) {
                blockedConfigInheritance = fpConfig;
            }
        }

        FeaturePackConfig pop() {
            final FeaturePackConfig popped;
            if (fpConfigStack.size() == 1) {
                popped = fpConfigStack.get(0);
                fpConfigStack = Collections.emptyList();
            } else {
                popped = fpConfigStack.remove(fpConfigStack.size() - 1);
                if (fpConfigStack.size() == 1) {
                    fpConfigStack = Collections.singletonList(fpConfigStack.get(0));
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


        boolean isPackageIncluded(String packageName) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = fpConfigStack.get(i--);
                if(fpConfig.isPackageIncluded(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean isPackageExcluded(String packageName) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                if(fpConfigStack.get(i--).isPackageExcluded(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean isModelOnlyConfigExcluded(ConfigId configId) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = fpConfigStack.get(i--);
                if(fpConfig.isConfigModelExcluded(configId)) {
                    return true;
                }
                if (!fpConfig.isInheritModelOnlyConfigs()) {
                    return !fpConfig.isConfigModelIncluded(configId);
                }
            }
            return false;
        }

        boolean isConfigExcluded(ConfigId configId) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = fpConfigStack.get(i--);
                if (fpConfig.isConfigExcluded(configId)) {
                    return true;
                }
                if(fpConfig.isConfigModelExcluded(configId)) {
                    return !fpConfig.isConfigIncluded(configId);
                }
                if (!fpConfig.isInheritConfigs()) {
                    return !fpConfig.isConfigModelIncluded(configId) && !fpConfig.isConfigIncluded(configId);
                }
            }
            return false;
        }

        boolean isModelOnlyConfigIncluded(ConfigId config) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = fpConfigStack.get(i--);
                if(fpConfig.isConfigModelIncluded(config)) {
                    return true;
                }
                if(fpConfig.isInheritModelOnlyConfigs()) {
                    return !fpConfig.isConfigModelExcluded(config);
                }
            }
            return false;
        }

        boolean isConfigIncluded(ConfigId config) {
            int i = fpConfigStack.size() - 1;
            while(i >= 0) {
                final FeaturePackConfig fpConfig = fpConfigStack.get(i--);
                if(fpConfig.isConfigIncluded(config)) {
                    return true;
                }
                if(fpConfig.isConfigModelIncluded(config)) {
                    return !fpConfig.isConfigExcluded(config);
                }
                if(fpConfig.isInheritConfigs()) {
                    return !fpConfig.isConfigModelExcluded(config) && !fpConfig.isConfigExcluded(config);
                }
            }
            return false;
        }

        void recordConfigStack() {
            final List<FeaturePackConfig> copy;
            if(fpConfigStack.isEmpty()) {
                copy = Collections.emptyList();
            } else if(fpConfigStack.size() == 1) {
                copy = Collections.singletonList(fpConfigStack.get(0));
            } else {
                copy = new ArrayList<>(fpConfigStack.size());
                for(int i = 0; i < copy.size(); ++i) {
                    copy.add(fpConfigStack.get(i));
                }
            }
            recordedStacks = PmCollections.add(recordedStacks, copy);
        }

        void activateConfigStack(int i) throws ProvisioningException {
            if(recordedStacks.size() <= i) {
                throw new ProvisioningException("Stack index " + i + " is exceeding the current stack size " + recordedStacks.size());
            }
            blockedPackageInheritance = null;
            blockedConfigInheritance = null;
            final List<FeaturePackConfig> stack = recordedStacks.get(i);
            for(int j = 0; j < stack.size(); ++j) {
                push(stack.get(j));
            }
        }

        FeaturePackRuntime build() throws ProvisioningException {
            return new FeaturePackRuntime(this);
        }
    }

    static Builder builder(ArtifactCoords.Gav gav, FeaturePackSpec spec, Path dir) {
        return new Builder(gav, spec, dir);
    }

    private final FeaturePackSpec spec;
    private final Path dir;
    private final Map<String, PackageRuntime> packages;
    private final Map<String, ResolvedFeatureSpec> featureSpecs;

    private FeaturePackRuntime(Builder builder) throws ProvisioningException {
        this.spec = builder.spec;
        this.dir = builder.dir;
        this.featureSpecs = builder.featureSpecs;

        Map<String, PackageRuntime> tmpPackages = new LinkedHashMap<>();
        for(String pkgName : builder.pkgOrder) {
            final PackageRuntime.Builder pkgRtBuilder = builder.pkgBuilders.get(pkgName);
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

    public Set<String> getFeatureSpecNames() {
        return featureSpecs.keySet();
    }

    public Collection<ResolvedFeatureSpec> getFeatureSpecs() {
        return featureSpecs.values();
    }

    public ResolvedFeatureSpec getFeatureSpec(String name) {
        return featureSpecs.get(name);
    }

    /**
     * Returns a resource path for a feature-pack.
     *
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
