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

package org.jboss.provisioning.plugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "install", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class FeaturePackInstallMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final String workdirPath = repoSession.getSystemProperties().get(Constants.PM_INSTALL_WORK_DIR);
        if(workdirPath == null) {
            throw new MojoExecutionException(FPMavenErrors.propertyMissing(Constants.PM_INSTALL_WORK_DIR));
        }
        final Path workDir = Paths.get(workdirPath, Constants.FEATURE_PACKS);
        if(!Files.exists(workDir)) {
            throw new MojoExecutionException(Errors.pathDoesNotExist(workDir.toAbsolutePath()));
        }

        final InstallRequest installReq = new InstallRequest();
        try (DirectoryStream<Path> wdStream = Files.newDirectoryStream(workDir)) {
            for (Path groupDir : wdStream) {
                final String groupId = groupDir.getFileName().toString();
                try (DirectoryStream<Path> groupStream = Files.newDirectoryStream(groupDir)) {
                    for (Path artifactDir : groupStream) {
                        final String artifactId = artifactDir.getFileName().toString();
                        try (DirectoryStream<Path> artifactStream = Files.newDirectoryStream(artifactDir)) {
                            for (Path versionDir : artifactStream) {
                                System.out.println("Preparing feature-pack " + versionDir.toAbsolutePath());
                                final Path zippedFP = workDir.getParent().resolve(
                                        groupId + '_' + artifactId + '_' + versionDir.getFileName().toString() + ".zip");
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

        try {
            repoSystem.install(repoSession, installReq);
        } catch (InstallationException e) {
            throw new MojoExecutionException(FPMavenErrors.featurePackInstallation(), e);
        }
    }
}
