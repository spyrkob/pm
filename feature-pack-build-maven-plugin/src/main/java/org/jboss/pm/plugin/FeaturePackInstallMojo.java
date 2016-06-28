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

package org.jboss.pm.plugin;

import java.io.File;
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
import org.jboss.pm.Constants;

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

        System.out.println("FEATURE PACK: install");
        final String workdirPath = repoSession.getSystemProperties().get(Constants.PM_INSTALL_WORK_DIR);
        if(workdirPath == null) {
            throw new MojoExecutionException("work dir is missing");
        }
        final File workDir = new File(workdirPath, Constants.FEATURE_PACKS);
        if(!workDir.exists()) {
            throw new MojoExecutionException("work dir does not exist: " + workDir.getAbsolutePath());
        }

        final InstallRequest installReq = new InstallRequest();
        for(File groupDir : workDir.listFiles()) {
            for(File artifactDir : groupDir.listFiles()) {
                for(File versionFile : artifactDir.listFiles()) {
                    final Artifact artifact = new DefaultArtifact(groupDir.getName(), artifactDir.getName(), null, "zip", versionFile.getName(), null, versionFile);
                    System.out.println("FP: " + artifact);
                    installReq.addArtifact(artifact);
                }
            }
        }

        try {
            repoSystem.install(repoSession, installReq);
        } catch (InstallationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
