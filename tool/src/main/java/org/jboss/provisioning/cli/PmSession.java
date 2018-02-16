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

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.readline.AeshContext;
import org.aesh.readline.Prompt;

/**
 *
 * @author Alexey Loubyansky
 */
public class PmSession implements CommandInvocationProvider<PmCommandInvocation>, CompleterInvocationProvider<PmCompleterInvocation> {

    private PrintStream out;
    private PrintStream err;
    private final Configuration config;
    private final Universes universes;

    public PmSession(Configuration config) throws Exception {
        this.config = config;
        //Build the universes
        this.universes = Universes.buildUniverses(MavenArtifactRepositoryManager.getInstance(), config.getUniversesLocations());
    }

    public Configuration getPmConfiguration() {
        return config;
    }

    public Universes getUniverses() {
        return universes;
    }

    // TO REMOVE when we have an universe for sure.
    public boolean hasPopulatedUniverse() {
        for (Universe u : universes.getUniverses()) {
            if (!u.getStreamLocations().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static Prompt buildPrompt(AeshContext aeshCtx) {
        return new Prompt(new StringBuilder().append('[')
                .append(aeshCtx.getCurrentWorkingDirectory().getName())
                .append("]$ ").toString());
    }

    static Path getWorkDir(AeshContext aeshCtx) {
        return Paths.get(aeshCtx.getCurrentWorkingDirectory().getAbsolutePath());
    }

    @Override
    public PmCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new PmCommandInvocation(this, out, err, commandInvocation);
    }

    void setOut(PrintStream out) {
        this.out = out;
    }

    void setErr(PrintStream err) {
        this.err = err;
    }

    @Override
    public PmCompleterInvocation enhanceCompleterInvocation(CompleterInvocation completerInvocation) {
        return new PmCompleterInvocation(completerInvocation, this);
    }
}
