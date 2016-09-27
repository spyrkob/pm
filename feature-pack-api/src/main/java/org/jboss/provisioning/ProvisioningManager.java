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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.util.PathsUtils;
import org.jboss.provisioning.xml.ProvisioningXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningManager {

    public static class Builder {

        private Path installationHome;

        private Builder() {
        }

        public Builder setInstallationHome(Path installationHome) {
            this.installationHome = installationHome;
            return this;
        }

        public ProvisioningManager build() {
            return new ProvisioningManager(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Path installationHome;

    private ProvisionedInstallationDescription userProvisionedDescr;
    private ProvisionedInstallationDescription layoutProvisionedDescr;

    private ProvisioningManager(Builder builder) {
        this.installationHome = builder.installationHome;
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
    public void install(Gav fpGav) throws ProvisioningException {
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
    public void unistall(Gav gav) throws ProvisioningException {
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
        // TODO
        throw new UnsupportedOperationException();
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

    public static void main(String[] args) throws Throwable {

        FeaturePackLayoutDescription.builder()
            .addFeaturePack(FeaturePackDescription.builder(Gav.fromString("g1:a1:v1")).build())
            .addFeaturePack(FeaturePackDescription.builder(Gav.fromString("g1:a1:v2")).build());

        final ProvisioningManager pm = ProvisioningManager.builder().setInstallationHome(Paths.get("installation/home")).build();

        pm.install(Gav.fromString("g1:a1:v1"));
        pm.install(
                ProvisionedFeaturePackDescription.builder()
                .setGav(Gav.fromString("g1:a1:v1"))
                .excludePackage("p1")
                .excludePackage("p2")
                .build());

        pm.provision(ProvisionedInstallationDescription.builder()
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                        .setGav(Gav.fromString("g1:a1:v1"))
                        .excludePackage("p1")
                        .excludePackage("p2")
                        .build())
                .addFeaturePack(
                        ProvisionedFeaturePackDescription.builder()
                        .setGav(Gav.fromString("g2:a2:v2"))
                        .excludePackage("p3")
                        .excludePackage("p4")
                        .build())
                .build());


    }
}
