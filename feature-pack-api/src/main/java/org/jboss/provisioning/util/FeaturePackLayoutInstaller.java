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

import java.nio.file.Path;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;

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
        install(fpLayoutDir,
                FeaturePackLayoutDescriber.describeLayout(fpLayoutDir, encoding),
                installDir);
    }

    public static void install(Path fpLayoutDir, FeaturePackLayoutDescription descr, Path installDir)
            throws FeaturePackInstallException {
        final FeaturePackInstaller fpInstaller = new FeaturePackInstaller();
        for(FeaturePackDescription fp : descr.getFeaturePacks()) {
            final Gav fpGav = fp.getGAV();
            System.out.println("Installing " + fpGav + " to " + installDir);
            final Path fpDir = fpLayoutDir.resolve(fpGav.getGroupId())
                    .resolve(fpGav.getArtifactId())
                    .resolve(fpGav.getVersion());
            fpInstaller.install(fp, fpDir, installDir);
        }
    }
}
