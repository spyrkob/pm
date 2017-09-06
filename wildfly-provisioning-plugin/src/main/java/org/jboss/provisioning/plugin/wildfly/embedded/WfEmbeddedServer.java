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
public class WfEmbeddedServer extends WfEmbeddedSupport<WfEmbeddedServer> {

    public static WfEmbeddedServer newInstance() {
        return new WfEmbeddedServer();
    }

    protected boolean adminOnly = true;
    protected boolean emptyConfig;
    protected boolean removeExistingConfig;
    protected String serverConfig;

    private WfEmbeddedServer() {
    }

    public WfEmbeddedServer setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
        return this;
    }

    public WfEmbeddedServer setEmptyConfig(boolean emptyConfig) {
        this.emptyConfig = emptyConfig;
        return this;
    }

    public WfEmbeddedServer setRemoveExistingConfig(boolean removeExistingConfig) {
        this.removeExistingConfig = removeExistingConfig;
        return this;
    }

    public WfEmbeddedServer setServerConfig(String serverConfig) {
        this.serverConfig = serverConfig;
        return this;
    }

    @Override
    protected String embedCommand() {
        final StringBuilder buf = new StringBuilder();
        buf.append("embed-server");
        if(adminOnly) {
            buf.append(" --admin-only");
        }
        if(emptyConfig) {
            buf.append(" --empty-config");
        }
        if(removeExistingConfig) {
            buf.append(" --remove-existing");
        }
        if(installHome != null) {
            buf.append(" --jboss-home=").append(installHome.toAbsolutePath());
        }
        if(serverConfig != null) {
            buf.append(" --server-config=").append(serverConfig);
        }
        return buf.toString();
    }

    protected String stopEmbeddedCommand() {
        return "stop-embedded-server";
    }
}
