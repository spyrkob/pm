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

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
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

    private final Path installationHome;

    private ProvisionedInstallationDescription userProvisionedDescr;
    private ProvisionedInstallationDescription layoutProvisionedDescr;

    private ProvisioningManager(Builder builder) {
        this.installationHome = builder.installationHome;
    }

    public Path getInstallationHome() {
        return installationHome;
    }

    public ProvisionedInstallationDescription getCurrentState(boolean includeDependencies) throws ProvisioningException {
        if(includeDependencies) {
            if(layoutProvisionedDescr == null) {
                layoutProvisionedDescr = readProvisionedState(Constants.LAYOUT_STATE_XML);
            }
            return layoutProvisionedDescr;
        }
        if (userProvisionedDescr == null) {
            userProvisionedDescr = readProvisionedState(Constants.USER_PROVISIONED_STATE_XML);
        }
        return userProvisionedDescr;
    }

    private ProvisionedInstallationDescription readProvisionedState(String fileName) throws ProvisioningException {
        final Path provisionedXml = getProvisionedStateXml(fileName);
        if (!Files.exists(provisionedXml)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(provisionedXml)) {
            return new ProvisioningXmlParser().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(provisionedXml));
        }
    }

    private Path getProvisionedStateXml(String fileName) {
        return installationHome.resolve(Constants.PROVISIONED_STATE_DIR).resolve(fileName);
    }
}
