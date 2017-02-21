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

/**
 *
 * @author Alexey Loubyansky
 */
public class GeneratorConfig {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private StandaloneConfig standalone;
        private DomainProfileConfig domainProfile;

        private Builder() {
        }

        public Builder setStandalone(StandaloneConfig standalone) {
            this.standalone = standalone;
            return this;
        }

        public Builder setDomainProfile(DomainProfileConfig domainProfileConfig) {
            this.domainProfile = domainProfileConfig;
            return this;
        }

        public GeneratorConfig build() {
            return new GeneratorConfig(this);
        }
    }

    private final StandaloneConfig standalone;
    private final DomainProfileConfig domainProfile;

    private GeneratorConfig(Builder builder) {
        this.standalone = builder.standalone;
        this.domainProfile = builder.domainProfile;
    }

    public boolean hasStandaloneConfig() {
        return standalone != null;
    }

    public StandaloneConfig getStandaloneConfig() {
        return standalone;
    }

    public boolean hasDomainProfile() {
        return domainProfile != null;
    }

    public DomainProfileConfig getDomainProfileConfig() {
        return domainProfile;
    }
}
