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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.config.FeatureGroupSupport;

/**
 * @author Alexey Loubyansky
 *
 */
class ConfigModelStack {

    private class ConfigScope {

        final ConfigModel config;
        List<List<ResolvedFeatureGroupConfig>> groupStack = new ArrayList<>();
        int lastOnStack = -1;

        ConfigScope(ConfigModel config) throws ProvisioningException {
            this.config = config;
            if(config != null) {
                push(config);
            }
        }

        boolean push(FeatureGroupSupport fg) throws ProvisioningException {
            final List<ResolvedFeatureGroupConfig> relevantFgs = lastOnStack == groupStack.size() - 1 ?
                    new ArrayList<>(fg.getExternalFeatureGroups().size() + 1) : groupStack.get(lastOnStack + 1);
            if(fg.hasExternalFeatureGroups()) {
                for(Map.Entry<String, FeatureGroup> entry : fg.getExternalFeatureGroups().entrySet()) {
                    final ResolvedFeatureGroupConfig resolvedFg = rt.resolveFg(entry.getKey(), entry.getValue());
                    if(ConfigModelStack.this.isRelevant(resolvedFg)) {
                        relevantFgs.add(resolvedFg);
                    }
                }
            }
            final ResolvedFeatureGroupConfig resolvedFg = rt.resolveFg(null, fg);
            if(resolvedFg != null && ConfigModelStack.this.isRelevant(resolvedFg)) {
                relevantFgs.add(resolvedFg);
            }
            if(relevantFgs.isEmpty()) {
                return false;
            }

            ++lastOnStack;
            if(lastOnStack == groupStack.size()) {
                groupStack.add(relevantFgs);
            }
            return true;
        }

        boolean pop() throws ProvisioningException {
            if(lastOnStack < 0) {
                throw new IllegalStateException("Feature group stack is empty");
            }
            final List<ResolvedFeatureGroupConfig> last = groupStack.get(lastOnStack--);
            final boolean processed = rt.processIncludedFeatures(last);
            last.clear();
            return processed;
        }

        void processIncludedFeatures() throws ProvisioningException {
            for (int i = lastOnStack; i >= 0; --i) {
                rt.processIncludedFeatures(groupStack.get(i));
            }
        }

        boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
            boolean included = false;
            for(int i = lastOnStack; i >= 0; --i) {
                final List<ResolvedFeatureGroupConfig> groups = groupStack.get(i);
                for(int j = groups.size() - 1; j >= 0; --j) {
                    final ResolvedFeatureGroupConfig stacked = groups.get(j);
                    if(!stacked.gav.equals(specId.gav)) {
                        continue;
                    }
                    if (stacked.inheritFeatures) {
                        if (id != null && stacked.excludedFeatures.contains(id)) {
                            return true;
                        }
                        if (stacked.excludedSpecs.contains(specId)) {
                            if (id != null && stacked.includedFeatures.containsKey(id)) {
                                included = true;
                                continue;
                            }
                            return true;
                        }
                    } else {
                        if (id != null && stacked.includedFeatures.containsKey(id)) {
                            included = true;
                            continue;
                        }
                        if (!stacked.includedSpecs.contains(specId)) {
                            return true;
                        }
                        if (id != null && stacked.excludedFeatures.contains(id)) {
                            return true;
                        }
                        included = true;
                    }
                }
            }
            if(included) {
                return false;
            }
            return config == null ? false : !config.isInheritFeatures();
        }

        private boolean isRelevant(ResolvedFeatureGroupConfig resolvedFg) {
            if(resolvedFg.fg.getId() == null) {
                return true;
            }
            for(int i = lastOnStack; i >= 0; --i) {
                final List<ResolvedFeatureGroupConfig> groups = groupStack.get(i);
                for(int j = groups.size() - 1; j >= 0; --j) {
                    final ResolvedFeatureGroupConfig stacked = groups.get(j);
                    if(stacked.fg.getId() == null
                            || !stacked.gav.equals(resolvedFg.gav)
                            || !stacked.fg.getId().equals(resolvedFg.fg.getId())) {
                        continue;
                    }
                    return !resolvedFg.isSubsetOf(stacked);
                }
            }
            return true;
        }

    }

    final ProvisioningRuntimeBuilder rt;
    private List<ConfigScope> configs = new ArrayList<>();
    private ConfigScope lastConfig;

    ConfigModelStack(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        this.rt = rt;
        lastConfig = new ConfigScope(null);
        configs.add(lastConfig);
    }

    void pushConfig(ConfigModel model) throws ProvisioningException {
        lastConfig = new ConfigScope(model);
        configs.add(lastConfig);
    }

    ConfigModel popConfig() throws ProvisioningException {
        final ConfigScope result = lastConfig;
        configs.remove(configs.size() - 1);
        lastConfig = configs.get(configs.size() - 1);
        result.processIncludedFeatures();
        return result.config;
    }

    boolean pushGroup(FeatureGroupSupport fg) throws ProvisioningException {
        return lastConfig.push(fg);
    }

    boolean popGroup() throws ProvisioningException {
        return lastConfig.pop();
    }

    boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
        if(lastConfig.isFilteredOut(specId, id)) {
            return true;
        }
        if(configs.size() > 1) {
            for (int i = configs.size() - 2; i >= 0; --i) {
                if (configs.get(i).isFilteredOut(specId, id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRelevant(ResolvedFeatureGroupConfig resolvedFg) {
        if(resolvedFg.fg.getId() == null || lastConfig == null) {
            return true;
        }
        if(!lastConfig.isRelevant(resolvedFg)) {
            return false;
        }
        if(configs.size() > 1) {
            for (int i = configs.size() - 2; i >= 0; --i) {
                if (!configs.get(i).isRelevant(resolvedFg)) {
                    return false;
                }
            }
        }
        return true;
    }
}
