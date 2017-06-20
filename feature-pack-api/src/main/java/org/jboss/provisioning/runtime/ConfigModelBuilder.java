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
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder {

    private static class SpecFeatures {
        final FeatureSpec spec;
        List<ConfiguredFeature> list = new ArrayList<>();
        boolean liningUp;

        private SpecFeatures(FeatureSpec spec) {
            this.spec = spec;
        }
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
    private boolean checkRefs;

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

    public boolean processFeature(ResolvedFeatureId id, ResolvedSpecId specId, FeatureSpec spec, FeatureConfig config) {
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
            features = new SpecFeatures(spec);
            featuresBySpec.put(specId, features);
        }
        features.list.add(feature);
        checkRefs = spec.hasRefs();

        System.out.println(model + ':' + name + "> added " + id);

        return true;
    }

    public void lineUp() throws ProvisioningException {
        System.out.println(model + ':' + name + "> line up");

        if(checkRefs) {
            for(SpecFeatures specFeatures : featuresBySpec.values()) {
                final FeatureSpec spec = specFeatures.spec;
                if(!spec.hasRefs()) {
                    continue;
                }
                for(FeatureReferenceSpec refSpec : spec.getRefs()) {
/*                    final FeatureSpec targetSpec = featuresBySpec.get(refSpec.getFeature());
                    if(targetSpec == null) {
                        throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                + refSpec.name + " which targets unknown " + refSpec.feature + " feature");
                    }
                    if(!targetSpec.hasId()) {
                        throw new ProvisioningDescriptionException(spec.name + " feature declares reference "
                                + refSpec.name + " which targets feature " + refSpec.feature + " that has no ID parameters");
                    }
                    if(targetSpec.idParams.size() != refSpec.localParams.length) {
                        throw new ProvisioningDescriptionException("Parameters of reference " + refSpec.name + " of feature " + spec.name +
                                " must correspond to the ID parameters of the target feature " + refSpec.feature);
                    }
                    for(int i = 0; i < refSpec.localParams.length; ++i) {
                        if(!spec.params.containsKey(refSpec.localParams[i])) {
                            throw new ProvisioningDescriptionException(spec.name
                                    + " feature does not include parameter " + refSpec.localParams[i] + " mapped in "
                                    + refSpec.name + " reference");
                        }
                        if(!targetSpec.params.containsKey(refSpec.targetParams[i])) {
                            throw new ProvisioningDescriptionException(targetSpec.name
                                    + " feature does not include parameter '" + refSpec.targetParams[i] + "' targeted from "
                                    + spec.name + " through " + refSpec.name + " reference");
                        }
                    }
*/                }
            }
        }

    }
}
