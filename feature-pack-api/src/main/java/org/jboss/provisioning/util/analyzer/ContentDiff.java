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
package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.provisioning.util.DescrFormatter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ContentDiff {

    public static class Builder {
        private TreeSet<String> uniquePaths;
        private TreeSet<String> conflictPaths;

        private Builder() {
        }

        public Builder addUniquePath(String path) {
            if(uniquePaths == null) {
                uniquePaths = new TreeSet<String>();
            }
            uniquePaths.add(path);
            return this;
        }

        public Builder addConflictPath(String path) {
            if(conflictPaths == null) {
                conflictPaths = new TreeSet<String>();
            }
            conflictPaths.add(path);
            return this;
        }

        public boolean hasRecords() {
            return uniquePaths != null || conflictPaths != null;
        }

        public ContentDiff build() {
            return new ContentDiff(uniquePaths == null ? Collections.emptySet() : Collections.unmodifiableSet(uniquePaths),
                    conflictPaths == null ? Collections.emptySet() : Collections.unmodifiableSet(conflictPaths));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Set<String> uniquePaths;
    private final Set<String> conflictPaths;

    private ContentDiff(Set<String> uniquePaths, Set<String> conflictPaths) {
        this.uniquePaths = uniquePaths;
        this.conflictPaths = conflictPaths;
    }

    public boolean hasUniquePaths() {
        return !uniquePaths.isEmpty();
    }

    public Set<String> getUniquePaths() {
        return uniquePaths;
    }

    public boolean hasConflictPaths() {
        return !conflictPaths.isEmpty();
    }

    public Set<String> getConflictPaths() {
        return conflictPaths;
    }

    void logContent(DescrFormatter out) throws IOException {
        out.println("Content");
        if(!uniquePaths.isEmpty()) {
            out.increaseOffset();
            out.println("Unique");
            out.increaseOffset();
            for(String path : uniquePaths) {
                out.println(path);
            }
            out.decreaseOffset();
            out.decreaseOffset();
        }
        if(!conflictPaths.isEmpty()) {
            out.increaseOffset();
            out.println("Conflicts");
            out.increaseOffset();
            for(String path : conflictPaths) {
                out.println(path);
            }
            out.decreaseOffset();
            out.decreaseOffset();
        }
    }
}
