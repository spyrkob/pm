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
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.pm.Constants;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="pm", description="pm description")
class PmCommand implements Command<CommandInvocation> {

    @Option(name="provisioning-xml")
    private String provisioningXmlArg;

    @Option(name="install-dir")
    private String installDirArg;

    @Override
    public CommandResult execute(CommandInvocation ci) throws IOException, InterruptedException {

        final String toolHome = new File("").getAbsolutePath();

        final File provisioningFile;
        if(provisioningXmlArg == null) {
            provisioningFile = new File(toolHome, Constants.PROVISIONING_XML);
        } else {
            provisioningFile = new File(provisioningXmlArg);
        }
        if(!provisioningFile.exists()) {
            if(provisioningXmlArg == null) {
                ci.println("Error: failed to locate provisioning file at default location " + provisioningFile.getAbsolutePath());
                ci.println("Hint: use --provisioning-xml argument to point to the desired provisioning spec");
            } else {
                ci.println("Error: failed to locate provisioning file " + provisioningFile.getAbsolutePath());
            }
            return CommandResult.FAILURE;
        }

        final File installDir;
        if(installDirArg == null) {
            installDir = new File(toolHome);
        } else {
            installDir = new File(installDirArg);
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream pomIs = cl.getResourceAsStream("maven/build-pom.xml");
        if(pomIs == null) {
            ci.println("Error: maven/pom.xml not found");
            return CommandResult.FAILURE;
        }

        final File workDir = Util.createRandomTmpDir();

        try {
            Util.copy(provisioningFile, new File(workDir, Constants.PROVISIONING_XML));

            InvocationRequest request = new DefaultInvocationRequest();

            final Properties props = new Properties();
            props.setProperty(Constants.PM_TOOL_HOME_DIR, toolHome);
            props.setProperty(Constants.PM_INSTALL_DIR, installDir.getAbsolutePath());
            request.setProperties(props);

            request.setPomFile(Util.saveAs(pomIs, new File(workDir, "pom.xml")));
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
        } finally {
            Util.recursiveDelete(workDir);
        }

        return CommandResult.SUCCESS;
    }
}