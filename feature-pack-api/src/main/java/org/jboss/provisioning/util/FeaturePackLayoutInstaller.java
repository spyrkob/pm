/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provisioning.util;

import java.nio.file.Path;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.InstallationDescription;
import org.jboss.provisioning.descr.InstallationDescriptionException;

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
     * @throws InstallationDescriptionException
     * @throws FeaturePackInstallException
     */
    public static void install(Path fpLayoutDir, Path installDir)
            throws InstallationDescriptionException, FeaturePackInstallException {
        final InstallationDescription installDescr = FeaturePackLayoutDescriber.describeLayout(fpLayoutDir);
        final FeaturePackInstaller fpInstaller = new FeaturePackInstaller();
        for(FeaturePackDescription fp : installDescr.getFeaturePacks()) {
            final GAV fpGav = fp.getGAV();
            System.out.println("Installing " + fpGav + " to " + installDir);
            final Path fpDir = fpLayoutDir.resolve(fpGav.getGroupId())
                    .resolve(fpGav.getArtifactId())
                    .resolve(fpGav.getVersion());
            fpInstaller.install(fp, fpDir, installDir);
        }
    }
}
