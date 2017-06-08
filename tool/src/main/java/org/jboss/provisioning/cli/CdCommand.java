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
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.io.Resource;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="cd", description="Changes the current work dir to the specified location")
class CdCommand extends PmSessionCommand {

    @Arguments
    private List<Resource> arguments;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        final AeshContext aeshCtx = session.getAeshContext();
        if (arguments != null) {
            final List<Resource> files = arguments.get(0).resolve(aeshCtx.getCurrentWorkingDirectory());
            if (files.get(0).isDirectory()) {
                aeshCtx.setCurrentWorkingDirectory(files.get(0));
            }
        }
        session.updatePrompt(session.getAeshContext());
    }
}