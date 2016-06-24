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

import javax.xml.stream.XMLStreamException;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.pm.build.FeaturePackBuild;
import org.jboss.pm.build.PMBuildException;
import org.jboss.pm.def.InstallationDef;
import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.util.IoUtils;
import org.jboss.pm.wildfly.xml.WFInstallationDefParser;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="fp", description = "fp builder")
public class FpCommand extends CommandBase {

    private static final String WF_FP_DEF_XML = "wildfly-feature-pack-def.xml";

    @Option(name="install-dir", required=true)
    private String installDirArg;

    @Override
    protected void runCommand(CommandInvocation ci) throws CommandExecutionException {

        final File installDir = new File(installDirArg);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream wfInstallDef = cl.getResourceAsStream(WF_FP_DEF_XML);
        if(wfInstallDef == null) {
            throw new CommandExecutionException(WF_FP_DEF_XML + " not found");
        }

        final InstallationDef wfInstallation;
        try {
            wfInstallation = new WFInstallationDefParser().parse(wfInstallDef).build(installDir);
        } catch (XMLStreamException e) {
            throw new CommandExecutionException("failed to parse " + WF_FP_DEF_XML, e);
        } catch (InstallationDefException e) {
            throw new CommandExecutionException("failed to build feature packs", e);
        }

/*        try {
            ci.println(wfInstallation.logContent());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        final File workDir = Util.createRandomTmpDir();
        try {
            FeaturePackBuild fpBuild = new FeaturePackBuild(wfInstallation, installDir, workDir);
            fpBuild.buildFeaturePacks();

            final File tmpDir = new File(new File("").getAbsolutePath(), "workdir");
            tmpDir.mkdir();
            IoUtils.copyFile(workDir, tmpDir);
        } catch (PMBuildException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Util.recursiveDelete(workDir);
        }
    }
}
