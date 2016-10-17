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

package org.jboss.provisioning;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription.Builder;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.FeaturePackLayoutInstaller;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class ProvisioningTask {

    private final ArtifactResolver artifactResolver;
    private final Path installationHome;
    private final String encoding;
    private final ProvisionedInstallationDescription installationDescr;

    private final Path workDir;
    private final Path layoutDir;
    private Collection<ArtifactCoords.Gav> provisioningPlugins = Collections.emptySet();

    private Set<ArtifactCoords.Gav> layedOutFps = Collections.emptySet();

    ProvisioningTask(ArtifactResolver artifactResolver, Path installationHome, String encoding, ProvisionedInstallationDescription installationDescr) {
        this.artifactResolver = artifactResolver;
        this.installationHome = installationHome;
        this.encoding = encoding;
        this.installationDescr = installationDescr;

        workDir = IoUtils.createRandomTmpDir();
        layoutDir = workDir.resolve("layout");
    }

    void execute() throws ProvisioningException {
        try {
            final FeaturePackLayoutDescription.Builder layoutBuilder = FeaturePackLayoutDescription.builder();

            Map<ArtifactCoords.Gav, ProvisionedFeaturePackDescription.Builder> fpBuilders = Collections.emptyMap();
            for (ProvisionedFeaturePackDescription provisionedFp : installationDescr.getFeaturePacks()) {
                fpBuilders = layoutFeaturePack(provisionedFp, layoutBuilder);
            }

            final ProvisionedInstallationDescription.Builder installBuilder = ProvisionedInstallationDescription.builder();
            for(ProvisionedFeaturePackDescription.Builder fpBuilder : fpBuilders.values()) {
                installBuilder.addFeaturePack(fpBuilder.build());
            }

            if (Files.exists(installationHome)) {
                IoUtils.recursiveDelete(installationHome);
            }
            mkdirs(installationHome);

            final FeaturePackLayoutDescription layoutDescr = layoutBuilder.build();
            FeaturePackLayoutInstaller.install(installBuilder.build(), installationDescr, layoutDescr, layoutDir, installationHome);

            if(!provisioningPlugins.isEmpty()) {
                executePlugins(installationDescr, layoutDescr);
            }
        } finally {
            IoUtils.recursiveDelete(workDir);
        }
    }

    private Map<ArtifactCoords.Gav, ProvisionedFeaturePackDescription.Builder> layoutFeaturePack(
            ProvisionedFeaturePackDescription provisionedFp,
            FeaturePackLayoutDescription.Builder layoutBuilder) throws ProvisioningException {

        final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
        final Path artifactPath = artifactResolver.resolve(fpGav.toArtifactCoords());
        final Path fpWorkDir = LayoutUtils.getFeaturePackDir(layoutDir, fpGav, false);
        mkdirs(fpWorkDir);
        try {
            System.out.println("Adding " + fpGav + " to the layout at " + fpWorkDir);
            ZipUtils.unzip(artifactPath, fpWorkDir);
        } catch (IOException e) {
            throw new ProvisioningException("Failed to unzip " + artifactPath + " to " + layoutDir, e);
        }

        final FeaturePackDescription fpDescr;
        try {
            fpDescr = FeaturePackLayoutDescriber.describeFeaturePack(fpWorkDir, encoding);
        } catch (ProvisioningDescriptionException e) {
            throw new ProvisioningException("Failed to describe feature-pack " + fpGav, e);
        }

        Map<ArtifactCoords.Gav, ProvisionedFeaturePackDescription.Builder> fpBuilders = Collections.emptyMap();
        if(fpDescr.hasDependencies()) {
            for(ProvisionedFeaturePackDescription dep : fpDescr.getDependencies()) {
                if(!installationDescr.containsFeaturePack(dep.getGav().getGa())) {
                    final Map<Gav, Builder> depBuilders = layoutFeaturePack(dep, layoutBuilder);
                    switch(fpBuilders.size()) {
                        case 0:
                            fpBuilders = depBuilders;
                            break;
                        case 1:
                            final Gav provisionedGav = fpBuilders.keySet().iterator().next();
                            if(depBuilders.size() == 1 && depBuilders.containsKey(provisionedGav)) {
                                fpBuilders.get(provisionedGav).include(depBuilders.get(provisionedGav).build());
                                break;
                            }
                            fpBuilders = new LinkedHashMap<>(fpBuilders);
                        default:
                            for(Map.Entry<ArtifactCoords.Gav, ProvisionedFeaturePackDescription.Builder> depEntry : depBuilders.entrySet()) {
                                final Builder fpBuilder = fpBuilders.get(depEntry.getKey());
                                if(fpBuilder == null) {
                                    fpBuilders.put(depEntry.getKey(), depEntry.getValue());
                                } else {
                                    fpBuilder.include(depEntry.getValue().build());
                                }
                            }
                    }
                }
            }
        }

        switch(fpBuilders.size()) {
            case 0:
                fpBuilders = Collections.singletonMap(fpGav, ProvisionedFeaturePackDescription.builder(provisionedFp));
                break;
            case 1:
                if(fpBuilders.containsKey(fpGav)) {
                    fpBuilders.get(fpGav).exclude(provisionedFp);
                    break;
                }
                fpBuilders = new LinkedHashMap<>(fpBuilders);
            default:
                if(fpBuilders.containsKey(fpGav)) {
                    fpBuilders.get(fpGav).exclude(provisionedFp);
                } else {
                    fpBuilders.put(fpGav, ProvisionedFeaturePackDescription.builder(provisionedFp));
                }
        }

        final Path fpResources = fpWorkDir.resolve("resources");
        if(Files.exists(fpResources)) {
            try {
                IoUtils.copy(fpResources, workDir.resolve("resources"));
            } catch (IOException e) {
                throw new ProvisioningException(Errors.copyFile(fpResources, workDir.resolve("resources")), e);
            }
        }

        if(fpDescr.hasProvisioningPlugins()) {
            for(ArtifactCoords.Gav gav : fpDescr.getProvisioningPlugins()) {
                addProvisioningPlugin(gav);
            }
        }

        try {
            layoutBuilder.addFeaturePack(fpDescr);
        } catch (ProvisioningDescriptionException e) {
            throw new ProvisioningException("Failed to layout feature packs", e);
        }

        return fpBuilders;
    }

    private void executePlugins(final ProvisionedInstallationDescription installationDescr,
            final FeaturePackLayoutDescription layoutDescr) throws ProvisioningException {
        final List<java.net.URL> urls = new ArrayList<java.net.URL>(provisioningPlugins.size());
        for(ArtifactCoords.Gav gavPart : provisioningPlugins) {
            try {
                urls.add(artifactResolver.resolve(gavPart.toArtifactCoords()).toUri().toURL());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!urls.isEmpty()) {
            final ProvisioningContext ctx = new ProvisioningContext() {
                @Override
                public Path getLayoutDir() {
                    return layoutDir;
                }
                @Override
                public Path getInstallDir() {
                    return installationHome;
                }
                @Override
                public Path getResourcesDir() {
                    return workDir.resolve("resources");
                }
                @Override
                public ProvisionedInstallationDescription getInstallationDescription() {
                    return installationDescr;
                }
                @Override
                public FeaturePackLayoutDescription getLayoutDescription() {
                    return layoutDescr;
                }
                @Override
                public Path resolveArtifact(ArtifactCoords coords) throws ArtifactResolutionException {
                    return artifactResolver.resolve(coords);
                }
                @Override
                public String getEncoding() {
                    return encoding;
                }
            };
            final java.net.URLClassLoader ucl = new java.net.URLClassLoader(
                    urls.toArray(new java.net.URL[urls.size()]),
                    Thread.currentThread().getContextClassLoader());
            final ServiceLoader<ProvisioningPlugin> plugins = ServiceLoader.load(ProvisioningPlugin.class, ucl);
            for (ProvisioningPlugin plugin : plugins) {
                try {
                    plugin.execute(ctx);
                } catch (ProvisioningException e) {
                    throw new ProvisioningException("Provisioning plugin failed", e);
                }
            }
        }
    }

    private void addProvisioningPlugin(ArtifactCoords.Gav gav) {
        switch(provisioningPlugins.size()) {
            case 0:
                provisioningPlugins = Collections.singleton(gav);
                break;
            case 1:
                if(provisioningPlugins.contains(gav)) {
                    return;
                }
                provisioningPlugins = new LinkedHashSet<>(provisioningPlugins);
            default:
                provisioningPlugins.add(gav);
        }
    }

    private boolean addLayedOutFp(ArtifactCoords.Gav gav) {
        switch(layedOutFps.size()) {
            case 0:
                layedOutFps = Collections.singleton(gav);
                return true;
            case 1:
                if(layedOutFps.contains(gav)) {
                    return false;
                }
                layedOutFps = new HashSet<>(layedOutFps);
            default:
                return layedOutFps.add(gav);
        }
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }
}
