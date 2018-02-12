/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Argument;
import org.aesh.io.Resource;
import org.aesh.readline.AeshContext;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="cd", description="Changes the current work dir to the specified location")
public class CdCommand extends PmSessionCommand {

    @Argument
    private Resource argument;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        final AeshContext aeshCtx = session.getAeshContext();
        if (argument != null) {
            final List<Resource> files = argument.resolve(aeshCtx.getCurrentWorkingDirectory());
            if (files.get(0).isDirectory()) {
                aeshCtx.setCurrentWorkingDirectory(files.get(0));
            }
        }
        session.updatePrompt(session.getAeshContext());
    }
}