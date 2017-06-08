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

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Shell;

/**
 *
 * @author Alexey Loubyansky
 */
class DelegatingCommandInvocation implements CommandInvocation {

    protected CommandInvocation delegate;

    @Override
    public ControlOperator getControlOperator() {
        return delegate.getControlOperator();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return delegate.getCommandRegistry();
    }

    @Override
    public Shell getShell() {
        return delegate.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        delegate.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return delegate.getPrompt();
    }

    @Override
    public String getHelpInfo(String commandName) {
        return delegate.getHelpInfo(commandName);
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public AeshContext getAeshContext() {
        return delegate.getAeshContext();
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return delegate.getInput();
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return delegate.getInputLine();
    }

    @Override
    public int getPid() {
        return delegate.getPid();
    }

    @Override
    public void putProcessInBackground() {
        delegate.putProcessInBackground();
    }

    @Override
    public void putProcessInForeground() {
        delegate.putProcessInForeground();
    }

    @Override
    public void executeCommand(String input) {
        delegate.executeCommand(input);
    }

    @Override
    public void print(String msg) {
        delegate.print(msg);
    }

    @Override
    public void println(String msg) {
        delegate.println(msg);
    }

    @Override
    public boolean isEchoing() {
        return delegate.isEchoing();
    }

    @Override
    public void setEcho(boolean echo) {
        delegate.setEcho(echo);
    }

    @Override
    public Command<?> getPopulatedCommand(String commandLine) throws CommandNotFoundException, CommandException,
            CommandLineParserException, OptionValidatorException {
        return delegate.getPopulatedCommand(commandLine);
    }
}
