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

import java.util.List;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;


/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="install", description="Installs specified feature-packs")
public class InstallCommand extends ProvisioningCommand {

    @Arguments(completer=GavCompleter.class)
    private List<String> coords;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {

        final ProvisioningManager manager = getManager(session);
        try {
            for(String coord : coords) {
                manager.install(ArtifactCoords.newGav(coord));
            }
        } catch (ProvisioningException e) {
            throw new CommandExecutionException("Provisioning failed", e);
        }
    }
}