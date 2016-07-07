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
package org.jboss.pm.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.pm.Constants;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="pm", description="pm description")
class PmCommand extends CommandBase {

    private static final String PROVISIONING_POM_XML = "maven/provisioning-pom.xml";

    @Option(name="provisioning-xml")
    private String provisioningXmlArg;

    @Option(name="install-dir")
    private String installDirArg;

    @Override
    protected void runCommand(CommandInvocation ci) throws CommandExecutionException {

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

        final Path workDir = Util.createRandomTmpDir();
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
                    throw new IllegalStateException("Build failed.");
                }
            } catch (MavenInvocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException e1) {
            throw new CommandExecutionException("Failed to copy pom.xml to the work dir.");
        } finally {
            Util.recursiveDelete(workDir);
        }

    }
}