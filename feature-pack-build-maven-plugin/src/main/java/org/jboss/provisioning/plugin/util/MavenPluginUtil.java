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
