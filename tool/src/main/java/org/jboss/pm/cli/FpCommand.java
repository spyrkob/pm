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
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.pm.def.InstallationDef;
import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.wildfly.xml.WFInstallationDefParser;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="fp", description = "fp builder")
public class FpCommand implements Command<CommandInvocation> {

    @Option(name="install-dir", required=true)
    private String installDirArg;

    @Override
    public CommandResult execute(CommandInvocation ci) throws IOException, InterruptedException {

        final String toolHome = new File("").getAbsolutePath();

        final File installDir = new File(installDirArg);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream wfInstallDef = cl.getResourceAsStream("wildfly-feature-pack-def.xml");
        if(wfInstallDef == null) {
            ci.println("Error: wildfly-feature-pack-def.xml not found");
            return CommandResult.FAILURE;
        }

        try {
            final InstallationDef wfInstallation = new WFInstallationDefParser().parse(wfInstallDef).build(installDir);
            ci.println(wfInstallation.logContent());
        } catch (XMLStreamException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InstallationDefException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

/*        try {
            final InstallationDef installDef = InstallationDefBuilder.newInstance()
                    .defineFeaturePack(new GAV("org.jboss.pm", "test", "1.0.0-SNAPSHOT"), new File(toolHome))
                    .build();
            ci.println(installDef.logContent());
        } catch (InstallationDefException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        return CommandResult.SUCCESS;
    }

}
