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

package org.jboss.provisioning.plugin.wildfly.embedded;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfEmbeddedHc extends WfEmbeddedSupport<WfEmbeddedHc> {

    public static WfEmbeddedHc newInstance() {
        return new WfEmbeddedHc();
    }

    protected boolean emptyHostConfig;
    protected boolean removeExistingHostConfig;
    protected String hostConfig;
    protected boolean emptyDomainConfig;
    protected boolean removeExistingDomainConfig;
    protected String domainConfig;

    private WfEmbeddedHc() {
    }

    public WfEmbeddedHc setEmptyHostConfig(boolean emptyHostConfig) {
        this.emptyHostConfig = emptyHostConfig;
        return this;
    }

    public WfEmbeddedHc setRemoveExistingHostConfig(boolean removeExistingHostConfig) {
        this.removeExistingHostConfig = removeExistingHostConfig;
        return this;
    }

    public WfEmbeddedHc setHostConfig(String hostConfig) {
        this.domainConfig = hostConfig;
        return this;
    }

    public WfEmbeddedHc setEmptyDomainConfig(boolean emptyDomainConfig) {
        this.emptyDomainConfig = emptyDomainConfig;
        return this;
    }

    public WfEmbeddedHc setRemoveExistingDomainConfig(boolean removeExistingDomainConfig) {
        this.removeExistingDomainConfig = removeExistingDomainConfig;
        return this;
    }

    public WfEmbeddedHc setDomainConfig(String domainConfig) {
        this.domainConfig = domainConfig;
        return this;
    }

    @Override
    protected String embedCommand() {
        final StringBuilder buf = new StringBuilder();
        buf.append("embed-host-controller");
        if(emptyHostConfig) {
            buf.append(" --empty-host-config");
        }
        if(removeExistingHostConfig) {
            buf.append(" --remove-existing-host-config");
        }
        if(hostConfig != null) {
            buf.append(" --host-config=").append(hostConfig);
        }
        if(emptyDomainConfig) {
            buf.append(" --empty-domain-config");
        }
        if(removeExistingDomainConfig) {
            buf.append(" --remove-existing-domain-config");
        }
        if(domainConfig != null) {
            buf.append(" --domain-config=").append(hostConfig);
        }
        if(installHome != null) {
            buf.append(" --jboss-home=").append(installHome.toAbsolutePath());
        }
        return buf.toString();
    }

    protected String stopEmbeddedCommand() {
        return "stop-embedded-host-controller";
    }
}
