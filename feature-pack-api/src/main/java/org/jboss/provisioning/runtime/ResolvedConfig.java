/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ConfigId;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.state.ProvisionedConfig;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedConfig implements ProvisionedConfig {

    static ResolvedConfig build(ConfigModelStack configStack) throws ProvisioningException {
        return new ResolvedConfig(configStack);
    }

    final ConfigId id;
    private final Map<String, String> props;
    private Map<String, ConfigId> configDeps;
    private final List<ResolvedFeature> features;

    private ResolvedConfig(ConfigModelStack configStack) throws ProvisioningException {
        this.id = configStack.id;
        this.props = configStack.props.isEmpty() ? configStack.props : Collections.unmodifiableMap(configStack.props);
        this.configDeps = configStack.configDeps.isEmpty() ? configStack.configDeps : Collections.unmodifiableMap(configStack.configDeps);
        this.features = Collections.unmodifiableList(configStack.orderFeatures());
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#getName()
     */
    @Override
    public String getName() {
        return id.getName();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#getModel()
     */
    @Override
    public String getModel() {
        return id.getModel();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#hasProperties()
     */
    @Override
    public boolean hasProperties() {
        return !props.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#getProperty(java.lang.String)
     */
    @Override
    public String getProperty(String name) {
        return props.get(name);
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#getProperties()
     */
    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    public boolean hasConfigDeps() {
        return !configDeps.isEmpty();
    }

    public Map<String, ConfigId> getConfigDeps() {
        return configDeps;
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#hasFeatures()
     */
    @Override
    public boolean hasFeatures() {
        return !features.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.state.ProvisionedConfig#handle(org.jboss.provisioning.plugin.ProvisionedConfigHandler)
     */
    @Override
    public void handle(ProvisionedConfigHandler handler) throws ProvisioningException {
        if(features.isEmpty()) {
            return;
        }
        //System.out.println(model + ':' + name + "> handle");
        handler.prepare(this);
        ResolvedSpecId lastHandledSpecId = null;
        for(ResolvedFeature feature : features) {
            if(feature.isBatchStart()) {
                handler.startBatch();
            }
            if(!feature.spec.id.equals(lastHandledSpecId)) {
                if (lastHandledSpecId == null || !feature.spec.id.gav.equals(lastHandledSpecId.gav)) {
                    handler.nextFeaturePack(feature.spec.id.gav);
                }
                handler.nextSpec(feature.spec);
                lastHandledSpecId = feature.getSpecId();
            }
            handler.nextFeature(feature);
            if(feature.isBatchEnd()) {
                handler.endBatch();
            }
        }
        handler.done();
    }
}
