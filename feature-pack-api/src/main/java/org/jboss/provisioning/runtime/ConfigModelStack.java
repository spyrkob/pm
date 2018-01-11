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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.config.FeatureGroupSupport;
import org.jboss.provisioning.util.PmCollections;

/**
 * @author Alexey Loubyansky
 *
 */
class ConfigModelStack {

    private class Level {

        final ConfigModel config;
        List<List<ResolvedFeatureGroupConfig>> groupStack = new ArrayList<>();

        Level(ConfigModel config) throws ProvisioningException {
            this.config = config;
            if(config != null) {
                push(config);
            }
        }

        boolean push(FeatureGroupSupport fg) throws ProvisioningException {
            List<ResolvedFeatureGroupConfig> relevantFgs = Collections.emptyList();
            if(fg.hasExternalFeatureGroups()) {
                for(Map.Entry<String, FeatureGroup> entry : fg.getExternalFeatureGroups().entrySet()) {
                    final ResolvedFeatureGroupConfig resolvedFg = rt.resolveFg(entry.getKey(), entry.getValue());
                    if(ConfigModelStack.this.isRelevant(resolvedFg)) {
                        relevantFgs = PmCollections.add(relevantFgs, resolvedFg);
                    }
                }
            }
            final ResolvedFeatureGroupConfig resolvedFg = rt.resolveFg(null, fg);
            if(resolvedFg != null && ConfigModelStack.this.isRelevant(resolvedFg)) {
                relevantFgs = PmCollections.add(relevantFgs, resolvedFg);
            }
            if(relevantFgs.isEmpty()) {
                return false;
            }
            groupStack.add(relevantFgs);
            return true;
        }

        List<ResolvedFeatureGroupConfig> pop() {
            if(groupStack.isEmpty()) {
                throw new IllegalStateException("Feature group stack is empty");
            }
            return groupStack.remove(groupStack.size() - 1);
        }

        boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
            boolean included = false;
            for(int i = groupStack.size() - 1; i >= 0; --i) {
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
            if(groupStack.isEmpty()) {
                return true;
            }
            if(resolvedFg.fg.getId() == null) {
                return true;
            }
            for(int i = groupStack.size() - 1; i >= 0; --i) {
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
    private List<Level> levels = new ArrayList<>();
    private Level top;

    ConfigModelStack(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        this.rt = rt;
        top = new Level(null);
        levels.add(top);
    }

    ConfigModel getCurrentConfig() {
        return top.config;
    }

    void pushConfig(ConfigModel model) throws ProvisioningException {
        top = new Level(model);
        levels.add(top);
    }

    ConfigModel popConfig() throws ProvisioningException {
        final Level result = top;
        levels.remove(levels.size() - 1);
        top = levels.get(levels.size() - 1);
        for(int i = result.groupStack.size() - 1; i >= 0; --i) {
            rt.processIncludedFeatures(result.groupStack.get(i));
        }
        return result.config;
    }

    boolean pushGroup(FeatureGroupSupport fg) throws ProvisioningException {
        return top.push(fg);
    }

    boolean popGroup() throws ProvisioningException {
        return rt.processIncludedFeatures(top.pop());
    }

    boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
        if(top.isFilteredOut(specId, id)) {
            return true;
        }
        if(levels.size() > 1) {
            for (int i = levels.size() - 2; i >= 0; --i) {
                if (levels.get(i).isFilteredOut(specId, id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRelevant(ResolvedFeatureGroupConfig resolvedFg) {
        if(resolvedFg.fg.getId() == null || top == null) {
            return true;
        }
        if(!top.isRelevant(resolvedFg)) {
            return false;
        }
        if(levels.size() > 1) {
            for (int i = levels.size() - 2; i >= 0; --i) {
                if (!levels.get(i).isRelevant(resolvedFg)) {
                    return false;
                }
            }
        }
        return true;
    }
}
