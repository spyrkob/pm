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
package org.jboss.provisioning.plugin.wildfly.featurepack.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author Eduardo Martins
 * @author Alexey Loubyansky
 */
public class ConfigDescription {

    public static class Builder {
        private List<ConfigFileDescription> standaloneConfigFiles = Collections.emptyList();
        private List<ConfigFileDescription> domainConfigFiles = Collections.emptyList();
        private List<ConfigFileDescription> hostConfigFiles = Collections.emptyList();

        private Builder() {
        }

        public Builder addStandalone(ConfigFileDescription configFile) {
            switch(standaloneConfigFiles.size()) {
                case 0:
                    standaloneConfigFiles = Collections.singletonList(configFile);
                    break;
                case 1:
                    standaloneConfigFiles = new ArrayList<>(standaloneConfigFiles);
                default:
                    standaloneConfigFiles.add(configFile);
            }
            return this;
        }

        public Builder addDomain(ConfigFileDescription configFile) {
            switch(domainConfigFiles.size()) {
                case 0:
                    domainConfigFiles = Collections.singletonList(configFile);
                    break;
                case 1:
                    domainConfigFiles = new ArrayList<>(domainConfigFiles);
                default:
                    domainConfigFiles.add(configFile);
            }
            return this;
        }

        public Builder addHost(ConfigFileDescription configFile) {
            switch(hostConfigFiles.size()) {
                case 0:
                    hostConfigFiles = Collections.singletonList(configFile);
                    break;
                case 1:
                    hostConfigFiles = new ArrayList<>(hostConfigFiles);
                default:
                    hostConfigFiles.add(configFile);
            }
            return this;
        }

        public ConfigDescription build() {
            return new ConfigDescription(Collections.unmodifiableList(standaloneConfigFiles),
                    Collections.unmodifiableList(domainConfigFiles),
                    Collections.unmodifiableList(hostConfigFiles));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<ConfigFileDescription> standaloneConfigFiles;
    private final List<ConfigFileDescription> domainConfigFiles;
    private final List<ConfigFileDescription> hostConfigFiles;

    private ConfigDescription(List<ConfigFileDescription> standalone, List<ConfigFileDescription> domain, List<ConfigFileDescription> host) {
        this.standaloneConfigFiles = standalone;
        this.domainConfigFiles = domain;
        this.hostConfigFiles = host;
    }

    public List<ConfigFileDescription> getStandaloneConfigFiles() {
        return standaloneConfigFiles;
    }

    public List<ConfigFileDescription> getDomainConfigFiles() {
        return domainConfigFiles;
    }

    public List<ConfigFileDescription> getHostConfigFiles() {
        return hostConfigFiles;
    }
}
