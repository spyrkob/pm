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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlParser;
import org.jboss.provisioning.xml.PackageXmlParser;


/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningRuntimeBuilder {

    public static ProvisioningRuntimeBuilder newInstance() {
        return new ProvisioningRuntimeBuilder();
    }

    final long startTime;
    String encoding;
    ArtifactResolver artifactResolver;
    ProvisioningConfig config;
    PackageParameterResolver paramResolver;
    Path installDir;
    final Path workDir;
    final Path layoutDir;
    Path pluginsDir = null;

    private final Map<ArtifactCoords.Ga, FeaturePackRuntime.Builder> fpRtBuilders = new HashMap<>();
    private List<FeaturePackRuntime.Builder> orderedRtBuilders = new ArrayList<>();
    private Map<ArtifactCoords.Gav, List<FeaturePackConfig>> fpConfigStacks = new HashMap<>();
    Map<ArtifactCoords.Gav, FeaturePackRuntime> fpRuntimes;

    private ProvisioningRuntimeBuilder() {
        startTime = System.currentTimeMillis();
        workDir = IoUtils.createRandomTmpDir();
        layoutDir = workDir.resolve("layout");
    }

    public ProvisioningRuntimeBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public ProvisioningRuntimeBuilder setArtifactResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder setConfig(ProvisioningConfig config) {
        this.config = config;
        return this;
    }

    public ProvisioningRuntimeBuilder setParameterResolver(PackageParameterResolver paramResolver) {
        this.paramResolver = paramResolver;
        return this;
    }

    public ProvisioningRuntimeBuilder setInstallDir(Path installDir) {
        this.installDir = installDir;
        return this;
    }

    public ProvisioningRuntime build() throws ProvisioningException {

        final Collection<FeaturePackConfig> fpConfigs = config.getFeaturePacks();
        for (FeaturePackConfig fpConfig : fpConfigs) {
            fpConfigStacks.put(fpConfig.getGav(), Collections.singletonList(fpConfig));
        }
        for (FeaturePackConfig fpConfig : fpConfigs) {
            processConfig(fpConfig);
        }

        switch(orderedRtBuilders.size()) {
            case 0: {
                fpRuntimes = Collections.emptyMap();
                break;
            }
            case 1: {
                final FeaturePackRuntime.Builder builder = orderedRtBuilders.get(0);
                copyResources(builder);
                fpRuntimes = Collections.singletonMap(builder.gav, builder.build(paramResolver));
                break;
            }
            default: {
                fpRuntimes = new LinkedHashMap<>(orderedRtBuilders.size());
                for(FeaturePackRuntime.Builder builder : orderedRtBuilders) {
                    copyResources(builder);
                    fpRuntimes.put(builder.gav, builder.build(paramResolver));
                }
                fpRuntimes = Collections.unmodifiableMap(fpRuntimes);
            }
        }

        return new ProvisioningRuntime(this);
    }

    private void processConfig(FeaturePackConfig fpConfig) throws ProvisioningException {
        final FeaturePackRuntime.Builder fp = getRtBuilder(fpConfig.getGav());

        List<FeaturePackConfig> pushedDepConfigs = Collections.emptyList();
        if(fp.spec.hasDependencies()) {
            final Collection<FeaturePackDependencySpec> fpDeps = fp.spec.getDependencies();
            pushedDepConfigs = new ArrayList<>(fpDeps.size());
            for (FeaturePackDependencySpec fpDep : fpDeps) {
                pushFpConfig(pushedDepConfigs, fpDep.getTarget());
            }
            if (!pushedDepConfigs.isEmpty()) {
                for (FeaturePackConfig depConfig : pushedDepConfigs) {
                    processConfig(depConfig);
                }
            }
        }

        boolean resolvedPackages = false;
        final List<FeaturePackConfig> fpConfigStack = fpConfigStacks.get(fpConfig.getGav());
        if(fpConfig.isInheritPackages()) {
            for(String packageName : fp.spec.getDefaultPackageNames()) {
                if(!isPackageExcluded(fpConfigStack, packageName)) {
                    resolvePackage(fp, fpConfigStack, packageName, Collections.emptyList());
                    resolvedPackages = true;
                }
            }
            if(fpConfig.hasIncludedPackages()) {
                for(PackageConfig pkgConfig : fpConfig.getIncludedPackages()) {
                    if(!isPackageExcluded(fpConfigStack, pkgConfig.getName())) {
                        resolvePackage(fp, fpConfigStack, pkgConfig.getName(), pkgConfig.getParameters());
                        resolvedPackages = true;
                    } else {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, pkgConfig.getName()));
                    }
                }
            }
        } else if(fpConfig.hasIncludedPackages()) {
            for (PackageConfig pkgConfig : fpConfig.getIncludedPackages()) {
                if (!isPackageExcluded(fpConfigStack, pkgConfig.getName())) {
                    resolvePackage(fp, fpConfigStack, pkgConfig.getName(), pkgConfig.getParameters());
                    resolvedPackages = true;
                } else {
                    throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, pkgConfig.getName()));
                }
            }
        }

        if (!pushedDepConfigs.isEmpty()) {
            popFpConfigs(pushedDepConfigs);
        }

        if(resolvedPackages && !fp.ordered) {
            orderFpRtBuilder(fp);
        }
    }

    private void popFpConfigs(List<FeaturePackConfig> fpConfigs) throws ProvisioningException {
        for (FeaturePackConfig fpConfig : fpConfigs) {
            final Gav fpGav = fpConfig.getGav();
            List<FeaturePackConfig> fpConfigStack = fpConfigStacks.get(fpGav);
            final FeaturePackConfig popped;
            if (fpConfigStack.size() == 1) {
                fpConfigStacks.remove(fpGav);
                popped = fpConfigStack.get(0);
                fpConfigStack = Collections.emptyList();
            } else {
                popped = fpConfigStack.remove(fpConfigStack.size() - 1);
                if (fpConfigStack.size() == 1) {
                    fpConfigStacks.put(fpGav, Collections.singletonList(fpConfigStack.get(0)));
                }
            }
            if (popped.hasIncludedPackages()) {
                final FeaturePackRuntime.Builder fp = getRtBuilder(popped.getGav());
                for (PackageConfig pkgConfig : popped.getIncludedPackages()) {
                    if (!isPackageExcluded(fpConfigStack, pkgConfig.getName())) {
                        resolvePackage(fp, fpConfigStack, pkgConfig.getName(), pkgConfig.getParameters());
                    } else {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, null, pkgConfig.getName()));
                    }
                }
            }
        }
    }

    private void pushFpConfig(List<FeaturePackConfig> pushed, FeaturePackConfig fpConfig)
            throws ProvisioningDescriptionException, ProvisioningException, ArtifactResolutionException {
        List<FeaturePackConfig> fpConfigStack = fpConfigStacks.get(fpConfig.getGav());
        if(fpConfigStack == null) {
            fpConfigStacks.put(fpConfig.getGav(), Collections.singletonList(fpConfig));
            pushed.add(fpConfig);
        } else if(fpConfigStack.get(fpConfigStack.size() - 1).isInheritPackages()) {
            boolean pushDep = false;
            if(fpConfig.hasExcludedPackages()) {
                for(String excluded : fpConfig.getExcludedPackages()) {
                    if(!isPackageExcluded(fpConfigStack, excluded) && !isPackageIncluded(fpConfigStack, excluded, Collections.emptyList())) {
                        pushDep = true;
                        break;
                    }
                }
            }
            if(!pushDep && fpConfig.hasIncludedPackages()) {
                for(PackageConfig included : fpConfig.getIncludedPackages()) {
                    if(!isPackageIncluded(fpConfigStack, included.getName(), included.getParameters()) && !isPackageExcluded(fpConfigStack, included.getName())) {
                        pushDep = true;
                        break;
                    }
                }
            }
            if(pushDep) {
                pushed.add(fpConfig);
                if (fpConfigStack.size() == 1) {
                    fpConfigStack = new ArrayList<>(fpConfigStack);
                    fpConfigStacks.put(fpConfig.getGav(), fpConfigStack);
                }
                fpConfigStack.add(fpConfig);
            }
        }
    }

    private FeaturePackRuntime.Builder getRtBuilder(ArtifactCoords.Gav gav) throws ProvisioningDescriptionException,
            ProvisioningException, ArtifactResolutionException {
        FeaturePackRuntime.Builder fp = fpRtBuilders.get(gav.toGa());
        if(fp == null) {
            fp = FeaturePackRuntime.builder(gav, LayoutUtils.getFeaturePackDir(layoutDir, gav, false));
            mkdirs(fp.dir);
            fpRtBuilders.put(fp.gav.toGa(), fp);

            final Path artifactPath = artifactResolver.resolve(fp.gav.toArtifactCoords());
            try {
                ZipUtils.unzip(artifactPath, fp.dir);
            } catch (IOException e) {
                throw new ProvisioningException("Failed to unzip " + artifactPath + " to " + layoutDir, e);
            }

            final Path fpXml = fp.dir.resolve(Constants.FEATURE_PACK_XML);
            if(!Files.exists(fpXml)) {
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpXml));
            }

            try(BufferedReader reader = Files.newBufferedReader(fpXml)) {
                fp.spec = FeaturePackXmlParser.getInstance().parse(reader);
            } catch (IOException | XMLStreamException e) {
                throw new ProvisioningException(Errors.parseXml(fpXml), e);
            }
        } else if(!fp.gav.equals(gav)) {
            throw new ProvisioningException(Errors.featurePackVersionConflict(fp.gav, gav));
        }
        return fp;
    }

    private void resolvePackage(FeaturePackRuntime.Builder fp, List<FeaturePackConfig> fpConfigStack, final String pkgName, Collection<PackageParameter> params)
            throws ProvisioningException {
        final PackageRuntime.Builder pkgRt = fp.pkgBuilders.get(pkgName);
        if(pkgRt != null) {
            if(!params.isEmpty()) {
                for(PackageParameter param : params) {
                    pkgRt.configBuilder.addParameter(param);
                }
            }
            return;
        }

        final PackageRuntime.Builder pkg = fp.newPackage(pkgName, LayoutUtils.getPackageDir(fp.dir, pkgName, false));
        if(!Files.exists(pkg.dir)) {
            throw new ProvisioningDescriptionException(Errors.packageNotFound(fp.gav, pkgName));
        }
        final Path pkgXml = pkg.dir.resolve(Constants.PACKAGE_XML);
        if(!Files.exists(pkgXml)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(pkgXml));
        }
        try(BufferedReader reader = Files.newBufferedReader(pkgXml)) {
            pkg.spec = PackageXmlParser.getInstance().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(pkgXml), e);
        }

        final PackageConfig.Builder pkgConfig = pkg.configBuilder;
        // set parameters set in the package spec first
        if(pkg.spec.hasParameters()) {
            for(PackageParameter param : pkg.spec.getParameters()) {
                pkgConfig.addParameter(param);
            }
        }

        if (pkg.spec.hasLocalDependencies()) {
            for (PackageDependencySpec dep : pkg.spec.getLocalDependencies().getDescriptions()) {
                if(isPackageExcluded(fpConfigStack, dep.getName())) {
                    if(!dep.isOptional()) {
                        throw new ProvisioningDescriptionException(Errors.unsatisfiedPackageDependency(fp.gav, pkgName, dep.getName()));
                    }
                    continue;
                }
                try {
                    resolvePackage(fp, fpConfigStack, dep.getName(), dep.getParameters());
                } catch(ProvisioningDescriptionException e) {
                    if(dep.isOptional()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        if(pkg.spec.hasExternalDependencies()) {
            final Collection<String> depNames = pkg.spec.getExternalDependencyNames();
            final List<FeaturePackConfig> pushedConfigs = new ArrayList<>(depNames.size());
            for(String depName : depNames) {
                pushFpConfig(pushedConfigs, fp.spec.getDependency(depName).getTarget());
            }
            for(String depName : depNames) {
                final FeaturePackDependencySpec depSpec = fp.spec.getDependency(depName);
                final FeaturePackRuntime.Builder targetFp = getRtBuilder(depSpec.getTarget().getGav());
                if(targetFp == null) {
                    throw new IllegalStateException(depSpec.getName() + " " + depSpec.getTarget().getGav() + " has not been layed out yet");
                }
                final List<FeaturePackConfig> depConfigStack = fpConfigStacks.get(targetFp.gav);
                final PackageDependencyGroupSpec pkgDeps = pkg.spec.getExternalDependencies(depName);
                boolean resolvedPackages = false;
                for(PackageDependencySpec pkgDep : pkgDeps.getDescriptions()) {
                    if(isPackageExcluded(depConfigStack, pkgDep.getName())) {
                        if(!pkgDep.isOptional()) {
                            throw new ProvisioningDescriptionException(Errors.unsatisfiedExternalPackageDependency(fp.gav, pkgName, targetFp.gav, pkgDep.getName()));
                        }
                        continue;
                    }
                    try {
                        resolvePackage(targetFp, depConfigStack, pkgDep.getName(), pkgDep.getParameters());
                        resolvedPackages = true;
                    } catch(ProvisioningDescriptionException e) {
                        if(pkgDep.isOptional()) {
                            continue;
                        } else {
                            throw e;
                        }
                    }
                }
                if(!targetFp.ordered && resolvedPackages) {
                    orderFpRtBuilder(targetFp);
                }
            }
            if (!pushedConfigs.isEmpty()) {
                popFpConfigs(pushedConfigs);
            }
        }

        if(!params.isEmpty()) {
            for(PackageParameter param : params) {
                pkgConfig.addParameter(param);
            }
        }
        fp.addPackage(pkgName);
    }

    private void orderFpRtBuilder(final FeaturePackRuntime.Builder fpRtBuilder) {
        this.orderedRtBuilders.add(fpRtBuilder);
        fpRtBuilder.ordered = true;
    }

    private boolean isPackageIncluded(List<FeaturePackConfig> stack, String packageName, Collection<PackageParameter> params) {
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

    private boolean isPackageExcluded(List<FeaturePackConfig> stack, String packageName) {
        int i = stack.size() - 1;
        while(i >= 0) {
            if(stack.get(i--).isExcluded(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }

    private void copyResources(FeaturePackRuntime.Builder fpRtBuilder) throws ProvisioningException {
        // resources should be copied last overriding the dependency resources
        final Path fpResources = fpRtBuilder.dir.resolve(Constants.RESOURCES);
        if(Files.exists(fpResources)) {
            try {
                IoUtils.copy(fpResources, workDir.resolve(Constants.RESOURCES));
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpResources, workDir.resolve(Constants.RESOURCES)), e);
            }
        }

        final Path fpPlugins = fpRtBuilder.dir.resolve(Constants.PLUGINS);
        if(Files.exists(fpPlugins)) {
            if(pluginsDir == null) {
                pluginsDir = workDir.resolve(Constants.PLUGINS);
            }
            try {
                IoUtils.copy(fpPlugins, pluginsDir);
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpPlugins, workDir.resolve(Constants.PLUGINS)), e);
            }
        }
    }
}
