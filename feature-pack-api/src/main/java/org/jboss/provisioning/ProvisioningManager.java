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
package org.jboss.provisioning;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.runtime.ProvisioningRuntimeBuilder;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PathsUtils;
import org.jboss.provisioning.xml.ProvisionedStateXmlParser;
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
        private PackageParameterResolver paramResolver;

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

        public Builder setPackageParameterResolver(PackageParameterResolver paramResolver) {
            this.paramResolver = paramResolver;
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
    private final PackageParameterResolver paramResolver;

    private ProvisioningConfig provisioningConfig;

    private ProvisioningManager(Builder builder) {
        this.encoding = builder.encoding;
        this.installationHome = builder.installationHome;
        this.artifactResolver = builder.artifactResolver;
        this.paramResolver = builder.paramResolver == null ? PackageParameterResolver.NULL_RESOLVER : builder.paramResolver;
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
     * Last recorded installation provisioning configuration or null in case
     * the installation is not found at the specified location.
     *
     * @return  the last recorded provisioning installation configuration
     * @throws ProvisioningException  in case any error occurs
     */
    public ProvisioningConfig getProvisioningConfig() throws ProvisioningException {
        if (provisioningConfig == null) {
            provisioningConfig = readProvisioningConfig(PathsUtils.getProvisioningXml(installationHome));
        }
        return provisioningConfig;
    }

    /**
     * Returns the detailed description of the provisioned installation.
     *
     * @return  detailed description of the provisioned installation
     * @throws ProvisioningException  in case there was an error reading the description from the disk
     */
    public ProvisionedState<ProvisionedFeaturePack<ProvisionedPackage>, ProvisionedPackage> getProvisionedState() throws ProvisioningException {
        final Path xml = PathsUtils.getProvisionedStateXml(installationHome);
        if (!Files.exists(xml)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(xml)) {
            return new ProvisionedStateXmlParser().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(xml), e);
        }
    }

    /**
     * Installs the specified feature-pack.
     *
     * @param fpGav  feature-pack GAV
     * @throws ProvisioningException  in case the installation fails
     */
    public void install(ArtifactCoords.Gav fpGav) throws ProvisioningException {
        install(FeaturePackConfig.forGav(fpGav));
    }

    /**
     * Installs the desired feature-pack configuration.
     *
     * @param fpConfig  the desired feature-pack configuration
     * @throws ProvisioningException  in case the installation fails
     */
    public void install(FeaturePackConfig fpConfig) throws ProvisioningException {
        final ProvisioningConfig provisioningConfig = this.getProvisioningConfig();
        if(provisioningConfig == null) {
            provision(ProvisioningConfig.builder().addFeaturePack(fpConfig).build());
        } else if(provisioningConfig.containsFeaturePack(fpConfig.getGav().toGa())) {
            final FeaturePackConfig presentConfig = provisioningConfig.getFeaturePack(fpConfig.getGav().toGa());
            if(presentConfig.getGav().equals(fpConfig.getGav())) {
                throw new ProvisioningException(Errors.featurePackAlreadyInstalled(fpConfig.getGav()));
            } else {
                throw new ProvisioningException(Errors.featurePackVersionConflict(fpConfig.getGav(), presentConfig.getGav()));
            }
        } else {
            provision(ProvisioningConfig.builder(provisioningConfig).addFeaturePack(fpConfig).build());
        }
    }

    /**
     * Uninstalls the specified feature-pack.
     *
     * @param gav  feature-pack GAV
     * @throws ProvisioningException  in case the uninstallation fails
     */
    public void uninstall(ArtifactCoords.Gav gav) throws ProvisioningException {
        final ProvisioningConfig provisioningConfig = getProvisioningConfig();
        if(provisioningConfig == null) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        } else if(!provisioningConfig.containsFeaturePack(gav.toGa())) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        } else {
            provision(ProvisioningConfig.builder(provisioningConfig).removeFeaturePack(gav).build());
        }
    }

    /**
     * (Re-)provisions the current installation to the desired specification.
     *
     * @param provisioningConfig  the desired installation specification
     * @throws ProvisioningException  in case the re-provisioning fails
     */
    public void provision(ProvisioningConfig provisioningConfig) throws ProvisioningException {

        if(!provisioningConfig.hasFeaturePacks()) {
            if(Files.exists(installationHome)) {
                try(DirectoryStream<Path> stream = Files.newDirectoryStream(installationHome)) {
                    for(Path p : stream) {
                        IoUtils.recursiveDelete(p);
                    }
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readDirectory(installationHome));
                }
            }
            return;
        }

        if(artifactResolver == null) {
            throw new ProvisioningException("Artifact resolver has not been provided.");
        }

        try(final ProvisioningRuntime runtime = ProvisioningRuntimeBuilder.newInstance()
                .setArtifactResolver(artifactResolver)
                .setConfig(provisioningConfig)
                .setEncoding(encoding)
                .setParameterResolver(paramResolver)
                .setInstallDir(installationHome)
                .build()) {
            // install the software
            ProvisioningRuntime.install(runtime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.provisioningConfig = null;
    }

    /**
     * Provision the state described in the specified XML file.
     *
     * @param provisioningXml  file describing the desired provisioned state
     * @throws ProvisioningException  in case provisioning fails
     */
    public void provision(Path provisioningXml) throws ProvisioningException {
        provision(readProvisioningConfig(provisioningXml));
    }

    /**
     * Exports the current provisioning configuration of the installation to
     * the specified file.
     *
     * @param location  file to which the current installation configuration should be exported
     * @throws ProvisioningException  in case the provisioning configuration record is missing
     * @throws IOException  in case writing to the specified file fails
     */
    public void exportProvisioningConfig(Path location) throws ProvisioningException, IOException {
        final Path userProvisionedXml = PathsUtils.getProvisioningXml(installationHome);
        if(!Files.exists(userProvisionedXml)) {
            throw new ProvisioningException("Provisioned state record is missing for " + installationHome);
        }
        IoUtils.copy(userProvisionedXml, location);
    }

    String getEncoding() {
        return encoding;
    }

    ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    PackageParameterResolver getPackageParameterResolver() {
        return paramResolver;
    }

    private ProvisioningConfig readProvisioningConfig(Path path) throws ProvisioningException {
        if (!Files.exists(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return new ProvisioningXmlParser().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(path), e);
        }
    }
}
