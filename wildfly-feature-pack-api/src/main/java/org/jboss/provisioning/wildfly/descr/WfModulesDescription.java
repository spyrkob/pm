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
package org.jboss.provisioning.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfModulesDescription {

    public static class Builder {

        private String relativeDir;
        private List<String> names = Collections.emptyList();

        Builder() {
        }

        public void setRelativeDir(String relativeDir) {
            this.relativeDir = relativeDir;
        }

        public void addModule(String name) {
            switch(names.size()) {
                case 0:
                    names = Collections.singletonList(name);
                    break;
                case 1:
                    names = new ArrayList<String>(names);
                default:
                    names.add(name);
            }
        }

        public WfModulesDescription build() {
            return new WfModulesDescription(relativeDir, Collections.unmodifiableList(names));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String relativeDir;
    private final List<String> names;

    public WfModulesDescription(String relativeDir, List<String> names) {
        this.relativeDir = relativeDir;
        this.names = names;
    }

    public String getRelativeDir() {
        return relativeDir;
    }

    public List<String> getNames() {
        return names;
    }
}
