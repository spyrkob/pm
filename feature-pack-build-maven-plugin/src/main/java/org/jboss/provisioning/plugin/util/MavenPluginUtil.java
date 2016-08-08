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

package org.jboss.provisioning.plugin.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.jboss.provisioning.plugin.FPMavenErrors;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenPluginUtil {

    public static InstallRequest getInstallLayoutRequest(final Path layoutDir) throws MojoExecutionException {
        final InstallRequest installReq = new InstallRequest();
        try (DirectoryStream<Path> wdStream = Files.newDirectoryStream(layoutDir, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        })) {
            for (Path groupDir : wdStream) {
                final String groupId = groupDir.getFileName().toString();
                try (DirectoryStream<Path> groupStream = Files.newDirectoryStream(groupDir)) {
                    for (Path artifactDir : groupStream) {
                        final String artifactId = artifactDir.getFileName().toString();
                        try (DirectoryStream<Path> artifactStream = Files.newDirectoryStream(artifactDir)) {
                            for (Path versionDir : artifactStream) {
                                System.out.println("Preparing feature-pack " + versionDir.toAbsolutePath());
                                final Path zippedFP = layoutDir.resolve(
                                        groupId + '_' + artifactId + '_' + versionDir.getFileName().toString() + ".zip");
                                if(Files.exists(zippedFP)) {
                                    IoUtils.recursiveDelete(zippedFP);
                                }
                                ZipUtils.zip(versionDir, zippedFP);
                                final Artifact artifact = new DefaultArtifact(
                                        groupDir.getFileName().toString(),
                                        artifactDir.getFileName().toString(), null,
                                        "zip", versionDir.getFileName().toString(), null, zippedFP.toFile());
                                installReq.addArtifact(artifact);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(FPMavenErrors.featurePackBuild(), e);
        }
        return installReq;
    }
}
