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
package org.jboss.provisioning.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="pm", description="Provisioning Manager CLI interface")
class PmCommand extends CommandBase {

    private static final String PROVISIONING_POM_XML = "maven/provisioning-pom.xml";

    @Option(name="provisioning-xml", completer=FileOptionCompleter.class)
    private String provisioningXmlArg;

    @Option(name="install-dir", completer=FileOptionCompleter.class)
    private String installDirArg;

    @Override
    protected void runCommand(CommandInvocation ci) throws CommandExecutionException {
/*
        final RepositorySystem repoSystem = newRepositorySystem();
        System.out.println("repoSystem=" + repoSystem);
        RepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(new DefaultArtifact("org.wildfly.core:wildfly-core-feature-pack-new:3.0.0.Alpha9-SNAPSHOT"));
        try {
            repoSystem.resolveArtifact(session, req);
        } catch (ArtifactResolutionException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
*/
        final String toolHome = Paths.get("").toAbsolutePath().toString();

        final Path provisioningFile;
        if(provisioningXmlArg == null) {
            provisioningFile = Paths.get(toolHome, Constants.PROVISIONING_XML);
        } else {
            provisioningFile = Paths.get(provisioningXmlArg);
        }
        if(!Files.exists(provisioningFile)) {
            if(provisioningXmlArg == null) {
                throw new CommandExecutionException("Failed to locate provisioning file at default location " + provisioningFile.toAbsolutePath());
            } else {
                throw new CommandExecutionException("Failed to locate provisioning file " + provisioningFile.toAbsolutePath());
            }
        }

        final File installDir;
        if(installDirArg == null) {
            installDir = new File(toolHome);
        } else {
            installDir = new File(installDirArg);
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream pomIs = cl.getResourceAsStream(PROVISIONING_POM_XML);
        if(pomIs == null) {
            throw new CommandExecutionException(PROVISIONING_POM_XML + " not found");
        }

        final Path workDir = IoUtils.createRandomTmpDir();
        try {
            Files.copy(provisioningFile, workDir.resolve(Constants.PROVISIONING_XML));
        } catch(IOException e) {
            throw new CommandExecutionException("Failed to copy " + provisioningFile.toAbsolutePath() + " to the work dir.");
        }

        try {
            InvocationRequest request = new DefaultInvocationRequest();

            final Properties props = new Properties();
            props.setProperty(Constants.PM_INSTALL_DIR, installDir.getAbsolutePath());
            props.setProperty(Constants.PROVISIONING_XML, provisioningFile.toAbsolutePath().toString());
            request.setProperties(props);

            final Path pomXml = workDir.resolve("pom.xml");
            Files.copy(pomIs, pomXml);
            request.setPomFile(pomXml.toFile());
            request.setGoals(Collections.singletonList("compile"));

            Invoker invoker = new DefaultInvoker();
            InvocationResult result;
            try {
                result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    throw new CommandExecutionException("Provisioning failed. Please, see the errors logged above.");
                }
            } catch (MavenInvocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException e1) {
            throw new CommandExecutionException("Failed to copy pom.xml to the work dir.");
        } finally {
            IoUtils.recursiveDelete(workDir);
        }
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }
}