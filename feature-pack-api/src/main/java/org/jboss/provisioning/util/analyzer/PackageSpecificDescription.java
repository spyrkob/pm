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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.util.DescrFormatter;


/**
 * Describes what is different in a given package comparing to another package.
 *
 * @author Alexey Loubyansky
 */
public class PackageSpecificDescription {

    static class Builder {

        private final String name;
        private List<String> dependencies = Collections.emptyList();
        private Boolean contentExists;
        private ContentDiff contentDiff;

        private Builder(String name) {
            this.name = name;
        }

        Builder addDependency(String dependencyName) {
            assert dependencyName != null : "dependency is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependencyName);
                    break;
                case 1:
                    dependencies = new ArrayList<String>(dependencies);
                default:
                    dependencies.add(dependencyName);
            }
            return this;
        }

        public Builder addAllDependencies(Collection<String> dependencies) {
            if(dependencies.isEmpty()) {
                return this;
            }
            for(String dep : dependencies) {
                addDependency(dep);
            }
            return this;
        }

        public Builder setContentExists(boolean contentDifferent) {
            this.contentExists = contentDifferent;
            return this;
        }

        public Builder setContentDiff(ContentDiff contentDiff) {
            this.contentDiff = contentDiff;
            return this;
        }

        public boolean hasRecords() {
            return contentDiff != null || contentExists != null || !dependencies.isEmpty();
        }

        public PackageSpecificDescription build() {
            return new PackageSpecificDescription(name, Collections.unmodifiableList(dependencies), contentExists, contentDiff);
        }
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    private final String name;
    private final List<String> dependencies;
    private final Boolean contentExists;
    private final ContentDiff contentDiff;

    public PackageSpecificDescription(String name, List<String> dependencies, Boolean contentExists, ContentDiff contentDiff) {
        this.name = name;
        this.dependencies = dependencies;
        this.contentExists = contentExists;
        this.contentDiff = contentDiff;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public Boolean getContentExists() {
        return contentExists;
    }

    public ContentDiff getContentDiff() {
        return contentDiff;
    }

    public String logContent() throws IOException {
        final DescrFormatter out = new DescrFormatter();
        logContent(out);
        return out.toString();
    }

    void logContent(DescrFormatter out) throws IOException {
        out.print("Package ").println(name);
        if(!dependencies.isEmpty()) {
            out.increaseOffset();
            out.println("Unique dependencies");
            out.increaseOffset();
            for(String dependency : dependencies) {
                out.println(dependency);
            }
            out.decreaseOffset();
            out.decreaseOffset();
        }
        if(contentExists != null) {
            out.increaseOffset();
            out.print("Content exists: ").println(contentExists.toString());
            out.decreaseOffset();
        }
        if(contentDiff != null) {
            out.increaseOffset();
            contentDiff.logContent(out);
            out.decreaseOffset();
        }
    }
}
