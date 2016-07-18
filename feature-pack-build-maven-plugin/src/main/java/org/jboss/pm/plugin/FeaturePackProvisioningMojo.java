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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.jboss.pm.Constants;
import org.jboss.pm.GAV;
import org.jboss.pm.descr.InstallationDescriptionException;
import org.jboss.pm.provisioning.ProvisioningMetaData;
import org.jboss.pm.provisioning.xml.ProvisioningXmlParser;
import org.jboss.pm.util.Errors;
import org.jboss.pm.util.FeaturePackInstallException;
import org.jboss.pm.util.FeaturePackLayoutInstaller;
import org.jboss.pm.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class FeaturePackProvisioningMojo extends AbstractMojo {

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

        System.out.println("FEATURE PACK: build");

        final String provXmlArg = repoSession.getSystemProperties().get(Constants.PROVISIONING_XML);
        if(provXmlArg == null) {
            throw new MojoExecutionException(FPMavenErrors.propertyMissing(Constants.PROVISIONING_XML));
        }
        final Path provXml = Paths.get(provXmlArg);
        if(!Files.exists(provXml)) {
            throw new MojoExecutionException(Errors.pathDoesNotExist(provXml));
        }

        final String installDirArg = repoSession.getSystemProperties().get(Constants.PM_INSTALL_DIR);
        if(installDirArg == null) {
            throw new MojoExecutionException(FPMavenErrors.propertyMissing(Constants.PM_INSTALL_DIR));
        }
        final Path installDir = Paths.get(installDirArg);

        ProvisioningMetaData metadata;
        try(InputStream fis = Files.newInputStream(provXml)) {
            metadata = new ProvisioningXmlParser().parse(fis);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException(Errors.pathDoesNotExist(provXml), e);
        } catch (XMLStreamException e) {
            throw new MojoExecutionException(Errors.parseXml(provXml), e);
        } catch (IOException e) {
            throw new MojoExecutionException(Errors.openFile(provXml), e);
        }

        System.out.println("Feature packs: " + metadata);

        final Collection<GAV> featurePacks = metadata.getFeaturePacks();
        if(featurePacks.isEmpty()) {
            return;
        }
        final List<ArtifactRequest> requests;
        if(featurePacks.size() ==  1) {
            requests = Collections.singletonList(getArtifactRequest(featurePacks.iterator().next()));
        } else {
            requests = new ArrayList<ArtifactRequest>(featurePacks.size());
            for(GAV gav : featurePacks) {
                requests.add(getArtifactRequest(gav));
            }
        }

        final List<ArtifactResult> results;
        try {
            results = repoSystem.resolveArtifacts(repoSession, requests);
        } catch ( ArtifactResolutionException e ) {
            throw new MojoExecutionException(FPMavenErrors.artifactResolution(featurePacks), e);
        }

        if(!Files.exists(installDir)) {
            mkdirs(installDir);
        }

        final Path workDir = IoUtils.createRandomTmpDir();
        try {
            for (ArtifactResult res : results) {
                final Artifact fpArtifact = res.getArtifact();
                final Path fpWorkDir = workDir.resolve(fpArtifact.getGroupId())
                        .resolve(fpArtifact.getArtifactId())
                        .resolve(fpArtifact.getVersion());
                mkdirs(fpWorkDir);
                try {
                    org.jboss.pm.util.ZipUtils.unzip(Paths.get(fpArtifact.getFile().getAbsolutePath()), fpWorkDir);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to unzip " + fpArtifact.getFile().getAbsolutePath()
                            + " to " + installDir, e);
                }
            }
            try {
                FeaturePackLayoutInstaller.install(workDir, installDir);
            } catch (InstallationDescriptionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FeaturePackInstallException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } finally {
            IoUtils.recursiveDelete(workDir);
        }

        //collectDependencies(artifact);
        //resolveDependencies(artifact);
        //versionRequest(artifact);
        //artifactRequest(new DefaultArtifact("org.wildfly.core", "wildfly-cli", "jar", "3.0.0.Alpha3-SNAPSHOT"));
        //artifactRequest(new DefaultArtifact("org.wildfly.core", "wildfly-cli", "jar", "LATEST"));
        //artifactRequest(new DefaultArtifact("org.wildfly.feature-pack", "wildfly", "zip", "10.1.0.Final-SNAPSHOT"));
    }

    private void mkdirs(final Path path) throws MojoExecutionException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new MojoExecutionException(Errors.mkdirs(path));
        }
    }

    private void collectDependencies(final Artifact artifact) throws MojoExecutionException {
        CollectResult cRes = null;
        try {
            cRes = repoSystem.collectDependencies(repoSession, new CollectRequest(new Dependency(artifact, null), remoteRepos));
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Failed to collect", e);
        }
        printDeps(cRes.getRoot());
    }

    private static void printDeps(DependencyNode dep) {
        printDeps(dep, 0);
    }

    private static void printDeps(DependencyNode dep, int level) {
        final StringBuilder buf = new StringBuilder();
        for(int i = 0; i < level; ++i) {
            buf.append("  ");
        }
        buf.append(dep.getArtifact().getGroupId())
            .append(':')
            .append(dep.getArtifact().getArtifactId())
            .append(':')
            .append(dep.getArtifact().getVersion());
        System.out.println(buf.toString());
        for(DependencyNode child : dep.getChildren()) {
            printDeps(child, level + 1);
        }
    }

    private void resolveDependencies(final Artifact artifact) throws MojoExecutionException {
        DependencyRequest dReq = new DependencyRequest().setCollectRequest(new CollectRequest(new Dependency(artifact, null), remoteRepos));
        DependencyResult dRes;
        try {
            dRes = repoSystem.resolveDependencies(repoSession, dReq);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException("Failed to resolve dependency", e);
        }

        System.out.println("   root " + dRes.getRoot());
        System.out.println("deps " + dRes.getArtifactResults());
        for(ArtifactResult aRes : dRes.getArtifactResults()) {
            System.out.println("  - " + aRes.getArtifact());
        }
    }

    private void versionRequest(final Artifact artifact) throws MojoExecutionException {
        VersionRequest vReq = new VersionRequest()
            .setArtifact(artifact)
            .setRepositories(remoteRepos);

        VersionResult vRes;
        try {
            vRes = repoSystem.resolveVersion(repoSession, vReq);
        } catch (VersionResolutionException e) {
            throw new MojoExecutionException("Failed to resolve version", e);
        }

        System.out.println("  version=" + vRes.getVersion());
    }

    private void artifactRequest(final Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        final ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch ( ArtifactResolutionException e ) {
            throw new RuntimeException("failed to resolve artifact "+artifact, e);
        }
        System.out.println(artifact.toString() + " " + result.getArtifact().getFile().getAbsolutePath());

        final File targetFile = new File(project.getBasedir(), result.getArtifact().getFile().getName());
        if(targetFile.exists()) {
            targetFile.delete();
        }
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            fos = new FileOutputStream(targetFile);
            fis = new FileInputStream(result.getArtifact().getFile());
            IOUtil.copy(fis, fos);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtil.close(fos);
            IOUtil.close(fis);
        }
    }

    private ArtifactRequest getArtifactRequest(GAV gav) {
        final ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), "zip", gav.getVersion()));
        req.setRepositories(remoteRepos);
        return req;
    }
}
