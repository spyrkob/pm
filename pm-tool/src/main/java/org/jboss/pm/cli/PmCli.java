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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.IOUtil;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class PmCli {

    public static void main(String[] args) throws Exception {
        Settings settings = new SettingsBuilder().logging(true).create();
        AeshConsole aeshConsole = new AeshConsoleBuilder().settings(settings).prompt(new Prompt("[pm] "))
                .addCommand(new ExitCommand())
                .addCommand(new PmCommand())
                .create();

        aeshConsole.start();
    }

    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="pm", description="pm description")
    public static class PmCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws IOException, InterruptedException {

            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final InputStream pomIs = cl.getResourceAsStream("maven/pom.xml");
            if(pomIs == null) {
                throw new IllegalStateException("maven/pom.xml not found");
            }

            final File tmpPom = File.createTempFile("jboss", "pm");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tmpPom);
                IOUtil.copy(pomIs, fos);
            } finally {
                IOUtil.close(pomIs);
                IOUtil.close(fos);
            }

            try {
                InvocationRequest request = new DefaultInvocationRequest();
                request.setPomFile(tmpPom);
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
                tmpPom.delete();
            }

            return CommandResult.SUCCESS;
        }
    }
}
