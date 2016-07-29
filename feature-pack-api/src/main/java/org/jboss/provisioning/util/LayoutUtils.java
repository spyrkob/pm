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

import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.InstallationDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class LayoutUtils {

    public static Path getFeaturePackDir(Path fpLayoutDir, GAV gav) throws InstallationDescriptionException {
        final Path fpPath = fpLayoutDir.resolve(gav.getGroupId()).resolve(gav.getArtifactId()).resolve(gav.getVersion());
        if(!Files.exists(fpPath)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(fpPath));
        }
        return fpPath;
    }

    public static Path getPackageDir(Path fpDir, String packageName) throws InstallationDescriptionException {
        final Path dir = fpDir.resolve(Constants.PACKAGES).resolve(packageName);
        if(!Files.exists(dir)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(dir));
        }
        return dir;
    }

    public static Path getPackageContentDir(Path fpDir, String packageName) throws InstallationDescriptionException {
        return fpDir.resolve(Constants.PACKAGES).resolve(packageName).resolve(Constants.CONTENT);
    }
}
