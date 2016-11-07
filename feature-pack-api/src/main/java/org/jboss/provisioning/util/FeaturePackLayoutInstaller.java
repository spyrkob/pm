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
package org.jboss.provisioning.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.FeaturePackLayoutDescription;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;
import org.jboss.provisioning.xml.ProvisionedStateXmlWriter;
import org.jboss.provisioning.xml.ProvisioningXmlWriter;

/**
 * Turns feature-pack layout into a target installation.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutInstaller {

    /**
     * Provisions an installation at the specified location by processing
     * the feature-pack layout.
     *
     * @param fpLayoutDir  feature-pack layout directory
     * @param installDir  target installation directory
     * @param encoding the encoding to use when reading files under {@code fpLayoutDir}
     * @throws ProvisioningDescriptionException
     * @throws FeaturePackInstallException
     */
    public static void install(Path fpLayoutDir, Path installDir, String encoding)
            throws ProvisioningDescriptionException, FeaturePackInstallException {
        final FeaturePackLayoutDescription layoutDescr = FeaturePackLayoutDescriber.describeLayout(fpLayoutDir, encoding);
        final ProvisioningConfig.Builder configBuilder = ProvisioningConfig.builder();
        for(FeaturePackSpec fpSpec : layoutDescr.getFeaturePacks()) {
            configBuilder.addFeaturePack(FeaturePackConfig.forGav(fpSpec.getGav()));
        }
        final ProvisioningConfig installConfig = configBuilder.build();
        final ProvisionedState provisionedState = new ProvisionedStateResolver().resolve(installConfig, layoutDescr, fpLayoutDir);
        install(layoutDescr, fpLayoutDir, installConfig, provisionedState, installDir);
    }

    public static void install(FeaturePackLayoutDescription layoutDescr, Path layoutDir,
            ProvisioningConfig userConfig, ProvisionedState provisionedState,
            Path installDir) throws FeaturePackInstallException {

        for(ProvisionedFeaturePack provisionedFp : provisionedState.getFeaturePacks()) {
            final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
            System.out.println("Installing " + fpGav + " to " + installDir);
            Path fpDir;
            try {
                fpDir = LayoutUtils.getFeaturePackDir(layoutDir, fpGav);
            } catch (ProvisioningDescriptionException e) {
                throw new FeaturePackInstallException(Errors.unknownFeaturePack(fpGav), e);
            }

            for(String pkgName : provisionedFp.getPackageNames()) {
                final Path pkgSrcDir = LayoutUtils.getPackageContentDir(fpDir, pkgName);
                if (Files.exists(pkgSrcDir)) {
                    try {
                        IoUtils.copy(pkgSrcDir, installDir);
                    } catch (IOException e) {
                        throw new FeaturePackInstallException(Errors.packageContentCopyFailed(pkgName), e);
                    }
                }
            }

            final FeaturePackSpec fp = layoutDescr.getFeaturePack(fpGav.toGa());
            if(fp == null) {
                throw new FeaturePackInstallException(Errors.unknownFeaturePack(fpGav));
            }
            recordFeaturePack(fp, installDir);
        }

        writeState(userConfig, PathsUtils.getProvisioningXml(installDir));

        try {
            ProvisionedStateXmlWriter.getInstance().write(provisionedState, PathsUtils.getProvisionedStateXml(installDir));
        } catch (XMLStreamException | IOException e) {
            throw new FeaturePackInstallException(Errors.writeXml(PathsUtils.getProvisionedStateXml(installDir)), e);
        }
    }

    private static void writeState(ProvisioningConfig provisioningConfig, final Path provisionedXml)
            throws FeaturePackInstallException {
        try {
            ProvisioningXmlWriter.INSTANCE.write(provisioningConfig, provisionedXml);
        } catch (XMLStreamException | IOException e) {
            throw new FeaturePackInstallException(Errors.writeXml(provisionedXml), e);
        }
    }

    private static void recordFeaturePack(FeaturePackSpec fpSpec, final Path installDir)
            throws FeaturePackInstallException {
        try {
            FeaturePackXmlWriter.INSTANCE.write(fpSpec, PathsUtils.getFeaturePackXml(installDir, fpSpec.getGav()));
        } catch (XMLStreamException | IOException e) {
            throw new FeaturePackInstallException(Errors.writeXml(installDir), e);
        }
    }
}
