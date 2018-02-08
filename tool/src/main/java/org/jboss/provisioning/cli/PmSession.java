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

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.readline.AeshContext;
import org.aesh.readline.Prompt;

/**
 *
 * @author Alexey Loubyansky
 */
public class PmSession extends DelegatingCommandInvocation implements CommandInvocationProvider<PmSession> {

    private Prompt prompt;
    private PrintStream out;
    private PrintStream err;
    void updatePrompt(AeshContext aeshCtx) {
        prompt = new Prompt(new StringBuilder().append('[')
                .append(aeshCtx.getCurrentWorkingDirectory().getName())
                .append("]$ ").toString());
        if(delegate != null) {
            setPrompt(prompt);
        }
    }

    @Override
    public Prompt getPrompt() {
        return prompt;
    }

    Path getWorkDir() {
        return Paths.get(getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());
    }

    Path resolvePath(String path) {
        return getWorkDir().resolve(path);
    }

    @Override
    public PmSession enhanceCommandInvocation(CommandInvocation commandInvocation) {
        commandInvocation.setPrompt(prompt);
        this.delegate = commandInvocation;
        return this;
    }

    void setOut(PrintStream out) {
        this.out = out;
    }

    void setErr(PrintStream err) {
        this.err = err;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }
}
