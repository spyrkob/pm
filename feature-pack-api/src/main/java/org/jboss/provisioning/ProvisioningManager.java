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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.descr.ResolvedInstallationDescription;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.PathsUtils;
import org.jboss.provisioning.xml.ProvisionedInstallationXmlParser;
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
     * Last recorded installation provisioning configuration or null in case
     * the installation is not found at the specified location.
     *
     * @return  the last recorded provisioning installation configuration
     * @throws ProvisioningException  in case any error occurs
     */
    public ProvisionedInstallationDescription getProvisioningConfig() throws ProvisioningException {
        if (userProvisionedDescr == null) {
            userProvisionedDescr = readProvisionedState(PathsUtils.getUserProvisionedXml(installationHome));
        }
        return userProvisionedDescr;
    }

    /**
     * Returns the detailed description of the provisioned installation.
     *
     * @return  detailed description of the provisioned installation
     * @throws ProvisioningException  in case there was an error reading the description from the disk
     */
    public ResolvedInstallationDescription getProvisionedState() throws ProvisioningException {
        final Path xml = PathsUtils.getProvisionedStateXml(installationHome);
        if (!Files.exists(xml)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(xml)) {
            return new ProvisionedInstallationXmlParser().parse(reader);
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
        install(ProvisionedFeaturePackDescription.forGav(fpGav));
    }

    /**
     * Installs the desired feature-pack specification.
     *
     * @param fpDescr  the desired feature-pack specification
     * @throws ProvisioningException  in case the installation fails
     */
    public void install(ProvisionedFeaturePackDescription fpDescr) throws ProvisioningException {
        final ProvisionedInstallationDescription currentState = this.getProvisioningConfig();
        if(currentState == null) {
            provision(ProvisionedInstallationDescription.builder().addFeaturePack(fpDescr).build());
        } else if(currentState.containsFeaturePack(fpDescr.getGav().toGa())) {
            final ProvisionedFeaturePackDescription presentDescr = currentState.getFeaturePack(fpDescr.getGav().toGa());
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
    public void uninstall(ArtifactCoords.Gav gav) throws ProvisioningException {
        final ProvisionedInstallationDescription currentState = getProvisioningConfig();
        if(currentState == null) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        } else if(!currentState.containsFeaturePack(gav.toGa())) {
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

        if(!installationDescr.hasFeaturePacks()) {
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

        new ProvisioningTask(artifactResolver, installationHome, encoding, installationDescr).execute();
        this.userProvisionedDescr = null;
    }

    /**
     * Provision the state described in the specified XML file.
     *
     * @param provisionedStateXml  file describing the desired provisioned state
     * @throws ProvisioningException  in case provisioning fails
     */
    public void provision(Path provisionedStateXml) throws ProvisioningException {
        provision(readProvisionedState(provisionedStateXml));
    }

    /**
     * Exports the current provisioned state of the installation to
     * the specified file.
     *
     * @param location  file to which the current installation state should be exported
     * @throws ProvisioningException  in case the provisioning state record is missing
     * @throws IOException  in case writing to the specified file fails
     */
    public void exportProvisionedState(Path location) throws ProvisioningException, IOException {
        final Path userProvisionedXml = PathsUtils.getUserProvisionedXml(installationHome);
        if(!Files.exists(userProvisionedXml)) {
            throw new ProvisioningException("Provisioned state record is missing for " + installationHome);
        }
        IoUtils.copy(userProvisionedXml, location);
    }

    private ProvisionedInstallationDescription readProvisionedState(Path ps) throws ProvisioningException {
        if (!Files.exists(ps)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(ps)) {
            return new ProvisioningXmlParser().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(ps), e);
        }
    }

    public static void main(String[] args) throws Throwable {

        final Path installDir = Paths.get("/home/olubyans/demo/wf");
        final ProvisioningManager pm = ProvisioningManager.builder().setInstallationHome(installDir).build();

        //pm.exportProvisionedState(installDir.getParent().resolve("provisioned-state.xml"));

        pm.install(ArtifactCoords.newGav("org.wildfly.core:wildfly-core-feature-pack-new:3.0.0.Alpha9-SNAPSHOT"));
/*        pm.install(
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
*/

    }
}
