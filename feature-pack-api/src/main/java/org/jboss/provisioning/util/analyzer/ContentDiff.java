/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
