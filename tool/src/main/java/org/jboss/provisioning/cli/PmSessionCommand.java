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

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PmSessionCommand implements Command<PmSession> {

    @Override
    public CommandResult execute(PmSession session) throws CommandException {
        try {
            runCommand(session);
            return CommandResult.SUCCESS;
        } catch (Throwable t) {
            if(t instanceof RuntimeException) {
                t.printStackTrace(session.getShell().err());
            }
            final StringBuilder buf = new StringBuilder("Error");
            while(t != null) {
                buf.append(": ");
                if(t.getLocalizedMessage() == null) {
                    buf.append(t.getClass().getName());
                } else {
                    buf.append(t.getLocalizedMessage());
                }
                t = t.getCause();
            }
            session.println(buf.toString());
            return CommandResult.FAILURE;
        }
    }

    protected abstract void runCommand(PmSession session) throws CommandExecutionException;
}
