/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
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
@CommandDefinition(name="export", description="Saves current provisioned spec into the specified file.")
public class ProvisionedSpecExportCommand extends ProvisioningCommand {

    @Arguments(completer=FileOptionCompleter.class, description="File to save the provisioned spec too.")
    private List<Resource> fileArg;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        if(fileArg == null || fileArg.isEmpty()) {
            throw new CommandExecutionException("Missing required file path argument.");
        }
        if(fileArg.size() > 1) {
            throw new CommandExecutionException("The command expects only one argument.");
        }

        final Resource specResource = fileArg.get(0).resolve(session.getAeshContext().getCurrentWorkingDirectory()).get(0);
        final Path targetFile = Paths.get(specResource.getAbsolutePath());

        try {
            getManager(session).exportProvisioningConfig(targetFile);
        } catch (ProvisioningException | IOException e) {
            throw new CommandExecutionException("Failed to export provisioned state", e);
        }
    }
}
