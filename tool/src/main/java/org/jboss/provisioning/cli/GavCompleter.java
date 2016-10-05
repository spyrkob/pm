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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.completer.CompleterInvocation;

/**
 *
 * @author Alexey Loubyansky
 */
public class GavCompleter implements OptionCompleter<CompleterInvocation> {

    protected String repoHome = Util.getMavenRepositoryPath();

    @Override
    public void complete(CompleterInvocation ci) {
        Path path = Paths.get(repoHome);
        if(!Files.isDirectory(path)) {
            return;
        }

        final String currentValue = ci.getGivenCompleteValue();
        int level = 1;
        String prefix = null;
        String chunk = null;
        int groupSeparator = currentValue.indexOf(':');
        if(groupSeparator > 0) {
            path = path.resolve(currentValue.substring(0, groupSeparator));
            ++level;
            if(groupSeparator + 1 < currentValue.length()) {
                int artifactSeparator = currentValue.indexOf(':', groupSeparator + 1);
                if(artifactSeparator > 0) {
                    ++level;
                    path = path.resolve(currentValue.substring(groupSeparator + 1, artifactSeparator));
                    if(artifactSeparator + 1 < currentValue.length()) {
                        prefix = currentValue.substring(0, artifactSeparator + 1);
                        chunk = currentValue.substring(artifactSeparator + 1);
                    } else {
                        prefix = currentValue;
                        chunk = null;
                    }
                } else {
                    prefix = currentValue.substring(0, groupSeparator + 1);
                    chunk = currentValue.substring(groupSeparator + 1);
                }
            } else {
                prefix = currentValue;
            }
        } else {
            chunk = currentValue;
        }

        if(!Files.isDirectory(path)) {
            return;
        }

        final List<String> candidates = new ArrayList<String>();
        try {
            addCandidates(path, prefix, chunk, candidates, level);
        } catch (IOException e) {
            return;
        }
        ci.addAllCompleterValues(candidates);
    }

    private void addCandidates(Path path, String prefix, String chunk, final List<String> candidates, int level) throws IOException {
        Path child = null;
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path,
                (Path p) -> Files.isDirectory(p) && (chunk == null ? true : p.getFileName().toString().startsWith(chunk)))) {
            final Iterator<Path> iter = stream.iterator();
            while(iter.hasNext()) {
                child = iter.next();
                candidates.add(
                        prefix == null ? child.getFileName().toString() :
                            prefix + child.getFileName().toString());
            }
        }
        if(level < 3 && candidates.size() == 1) {
            prefix = candidates.get(0) + ':';
            candidates.clear();
            addCandidates(child, prefix, null, candidates, level + 1);
        }
    }
}
