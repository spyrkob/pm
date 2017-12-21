/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.wildfly.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.XMLStreamException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.IoUtils;
import org.wildfly.core.embedded.EmbeddedProcessFactory;
import org.wildfly.core.embedded.EmbeddedProcessStartException;
import org.wildfly.core.embedded.HostController;
import org.wildfly.core.embedded.StandaloneServer;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@Mojo(name = "wf-spec", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class WfFeatureSpecBuildMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(required = true)
    private File outputDirectory;

    @Parameter(required = true)
    private String moduleDirectory;

    @Parameter(required = false)
    private List<ArtifactItem> featurePacks;

    @Parameter(required = true)
    private List<String> standaloneExtensions;

    @Parameter(required = true)
    private List<String> domainExtensions;

    @Parameter(required = true)
    private List<String> hostExtensions;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    private MavenResourcesFiltering mavenResourcesFiltering;

    @Component
    private ArchiverManager archiverManager;

    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private ArtifactResolver artifactResolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        } catch (RuntimeException | Error | MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (IOException | MavenFilteringException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void doExecute() throws MojoExecutionException, MojoFailureException, MavenFilteringException, IOException {
        List<org.apache.maven.model.Resource> modulesResources = new ArrayList<>();
        org.apache.maven.model.Resource srcModuleResource = new org.apache.maven.model.Resource();
        srcModuleResource.setDirectory(moduleDirectory);
        srcModuleResource.setFiltering(true);
        modulesResources.add(srcModuleResource);
        Path tmpModules = Files.createTempDirectory("modules");
        List<Artifact> featurePackArtifacts = new ArrayList<>();
        if (featurePacks != null && !featurePacks.isEmpty()) {
            IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
            selector.setIncludes(new String[]{"**/**/module/modules/**/*"});
            IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[]{selector};
            for (ArtifactItem fp : featurePacks) {
                Artifact fpArtifact = findArtifact(fp);
                if (fpArtifact != null) {
                    featurePackArtifacts.add(fpArtifact);
                    File archive = fpArtifact.getFile();
                    Path tmpArchive = Files.createTempDirectory(fp.toString());
                    try {
                        UnArchiver unArchiver;
                        try {
                            unArchiver = archiverManager.getUnArchiver(fpArtifact.getType());
                            getLog().debug("Found unArchiver by type: " + unArchiver);
                        } catch (NoSuchArchiverException e) {
                            unArchiver = archiverManager.getUnArchiver(archive);
                            getLog().debug("Found unArchiver by extension: " + unArchiver);
                        }
                        unArchiver.setFileSelectors(selectors);
                        unArchiver.setSourceFile(archive);
                        unArchiver.setDestDirectory(tmpArchive.toFile());
                        unArchiver.extract();
                        setModules(tmpArchive, tmpModules.resolve("modules"));
                    } catch (NoSuchArchiverException ex) {
                        getLog().warn(ex);
                    }
                } else {
                    getLog().warn("No artifact was found for " + fp);
                }
            }
            org.apache.maven.model.Resource fpModuleResource = new org.apache.maven.model.Resource();
            fpModuleResource.setDirectory(tmpModules.resolve("modules").toString());
            fpModuleResource.setFiltering(true);
            modulesResources.add(fpModuleResource);
        }
        setArtifactVersions(featurePackArtifacts);
        Path wildfly = outputDirectory.toPath().resolve("wildfly");
        Files.createDirectories(wildfly.resolve("standalone").resolve("configuration"));
        Files.createDirectories(wildfly.resolve("domain").resolve("configuration"));
        MavenResourcesExecution mavenResourcesExecution
                = new MavenResourcesExecution(modulesResources, wildfly.resolve("modules").toFile(), project,
                        "UTF-8", Collections.emptyList(),
                        Collections.emptyList(), session);
        mavenResourcesExecution.setOverwrite(true);
        mavenResourcesExecution.setIncludeEmptyDirs(true);
        mavenResourcesExecution.setSupportMultiLineFiltering(false);
        mavenResourcesExecution.setFilterFilenames(false);
        mavenResourcesExecution.setAddDefaultExcludes(true);
        mavenResourcesFiltering.filterResources(mavenResourcesExecution);

        List<String> lines = new ArrayList<>(standaloneExtensions.size() + 5);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<server xmlns=\"urn:jboss:domain:6.0\">");
        lines.add("<extensions>");
        for (String extension : standaloneExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("</server>");
        Files.write(wildfly.resolve("standalone").resolve("configuration").resolve("standalone.xml"), lines);

        System.setProperty("org.wildfly.logging.skipLogManagerCheck", "true");
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        Properties props = (Properties) System.getProperties().clone();
        StandaloneServer server = EmbeddedProcessFactory.createStandaloneServer(wildfly.toAbsolutePath().toString(), wildfly.resolve("modules").toAbsolutePath().toString(), null, new String[]{"--admin-only"});
        try {
            server.start();
            try (ModelControllerClient client = server.getModelControllerClient()) {
                ModelNode address = new ModelNode().setEmptyList();
                ModelNode op = Operations.createOperation("read-feature", address);
                op.get("recursive").set(true);
                ModelNode result = client.execute(op);
                Files.write(wildfly.resolve("standalone_features.dmr"), Collections.singletonList(result.toString()));
                FeatureSpecExporter.export(result, outputDirectory.toPath());
            } catch (XMLStreamException | ProvisioningDescriptionException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        } catch (EmbeddedProcessStartException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } finally {
            server.stop();
            clearXMLConfiguration(props);
        }
        lines = new ArrayList<>(domainExtensions.size() + 8);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<domain xmlns=\"urn:jboss:domain:6.0\">");
        lines.add("<extensions>");
        for (String extension : domainExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("</domain>");
        Files.write(wildfly.resolve("domain").resolve("configuration").resolve("domain.xml"), lines);
        lines = new ArrayList<>(14);
        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<host xmlns=\"urn:jboss:domain:6.0\" name=\"master\">");
        lines.add("<extensions>");
        for (String extension : hostExtensions) {
            lines.add(String.format("<extension module=\"%s\"/>", extension));
        }
        lines.add("</extensions>");
        lines.add("<management>");
        lines.add("</management>");
        lines.add("<domain-controller>");
        lines.add("<local />");
        lines.add("</domain-controller>");
        lines.add("</host>");
        Files.write(wildfly.resolve("domain").resolve("configuration").resolve("host.xml"), lines);
        HostController host = EmbeddedProcessFactory.createHostController(wildfly.toAbsolutePath().toString(), wildfly.resolve("modules").toAbsolutePath().toString(), null, new String[]{"--admin-only"});
        try {
            host.start();
            try (ModelControllerClient client = host.getModelControllerClient()) {
                ModelNode address = new ModelNode().setEmptyList();
                ModelNode op = Operations.createOperation("read-feature", address);
                op.get("recursive").set(true);
                ModelNode result = client.execute(op);
                Files.write(wildfly.resolve("domain_features.dmr"), Collections.singletonList(result.toString()));
                FeatureSpecExporter.export(result, outputDirectory.toPath());
            } catch (XMLStreamException | ProvisioningDescriptionException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        } catch (EmbeddedProcessStartException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } finally {
            host.stop();
            clearXMLConfiguration(props);
        }
    }

    private void setModules(Path fpDirectory, Path moduleDir) throws IOException {
        Files.walkFileTree(fpDirectory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isModule(dir)) {
                    getLog().info("Copying " + dir + " to " + moduleDir);
                    IoUtils.copy(dir, moduleDir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isModule(Path dir) {
        return "modules".equals(dir.getFileName().toString())
                && "module".equals(dir.getParent().getFileName().toString())
                && "wildfly".equals(dir.getParent().getParent().getFileName().toString())
                && "pm".equals(dir.getParent().getParent().getParent().getFileName().toString())
                && "packages".equals(dir.getParent().getParent().getParent().getParent().getParent().getFileName().toString());
    }

    private Artifact findArtifact(ArtifactItem featurePack) throws MojoExecutionException {
        try {
        ProjectBuildingRequest buildingRequest
                = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
        ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, featurePack);
        if(result != null) {
            return result.getArtifact();
        }
        return null;
        } catch (ArtifactResolverException e) {
            throw new MojoExecutionException("Couldn't resolve artifact: " + e.getMessage(), e);
        }
    }

    private void setArtifactVersions(List<Artifact> featurePackArtifacts) throws MojoExecutionException {
        for (Artifact artifact : project.getArtifacts()) {
            final StringBuilder buf = new StringBuilder(artifact.getGroupId()).append(':').
                    append(artifact.getArtifactId());
            final String classifier = artifact.getClassifier();
            final StringBuilder version = new StringBuilder(buf);
            if (classifier != null && !classifier.isEmpty()) {
                buf.append("::").append(classifier);
                version.append(':').append(artifact.getVersion()).append(':').append(classifier);
            } else {
                version.append(':').append(artifact.getVersion());
            }
            project.getProperties().put(buf.toString(), version.toString());
        }
        for (Artifact featurePackArtifact : featurePackArtifacts) {
            for (ArtifactResult result : resolveDependencies(featurePackArtifact)) {
                Artifact dep = result.getArtifact();
                final StringBuilder buf = new StringBuilder(dep.getGroupId()).append(':').
                        append(dep.getArtifactId());
                final String classifier = dep.getClassifier();
                final StringBuilder version = new StringBuilder(buf);
                if (classifier != null && !classifier.isEmpty()) {
                    buf.append("::").append(classifier);
                    version.append(':').append(dep.getVersion()).append(':').append(classifier);
                } else {
                    version.append(':').append(dep.getVersion());
                }
                project.getProperties().put(buf.toString(), version.toString());
            }
        }
    }

    private Iterable<ArtifactResult> resolveDependencies(Artifact artifact) throws MojoExecutionException {
        try {
            DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
            coordinate.setGroupId(artifact.getGroupId());
            coordinate.setArtifactId(artifact.getArtifactId());
            coordinate.setVersion(artifact.getVersion());
            coordinate.setType(artifact.getType());
            coordinate.setClassifier(artifact.getClassifier());
            ProjectBuildingRequest buildingRequest
                    = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

            buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
            getLog().info("Resolving " + coordinate + " with transitive dependencies");
            return dependencyResolver.resolveDependencies(buildingRequest, coordinate, null);
        } catch (DependencyResolverException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }
    }

    private void clearXMLConfiguration(Properties props) {
        clearProperty(props, "javax.xml.parsers.DocumentBuilderFactory");
        clearProperty(props, "javax.xml.parsers.SAXParserFactory");
        clearProperty(props, "javax.xml.transform.TransformerFactory");
        clearProperty(props, "javax.xml.xpath.XPathFactory");
        clearProperty(props, "javax.xml.stream.XMLEventFactory");
        clearProperty(props, "javax.xml.stream.XMLInputFactory");
        clearProperty(props, "javax.xml.stream.XMLOutputFactory");
        clearProperty(props, "javax.xml.datatype.DatatypeFactory");
        clearProperty(props, "javax.xml.validation.SchemaFactory");
        clearProperty(props, "org.xml.sax.driver");
    }

    private void clearProperty(Properties props, String name) {
        if (props.containsKey(name)) {
            System.setProperty(name, props.getProperty(name));
        } else {
            System.clearProperty(name);
        }
    }
}
