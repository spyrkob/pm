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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

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
import org.jboss.pm.descr.InstallationDescriptionException;
import org.jboss.pm.util.IoUtils;
import org.jboss.pm.wildfly.descr.WFFeaturePackLayoutBuilder;
import org.jboss.pm.wildfly.descr.WFInstallationDescription;
import org.jboss.pm.wildfly.xml.WFInstallationDefParser;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="fp", description = "fp builder")
public class FpCommand extends CommandBase {

    private static final String INSTALL_FEATURE_PACKS_POM = "maven/install-feature-packs-pom.xml";

    private static final String WF_FP_DEF_XML = "wildfly-feature-pack-def.xml";

    @Option(name="install-dir", required=true)
    private String installDirArg;

    @Option(name="workdir", required=false)
    private String fpWorkDir;

    @Override
    protected void runCommand(CommandInvocation ci) throws CommandExecutionException {

        final Path installDir = Paths.get(installDirArg);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream wfInstallDef = cl.getResourceAsStream(WF_FP_DEF_XML);
        if(wfInstallDef == null) {
            throw new CommandExecutionException(WF_FP_DEF_XML + " not found");
        }

        final WFInstallationDescription wfDescr;
        try {
            wfDescr = new WFInstallationDefParser().parse(wfInstallDef);
        } catch (XMLStreamException e) {
            throw new CommandExecutionException("failed to parse " + WF_FP_DEF_XML, e);
        }

        final Path workDir;
        final boolean deleteWorkDir;
        if(fpWorkDir != null) {
            workDir = Paths.get(fpWorkDir);
            deleteWorkDir = false;
        } else {
            workDir = IoUtils.createRandomTmpDir();
            deleteWorkDir = true;
        }

        try {
            final Path fpsDir = workDir.resolve(Constants.FEATURE_PACKS);
            WFFeaturePackLayoutBuilder layoutBuilder = new WFFeaturePackLayoutBuilder();
            try {
                layoutBuilder.build(wfDescr, installDir, fpsDir);
            } catch (InstallationDescriptionException e) {
                throw new CommandExecutionException("Failed to layout feature packs", e);
            }
            install(workDir);
        } finally {
            if (deleteWorkDir) {
                IoUtils.recursiveDelete(workDir);
            }
        }
    }

    private void install(final Path workDir) throws CommandExecutionException {
        final InputStream pomIs = Util.getResourceStream(INSTALL_FEATURE_PACKS_POM);
        try {
            InvocationRequest request = new DefaultInvocationRequest();

            final Properties props = new Properties();
            props.setProperty(Constants.PM_INSTALL_WORK_DIR, workDir.toAbsolutePath().toString());
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
        } catch (IOException e) {
            throw new CommandExecutionException("Failed to copy " + INSTALL_FEATURE_PACKS_POM);
        }
    }
}
