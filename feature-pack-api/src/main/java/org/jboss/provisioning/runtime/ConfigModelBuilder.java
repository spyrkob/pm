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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureSpec;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder {

    private static class SpecFeatures {
        List<ConfiguredFeature> list = new ArrayList<>();
        boolean liningUp;
    }

    private static class ConfiguredFeature {
        final ResolvedFeatureId id;
        final FeatureSpec spec;
        Map<String, String> params = Collections.emptyMap();
        Set<ResolvedFeatureId> dependencies = Collections.emptySet();
        boolean liningUp;

        ConfiguredFeature(ResolvedFeatureId id, FeatureSpec spec) {
            this.id = id;
            this.spec = spec;
        }
    }

    public static ConfigModelBuilder anonymous() {
        return new ConfigModelBuilder(null, null);
    }

    public static ConfigModelBuilder forName(String name) {
        return new ConfigModelBuilder(null, name);
    }

    public static ConfigModelBuilder forModel(String model) {
        return new ConfigModelBuilder(model, null);
    }

    public static ConfigModelBuilder forConfig(String model, String name) {
        return new ConfigModelBuilder(model, name);
    }

    final String model;
    final String name;
    private Map<ResolvedFeatureId, ConfiguredFeature> featuresById = new HashMap<>();
    private Map<ResolvedSpecId, SpecFeatures> featuresBySpec = new LinkedHashMap<>();

    private Map<ArtifactCoords.Ga, List<ResolvedFeatureGroupConfig>> fgConfigStacks = new HashMap<>();

    private ConfigModelBuilder(String model, String name) {
        this.model = model;
        this.name = name;
    }

    public void pushConfig(ArtifactCoords.Ga ga, ResolvedFeatureGroupConfig fgConfig) {
        List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(ga);
        if(fgConfigStack == null) {
            fgConfigStack = new ArrayList<>();
            fgConfigStacks.put(ga, fgConfigStack);
        }
        fgConfigStack.add(fgConfig);
    }

    public void popConfig(ArtifactCoords.Ga ga) {
        final List<ResolvedFeatureGroupConfig> stack = fgConfigStacks.get(ga);
        if(stack == null) {
            throw new IllegalStateException("Feature group stack is null for " + ga);
        }
        if(stack.isEmpty()) {
            throw new IllegalStateException("Feature group stack is empty for " + ga);
        }
        stack.remove(stack.size() - 1);
    }

    public boolean processFeature(ResolvedFeatureId id, ResolvedSpecId specId,FeatureSpec spec, FeatureConfig config) {
        if(id != null && featuresById.containsKey(id)) {
            return false;
        }
        final List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(specId.ga);
        if (fgConfigStack != null) {
            int i = fgConfigStack.size() - 1;
            while (i >= 0) {
                final ResolvedFeatureGroupConfig fgConfig = fgConfigStack.get(i--);
                if (fgConfig.inheritFeatures) {
                    if(id != null && fgConfig.excludedFeatures.contains(id)) {
                        return false;
                    }
                    if (fgConfig.excludedSpecs.contains(id.specId)) {
                        if(id != null && fgConfig.includedFeatures.containsKey(id)) {
                            continue;
                        }
                        return false;
                    }
                } else {
                    if(id != null && fgConfig.includedFeatures.containsKey(id)) {
                        continue;
                    }
                    if(!fgConfig.includedSpecs.contains(id.specId)) {
                        return false;
                    } else if(id != null && fgConfig.excludedFeatures.contains(id)) {
                        return false;
                    }
                }
            }
        }
        ConfiguredFeature feature = new ConfiguredFeature(id, spec);
        if(id != null) {
            featuresById.put(id, feature);
        }
        SpecFeatures features = featuresBySpec.get(specId);
        if(features == null) {
            features = new SpecFeatures();
            featuresBySpec.put(specId, features);
        }
        features.list.add(feature);

        System.out.println(model + ':' + name + "> added " + id);

        return true;
    }

    public void lineUp() throws ProvisioningException {
        System.out.println(model + ':' + name + "> line up");
    }
}
