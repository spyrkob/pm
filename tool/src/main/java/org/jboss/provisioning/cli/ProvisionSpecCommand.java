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
import java.util.List;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.io.Resource;
import org.jboss.provisioning.ProvisioningException;


/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="provision-spec", description="(Re)Provisions the installation according to the specification provided in an XML file")
public class ProvisionSpecCommand extends ProvisioningCommand {

    @Arguments(completer=FileOptionCompleter.class, description="File describing the desired provisioned state.")
    private List<Resource> specArg;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {

        if(specArg == null || specArg.isEmpty()) {
            throw new CommandExecutionException("Missing required file path argument.");
        }
        if(specArg.size() > 1) {
            throw new CommandExecutionException("The command expects only one argument.");
        }

        final Resource specResource = specArg.get(0).resolve(session.getAeshContext().getCurrentWorkingDirectory()).get(0);
        final Path provisioningFile = Paths.get(specResource.getAbsolutePath());
        if(!Files.exists(provisioningFile)) {
            throw new CommandExecutionException("Failed to locate provisioning file " + provisioningFile.toAbsolutePath());
        }
        try {
            getManager(session).provision(provisioningFile);
        } catch (ProvisioningException e) {
            throw new CommandExecutionException("Provisioning failed", e);
        }
    }
}