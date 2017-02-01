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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.plugin.wildfly.config.GeneratorConfigParser20.ScriptsBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class StandaloneConfig {

    private static final String DEFAULT_CONFIG = "standalone.xml";
    private static final List<String> DEFAULT_SCRIPTS = Arrays.asList("main.cli", "standalone.cli");

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements ScriptsBuilder {

        private String serverConfig = DEFAULT_CONFIG;
        private List<String> scripts = Collections.emptyList();

        private Builder() {
        }

        public Builder setServerConfig(String serverConfig) {
            this.serverConfig = serverConfig;
            return this;
        }

        @Override
        public void addScript(String script) {
            switch(scripts.size()) {
                case 0:
                    scripts = Collections.singletonList(script);
                    break;
                case 1:
                    scripts = new ArrayList<>(scripts);
                default:
                    scripts.add(script);
            }
        }

        public StandaloneConfig build() {
            return new StandaloneConfig(this);
        }
    }

    private final String serverConfig;
    private final List<String> scripts;

    private StandaloneConfig(Builder builder) {
        this.serverConfig = builder.serverConfig;
        this.scripts = Collections.unmodifiableList(builder.scripts.isEmpty() ? DEFAULT_SCRIPTS : builder.scripts);
    }

    public String getServerConfig() {
        return serverConfig;
    }

    public List<String> getScripts() {
        return scripts;
    }
}