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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.FeaturePackLayoutInstaller;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.PathsUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.ProvisioningXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningManager {

    public static class Builder {

        private String encoding = "UTF-8";
        private Path installationHome;
        private ArtifactResolver artifactResolver;

        private Builder() {
        }

        public Builder setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder setInstallationHome(Path installationHome) {
            this.installationHome = installationHome;
            return this;
        }

        public Builder setArtifactResolver(ArtifactResolver artifactResolver) {
            this.artifactResolver = artifactResolver;
            return this;
        }

        public ProvisioningManager build() {
            return new ProvisioningManager(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String encoding;
    private final Path installationHome;
    private final ArtifactResolver artifactResolver;

    private ProvisionedInstallationDescription userProvisionedDescr;
    private ProvisionedInstallationDescription layoutProvisionedDescr;

    private ProvisioningManager(Builder builder) {
        this.encoding = builder.encoding;
        this.installationHome = builder.installationHome;
        this.artifactResolver = builder.artifactResolver;
    }

    /**
     * Location of the installation.
     *
     * @return  location of the installation
     */
    public Path getInstallationHome() {
        return installationHome;
    }

    /**
     * Last recorded provisioned state of the installation or null in case
     * the installation is not found at the specified installation location.
     *
     * If the user does not request to include the dependencies then the state
     * returned will reflect the installation specification picked by the user
     * explicitly without including the feature-packs installed as required
     * dependencies of the feature-packs the user has chosen explicitly.
     *
     * If the user does request to include the dependencies, the state returned
     * will reflect all the explicitly chosen feature-packs plus the ones
     * brought in implicitly as dependencies of the explicit ones.
     *
     * @param includeDependencies  whether the dependencies of the explicitly
     *                             selected feature-packs should be included
     *                             into the result
     * @return  description of the last recorded provisioned state
     * @throws ProvisioningException  in case any error occurs
     */
    public ProvisionedInstallationDescription getCurrentState(boolean includeDependencies) throws ProvisioningException {
        if(includeDependencies) {
            if(layoutProvisionedDescr == null) {
                layoutProvisionedDescr = readProvisionedState(PathsUtils.getLayoutStateXml(installationHome));
            }
            return layoutProvisionedDescr;
        }
        if (userProvisionedDescr == null) {
            userProvisionedDescr = readProvisionedState(PathsUtils.getUserProvisionedXml(installationHome));
        }
        return userProvisionedDescr;
    }

    /**
     * Installs the specified feature-pack.
     *
     * @param fpGav  feature-pack GAV
     * @throws ProvisioningException  in case the installation fails
     */
    public void install(ArtifactCoords.GavPart fpGav) throws ProvisioningException {
        install(ProvisionedFeaturePackDescription.builder().setGav(fpGav).build());
    }

    /**
     * Installs the desired feature-pack specification.
     *
     * @param fpDescr  the desired feature-pack specification
     * @throws ProvisioningException  in case the installation fails
     */
    public void install(ProvisionedFeaturePackDescription fpDescr) throws ProvisioningException {
        final ProvisionedInstallationDescription currentState = this.getCurrentState(false);
        if(currentState == null) {
            provision(ProvisionedInstallationDescription.builder().addFeaturePack(fpDescr).build());
        } else if(currentState.containsFeaturePack(fpDescr.getGav().getGaPart())) {
            final ProvisionedFeaturePackDescription presentDescr = currentState.getFeaturePack(fpDescr.getGav().getGaPart());
            if(presentDescr.getGav().equals(fpDescr.getGav())) {
                throw new ProvisioningException("Feature-pack " + fpDescr.getGav() + " is already installed");
            } else {
                throw new ProvisioningException(Errors.featurePackVersionConflict(fpDescr.getGav(), presentDescr.getGav()));
            }
        } else {
            provision(ProvisionedInstallationDescription.builder(currentState).addFeaturePack(fpDescr).build());
        }
    }

    /**
     * Uninstalls the specified feature-pack.
     *
     * @param gav  feature-pack GAV
     * @throws ProvisioningException  in case the uninstallation fails
     */
    public void unistall(ArtifactCoords.GavPart gav) throws ProvisioningException {
        final ProvisionedInstallationDescription currentState = this.getCurrentState(false);
        if(currentState == null) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        } else if(!currentState.containsFeaturePack(gav.getGaPart())) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        } else {
            provision(ProvisionedInstallationDescription.builder(currentState).removeFeaturePack(gav).build());
        }
    }

    /**
     * (Re-)provisions the current installation to the desired specification.
     *
     * @param installationDescr  the desired installation specification
     * @throws ProvisioningException  in case the re-provisioning fails
     */
    public void provision(ProvisionedInstallationDescription installationDescr) throws ProvisioningException {

        final Path workDir = IoUtils.createRandomTmpDir();
        final Path layoutDir = workDir.resolve("layout");
        try {
            final FeaturePackLayoutDescription.Builder layoutBuilder = FeaturePackLayoutDescription.builder();
            final Collection<ArtifactCoords.GavPart> provisioningPlugins = new LinkedHashSet<>();
            layoutFeaturePacks(installationDescr.getFeaturePacks(), layoutBuilder, provisioningPlugins, layoutDir, workDir);
            if (Files.exists(installationHome)) {
                IoUtils.recursiveDelete(installationHome);
            }
            mkdirs(installationHome);
            final FeaturePackLayoutDescription layoutDescr = layoutBuilder.build();
            FeaturePackLayoutInstaller.install(layoutDir, layoutDescr, installationDescr, installationHome);

            if(!provisioningPlugins.isEmpty()) {
                executePlugins(provisioningPlugins, layoutDescr, layoutDir, workDir);
            }
        } finally {
            IoUtils.recursiveDelete(workDir);
        }
    }

    private void layoutFeaturePacks(Collection<ProvisionedFeaturePackDescription> provisionedFps,
            FeaturePackLayoutDescription.Builder layoutBuilder,
            Collection<ArtifactCoords.GavPart> provisioningPlugins,
            Path layoutDir,
            Path workDir) throws ProvisioningException {

        for (ProvisionedFeaturePackDescription provisionedFp : provisionedFps) {
            final ArtifactCoords.GavPart fpGav = provisionedFp.getGav();
            final Path artifactPath = artifactResolver.resolve(fpGav.getArtifactCoords());
            final Path fpWorkDir = layoutDir.resolve(fpGav.getGroupId()).resolve(fpGav.getArtifactId()).resolve(fpGav.getVersion());
            mkdirs(fpWorkDir);
            try {
                System.out.println("Adding " + fpGav + " to the layout at " + fpWorkDir);
                ZipUtils.unzip(artifactPath, fpWorkDir);
            } catch (IOException e) {
                throw new ProvisioningException("Failed to unzip " + artifactPath + " to " + layoutDir, e);
            }

            final FeaturePackDescription fpDescr;
            try {
                fpDescr = FeaturePackLayoutDescriber.describeFeaturePack(LayoutUtils.getFeaturePackDir(layoutDir, fpGav), encoding);
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningException("Failed to describe feature-pack " + fpGav, e);
            }
            if(fpDescr.hasDependencies()) {
                layoutFeaturePacks(fpDescr.getDependencies(), layoutBuilder, provisioningPlugins, layoutDir, workDir);
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
                for(ArtifactCoords.GavPart gavPart : fpDescr.getProvisioningPlugins()) {
                    provisioningPlugins.add(gavPart);
                }
            }

            try {
                layoutBuilder.addFeaturePack(fpDescr);
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningException("Failed to layout feature packs", e);
            }
        }
    }

    private void executePlugins(final Collection<ArtifactCoords.GavPart> provisioningPlugins,
            final FeaturePackLayoutDescription layoutDescr,
            final Path layoutDir,
            final Path workDir) throws ProvisioningException {
        final List<java.net.URL> urls = new ArrayList<java.net.URL>(provisioningPlugins.size());
        for(ArtifactCoords.GavPart gavPart : provisioningPlugins) {
            try {
                urls.add(artifactResolver.resolve(gavPart.getArtifactCoords()).toUri().toURL());
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

    private ProvisionedInstallationDescription readProvisionedState(Path ps) throws ProvisioningException {
        if (!Files.exists(ps)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(ps)) {
            return new ProvisioningXmlParser().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(ps));
        }
    }

    private void mkdirs(final Path path) throws ProvisioningException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.mkdirs(path));
        }
    }

    public static void main(String[] args) throws Throwable {

        final FeaturePackLayoutDescription layout = FeaturePackLayoutDescription.builder()
            .addFeaturePack(FeaturePackDescription.builder(ArtifactCoords.getGavPart("g1:a1:v1")).build())
            .addFeaturePack(FeaturePackDescription.builder(ArtifactCoords.getGavPart("g1:a2:v2")).build())
            .addFeaturePack(FeaturePackDescription.builder(ArtifactCoords.getGavPart("g2:a3:v3")).build())
            .build();
        System.out.println(layout.getGav(ArtifactCoords.getGaPart("g2", "a3")));

        final ProvisioningManager pm = ProvisioningManager.builder().setInstallationHome(Paths.get("installation/home")).build();

        pm.install(ArtifactCoords.getGavPart("g1:a1:v1"));
        pm.install(
                ProvisionedFeaturePackDescription.builder()
                .setGav(ArtifactCoords.getGavPart("g1:a1:v1"))
                .excludePackage("p1")
                .excludePackage("p2")
                .build());

        pm.provision(ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                        .setGav(ArtifactCoords.getGavPart("g1:a1:v1"))
                        .excludePackage("p1")
                        .excludePackage("p2")
                        .build())
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                        .setGav(ArtifactCoords.getGavPart("g2:a2:v2"))
                        .excludePackage("p3")
                        .excludePackage("p4")
                        .build())
                .build());


    }
}
