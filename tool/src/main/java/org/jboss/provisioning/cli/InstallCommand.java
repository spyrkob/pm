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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningException;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="install", description="Installs specified feature-packs")
class InstallCommand extends ProvisioningCommand {

    @Option(name="provisioning-xml", completer=FileOptionCompleter.class, description="File describing the desired provisioned state.")
    private String provisioningXmlArg;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {

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

        try {
            getManager(session).provision(provisioningFile);
        } catch (ProvisioningException e) {
            throw new CommandExecutionException("Provisioning failed", e);
        }
    }
}