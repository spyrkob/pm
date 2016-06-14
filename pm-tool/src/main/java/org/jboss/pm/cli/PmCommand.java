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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.IOUtil;
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
    private String provisioningXml;

    @Override
    public CommandResult execute(CommandInvocation ci) throws IOException, InterruptedException {

        final File provisioningFile;
        if(provisioningXml == null) {
            provisioningFile = new File(Constants.PROVISIONING_XML);
        } else {
            provisioningFile = new File(provisioningXml);
        }
        if(!provisioningFile.exists()) {
            if(provisioningXml == null) {
                ci.println("Error: failed to locate provisioning file at default location " + provisioningFile.getAbsolutePath());
                ci.println("Hint: use --provisioning-xml argument to point to the desired provisioning spec");
            } else {
                ci.println("Error: failed to locate provisioning file " + provisioningFile.getAbsolutePath());
            }
            return CommandResult.FAILURE;
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream pomIs = cl.getResourceAsStream("maven/pom.xml");
        if(pomIs == null) {
            throw new IllegalStateException("maven/pom.xml not found");
        }

        final File workDir = Util.createRandomTmpDir();

        try {
            Util.copy(provisioningFile, new File(workDir, Constants.PROVISIONING_XML));

            InvocationRequest request = new DefaultInvocationRequest();

            final Properties props = new Properties();
            props.setProperty(Constants.TOOL_BASE_DIR, Paths.get("").toAbsolutePath().toString());
            request.setProperties(props);

            request.setPomFile(extractPom(pomIs, workDir));
            request.setGoals(Collections.singletonList("package"));

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

    private File extractPom(final InputStream pomIs, final File workDir) throws FileNotFoundException, IOException {
        final File tmpPom = new File(workDir, "pom.xml");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpPom);
            IOUtil.copy(pomIs, fos);
        } finally {
            IOUtil.close(pomIs);
            IOUtil.close(fos);
        }
        return tmpPom;
    }
}