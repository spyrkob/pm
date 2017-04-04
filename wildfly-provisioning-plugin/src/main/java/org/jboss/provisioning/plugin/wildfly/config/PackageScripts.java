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

package org.jboss.provisioning.plugin.wildfly.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageScripts {

    public static class Script {

        public static class Builder {

            private String path;
            private String prefix;
            private boolean collectAgain;
            private Map<String, String> params = Collections.emptyMap();
            private String line;

            private Builder() {
                this(null);
            }

            private Builder(String path) {
                this(path, null);
            }

            private Builder(String path, String prefix) {
                this(path, prefix, false);
            }

            private Builder(String path, String prefix, boolean collectAgain) {
                this.path = path;
                this.prefix = prefix;
                this.collectAgain = collectAgain;
            }

            public Builder setPath(String path) {
                this.path = path;
                return this;
            }

            public Builder setPrefix(String prefix) {
                this.prefix = prefix;
                return this;
            }

            public Builder setCollectAgainPath(boolean collectAgain) {
                this.collectAgain = collectAgain;
                return this;
            }

            public Builder addParameter(String name, String value) {
                switch(params.size()) {
                    case 0:
                        params = Collections.singletonMap(name, value);
                        break;
                    case 1:
                        params = new HashMap<>(params);
                    default:
                        params.put(name, value);
                }
                return this;
            }

            public Builder setLine(String line) {
                this.line = line;
                return this;
            }

            public Script build() {
                return new Script(this);
            }
        }

        public static Builder builder() {
            return builder(null);
        }

        public static Builder builder(String path) {
            return builder(path, false);
        }

        public static Builder builder(String path, boolean collectAgain) {
            return builder(path, null, collectAgain);
        }

        public static Builder builder(String path, String prefix) {
            return builder(path, prefix, false);
        }

        public static Builder builder(String path, String prefix, boolean collectAgain) {
            return new Builder(path, prefix, collectAgain);
        }

        private final String path;
        private final String prefix;
        private final boolean collectAgain;
        private final Map<String, String> params;
        private final String line;

        private Script(Builder builder) {
            this.path = builder.path;
            this.prefix = builder.prefix;
            this.collectAgain = builder.collectAgain;
            this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
            this.line = builder.line;
        }

        public String getPath() {
            return path;
        }

        public boolean hasPrefix() {
            return prefix != null;
        }

        public String getPrefix() {
            return prefix;
        }

        public boolean isCollectAgain() {
            return collectAgain;
        }

        public boolean hasParameters() {
            return !params.isEmpty();
        }

        public Map<String, String> getParameters() {
            return params;
        }

        public String getLine() {
            return line;
        }
    }

    public static class ScriptsBuilder {
        private List<Script> list = Collections.emptyList();
        public ScriptsBuilder addStandalone(Script script) {
            switch(list.size()) {
                case 0:
                    list = Collections.singletonList(script);
                    break;
                case 1:
                    list = new ArrayList<>(list);
                default:
                    list.add(script);
            }
            return this;
        }
        public List<Script> build() {
            return list.size() > 1 ? Collections.unmodifiableList(list) : list;
        }
    }

    public static class Builder {

        private List<Script> standalone = Collections.emptyList();
        private List<Script> domain = Collections.emptyList();
        private List<Script> host = Collections.emptyList();

        private Builder() {
        }

        public Builder addStandalone(Script script) {
            switch(standalone.size()) {
                case 0:
                    standalone = Collections.singletonList(script);
                    break;
                case 1:
                    standalone = new ArrayList<>(standalone);
                default:
                    standalone.add(script);
            }
            return this;
        }

        public void setStandalone(List<Script> scripts) {
            standalone = scripts;
        }

        public Builder addDomain(Script script) {
            switch(domain.size()) {
                case 0:
                    domain = Collections.singletonList(script);
                    break;
                case 1:
                    domain = new ArrayList<>(domain);
                default:
                    domain.add(script);
            }
            return this;
        }

        public void setDomain(List<Script> scripts) {
            domain = scripts;
        }

        public Builder addHost(Script script) {
            switch(host.size()) {
                case 0:
                    host = Collections.singletonList(script);
                    break;
                case 1:
                    host = new ArrayList<>(host);
                default:
                    host.add(script);
            }
            return this;
        }

        public void setHost(List<Script> scripts) {
            host = scripts;
        }

        public PackageScripts build() {
            return new PackageScripts(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final PackageScripts DEFAULT = builder()
            .addStandalone(Script.builder("main.cli").build())
            .addStandalone(Script.builder("standalone.cli").build())
            .addStandalone(Script.builder("variable.cli", false).build())
            .addStandalone(Script.builder("profile.cli").build())
            .addDomain(Script.builder("main.cli").build())
            .addDomain(Script.builder("domain.cli").build())
            .addDomain(Script.builder("variable.cli", true).build())
            .addDomain(Script.builder("profile.cli", "/profile=$profile", true).build())
            .addHost(Script.builder("host.cli", "/host=${host:master}", false).build())
            .build();

    private final List<Script> standalone;
    private final List<Script> domain;
    private final List<Script> host;

    private PackageScripts(Builder builder) {
        this.standalone = builder.standalone.isEmpty() ? builder.standalone : Collections.unmodifiableList(builder.standalone);
        this.domain = builder.domain.isEmpty() ? builder.domain : Collections.unmodifiableList(builder.domain);
        this.host = builder.host.isEmpty() ? builder.host : Collections.unmodifiableList(builder.host);
    }

    public boolean hasStandalone() {
        return !standalone.isEmpty();
    }

    public boolean hasHost() {
        return !host.isEmpty();
    }

    public boolean hasDomain() {
        return !domain.isEmpty();
    }

    public List<Script> getStandalone() {
        return standalone;
    }

    public List<Script> getDomain() {
        return domain;
    }

    public List<Script> getHost() {
        return host;
    }
}
