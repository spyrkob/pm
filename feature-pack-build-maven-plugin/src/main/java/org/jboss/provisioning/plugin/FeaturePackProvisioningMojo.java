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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

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
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.PMException;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.InstallationDescriptionBuilder;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.util.FeaturePackInstallException;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.FeaturePackLayoutInstaller;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.util.plugin.ProvisioningContext;
import org.jboss.provisioning.util.plugin.ProvisioningPlugin;
import org.jboss.provisioning.xml.ProvisioningMetaData;
import org.jboss.provisioning.xml.ProvisioningXmlParser;

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

    private Path workDir;
    private Set<GAV> provisioningPlugins = Collections.emptySet();

    private void addProvisioningPlugin(GAV gav) {
        switch(provisioningPlugins.size()) {
            case 0:
                provisioningPlugins = Collections.singleton(gav);
                break;
            case 1:
                provisioningPlugins = new LinkedHashSet<GAV>(provisioningPlugins);
            default:
                provisioningPlugins.add(gav);
        }
    }

    private void addAllProvisioningPlugins(List<GAV> gavs) {
        for(GAV gav : gavs) {
            addProvisioningPlugin(gav);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

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

        final InstallationDescriptionBuilder descrBuilder = InstallationDescriptionBuilder.newInstance();
        workDir = IoUtils.createRandomTmpDir();
        final Path layoutDir = workDir.resolve("layout");
        try {
            layoutFeaturePacks(metadata.getFeaturePacks(), descrBuilder, layoutDir);
            if (!Files.exists(installDir)) {
                mkdirs(installDir);
            }
            FeaturePackLayoutInstaller.install(layoutDir, descrBuilder.build(), installDir);

            if(!provisioningPlugins.isEmpty()) {
                executePlugins(installDir, layoutDir);
            }
        } catch (InstallationDescriptionException | FeaturePackInstallException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    private void executePlugins(final Path installDir, final Path layoutDir) throws MojoExecutionException {
        final List<java.net.URL> urls = new ArrayList<java.net.URL>();
        for(ArtifactResult res : resolveArtifacts(provisioningPlugins, "jar")) {
            final Artifact artifact = res.getArtifact();
            if(!res.isResolved()) {
                throw new MojoExecutionException(FPMavenErrors.artifactResolution(artifact));
            }
            if(res.isMissing()) {
                throw new MojoExecutionException(FPMavenErrors.artifactMissing(artifact));
            }
            try {
                urls.add(artifact.getFile().toURI().toURL());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!urls.isEmpty()) {
            final ProvisioningContext ctx = new ProvisioningContext() {
                @Override
                public Path getLayoutDir() {
                    return layoutDir;
                }
                @Override
                public Path getInstallDir() {
                    return installDir;
                }
                @Override
                public Path getResourcesDir() {
                    return workDir.resolve("resources");
                }
            };
            final java.net.URLClassLoader ucl = new java.net.URLClassLoader(
                    urls.toArray(new java.net.URL[urls.size()]),
                    Thread.currentThread().getContextClassLoader());
            final ServiceLoader<ProvisioningPlugin> plugins = ServiceLoader.load(ProvisioningPlugin.class, ucl);
            for (ProvisioningPlugin plugin : plugins) {
                try {
                    plugin.execute(ctx);
                } catch (PMException e) {
                    throw new MojoExecutionException("Provisioning plugin failed", e);
                }
            }
        }
    }

    private void layoutFeaturePacks(Collection<GAV> fpGavs, InstallationDescriptionBuilder descr, Path layoutDir) throws MojoExecutionException {
        for (ArtifactResult res : resolveArtifacts(fpGavs, "zip")) {
            final Artifact fpArtifact = res.getArtifact();
            if(!res.isResolved()) {
                throw new MojoExecutionException(FPMavenErrors.artifactResolution(fpArtifact));
            }
            if(res.isMissing()) {
                throw new MojoExecutionException(FPMavenErrors.artifactMissing(fpArtifact));
            }
            final Path fpWorkDir = layoutDir.resolve(fpArtifact.getGroupId()).resolve(fpArtifact.getArtifactId())
                    .resolve(fpArtifact.getVersion());
            mkdirs(fpWorkDir);
            try {
                System.out.println("Adding " + fpArtifact.getGroupId() + ":" + fpArtifact.getArtifactId() + ":" + fpArtifact.getVersion() +
                        " to layout " + fpWorkDir);
                ZipUtils.unzip(Paths.get(fpArtifact.getFile().getAbsolutePath()), fpWorkDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to unzip " + fpArtifact.getFile().getAbsolutePath() + " to "
                        + layoutDir, e);
            }
        }

        for(GAV fpGav : fpGavs) {
            final FeaturePackDescription fpDescr;
            try {
                fpDescr = FeaturePackLayoutDescriber.describeFeaturePack(LayoutUtils.getFeaturePackDir(layoutDir, fpGav));
            } catch (InstallationDescriptionException e) {
                throw new MojoExecutionException("Failed to describe feature-pack " + fpGav, e);
            }
            if(fpDescr.hasDependencies()) {
                final Collection<FeaturePackDependencyDescription> depsDescr = fpDescr.getDependencies();
                final List<GAV> deps;
                if(depsDescr.size() == 1) {
                    deps = Collections.singletonList(depsDescr.iterator().next().getGAV());
                } else {
                    deps = new ArrayList<GAV>(depsDescr.size());
                    for (FeaturePackDependencyDescription depDescr : depsDescr) {
                        deps.add(depDescr.getGAV());
                    }
                }
                layoutFeaturePacks(deps, descr, layoutDir);
            }

            final Path fpWorkDir = layoutDir.resolve(fpGav.getGroupId()).resolve(fpGav.getArtifactId()).resolve(fpGav.getVersion());
            final Path fpResources = fpWorkDir.resolve("resources");
            if(Files.exists(fpResources)) {
                try {
                    IoUtils.copy(fpResources, workDir.resolve("resources"));
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy " + fpGav + " resources", e);
                }
            }

            if(fpDescr.hasProvisioningPlugins()) {
                addAllProvisioningPlugins(fpDescr.getProvisioningPlugins());
            }

            try {
                descr.addFeaturePack(fpDescr);
            } catch (InstallationDescriptionException e) {
                throw new MojoExecutionException("Failed to layout feature packs", e);
            }
        }
    }

    private List<ArtifactResult> resolveArtifacts(final Collection<GAV> fpGavs, String extension) throws MojoExecutionException {
        final List<ArtifactRequest> requests;
        if (fpGavs.size() == 1) {
            requests = Collections.singletonList(getArtifactRequest(fpGavs.iterator().next(), extension));
        } else {
            requests = new ArrayList<ArtifactRequest>(fpGavs.size());
            for (GAV gav : fpGavs) {
                requests.add(getArtifactRequest(gav, extension));
            }
        }
        try {
            return repoSystem.resolveArtifacts(repoSession, requests);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(FPMavenErrors.artifactResolution(fpGavs), e);
        }
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

    private ArtifactRequest getArtifactRequest(GAV gav, String extension) {
        final ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), extension, gav.getVersion()));
        req.setRepositories(remoteRepos);
        return req;
    }
}
