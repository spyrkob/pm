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
public class HostControllerConfig {

    private static final String DEFAULT_DOMAIN_CONFIG = "domain.xml";
    private static final String DEFAULT_HOST_CONFIG = "host.xml";
    private static final List<String> DEFAULT_SCRIPTS = Arrays.asList("provisioning.cli", "domain.cli", "host.cli");

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements ScriptsBuilder {

        private String domainConfig = DEFAULT_DOMAIN_CONFIG;
        private String hostConfig = DEFAULT_HOST_CONFIG;
        private List<String> scripts = Collections.emptyList();

        private Builder() {
        }

        public Builder setDomainConfig(String domainConfig) {
            this.domainConfig = domainConfig;
            return this;
        }

        public Builder setHostConfig(String hostConfig) {
            this.hostConfig = hostConfig;
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

        public HostControllerConfig build() {
            return new HostControllerConfig(this);
        }
    }

    private final String domainConfig;
    private final String hostConfig;
    private final List<String> scripts;

    private HostControllerConfig(Builder builder) {
        this.domainConfig = builder.domainConfig;
        this.hostConfig = builder.hostConfig;
        this.scripts = Collections.unmodifiableList(builder.scripts.isEmpty() ? DEFAULT_SCRIPTS : builder.scripts);
    }

    public String getDomainConfig() {
        return domainConfig;
    }

    public String getHostConfig() {
        return hostConfig;
    }

    public List<String> getScripts() {
        return scripts;
    }
}
