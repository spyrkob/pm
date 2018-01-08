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

        Level(ConfigModel config) {
            this.config = config;
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

        List<ResolvedFeatureGroupConfig> peek() {
            return groupStack.get(groupStack.size() - 1);
        }

        List<ResolvedFeatureGroupConfig> pop() {
            if(groupStack.isEmpty()) {
                throw new IllegalStateException("Feature group stack is empty");
            }
            return groupStack.remove(groupStack.size() - 1);
        }

        boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
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
                                continue;
                            }
                            return true;
                        }
                    } else {
                        if (id != null && stacked.includedFeatures.containsKey(id)) {
                            continue;
                        }
                        if (!stacked.includedSpecs.contains(specId)) {
                            return true;
                        } else if (id != null && stacked.excludedFeatures.contains(id)) {
                            return true;
                        }
                    }
                }
            }
            return false;
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

    private final ProvisioningRuntimeBuilder rt;
    private List<Level> levels = Collections.emptyList();
    private Level top;

    ConfigModelStack(ProvisioningRuntimeBuilder rt) {
        this.rt = rt;
    }

    void push(ConfigModel model) {
        top = new Level(model);
        levels = PmCollections.add(levels, top);
    }

    List<ResolvedFeatureGroupConfig> popConfig() {
        throw new UnsupportedOperationException();
    }

    boolean push(FeatureGroupSupport fg) throws ProvisioningException {
        throw new UnsupportedOperationException();
    }

    List<ResolvedFeatureGroupConfig> peek() {
        throw new UnsupportedOperationException();
    }

    List<ResolvedFeatureGroupConfig> pop() {
        throw new UnsupportedOperationException();
    }

    boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
        throw new UnsupportedOperationException();
    }

    private boolean isRelevant(ResolvedFeatureGroupConfig resolvedFg) {
        throw new UnsupportedOperationException();
    }
}
