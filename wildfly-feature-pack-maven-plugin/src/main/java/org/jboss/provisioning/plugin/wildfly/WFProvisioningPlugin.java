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

package org.jboss.provisioning.plugin.wildfly;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.PMException;
import org.jboss.provisioning.util.plugin.ProvisioningContext;
import org.jboss.provisioning.util.plugin.ProvisioningPlugin;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFProvisioningPlugin implements ProvisioningPlugin {

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void execute(ProvisioningContext ctx) throws PMException {

        System.out.println("WF CONFIG ASSEMBLER layout=" + ctx.getLayoutDir() + " install-dir=" + ctx.getInstallDir());

        try(DirectoryStream<Path> groups = Files.newDirectoryStream(ctx.getLayoutDir())) {
            for(Path groupDir : groups) {
                try(DirectoryStream<Path> artifacts = Files.newDirectoryStream(groupDir)) {
                    for(Path artifactDir : artifacts) {
                        try(DirectoryStream<Path> versions = Files.newDirectoryStream(artifactDir)) {
                            for(Path versionDir : versions) {

                            }
                        } catch(IOException e) {
                            throw new PMException(Errors.readDirectory(artifactDir));
                        }
                    }
                } catch(IOException e) {
                    throw new PMException(Errors.readDirectory(groupDir));
                }
            }
        } catch (IOException e) {
            throw new PMException(Errors.readDirectory(ctx.getLayoutDir()));
        }
    }
}
