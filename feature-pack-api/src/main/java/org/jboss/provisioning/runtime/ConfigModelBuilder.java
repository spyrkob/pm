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
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.state.ProvisionedConfig;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder implements ProvisionedConfig {

    private class SpecFeatures {
        final ResolvedFeatureSpec spec;
        List<ResolvedFeature> list = new ArrayList<>();
        boolean beingHandled;

        private SpecFeatures(ResolvedFeatureSpec spec) {
            this.spec = spec;
        }

        void checkRefs() throws ProvisioningDescriptionException {
            if (spec.resolvedRefTargets.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ResolvedSpecId> entry : spec.resolvedRefTargets.entrySet()) {
                final FeatureReferenceSpec refSpec = spec.xmlSpec.getRef(entry.getKey());
                final SpecFeatures targetSpec = featuresBySpec.get(entry.getValue());
                if(targetSpec == null) {
                    throw new ProvisioningDescriptionException(spec.xmlSpec.getName() + " feature declares reference "
                            + refSpec.getName() + " which targets unknown feature " + entry.getValue());
                }
                if (!targetSpec.spec.xmlSpec.hasId()) {
                    throw new ProvisioningDescriptionException(spec.xmlSpec.getName() + " feature declares reference "
                            + refSpec.getName() + " which targets feature " + targetSpec.spec.xmlSpec.getName()
                            + " that has no ID parameters");
                }
                if (targetSpec.spec.xmlSpec.getIdParams().size() != refSpec.getParamsMapped()) {
                    throw new ProvisioningDescriptionException("Parameters of reference " + refSpec.getName() + " of feature "
                            + spec.xmlSpec.getName() + " must correspond to the ID parameters of the target feature "
                            + targetSpec.spec.xmlSpec.getName());
                }
                for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                    if (!spec.xmlSpec.hasParam(refSpec.getLocalParam(i))) {
                        throw new ProvisioningDescriptionException(spec.xmlSpec.getName() + " feature does not include parameter "
                                + refSpec.getLocalParam(i) + " mapped in reference " + refSpec.getName());
                    }
                    if (!targetSpec.spec.xmlSpec.hasParam(refSpec.getTargetParam(i))) {
                        throw new ProvisioningDescriptionException(targetSpec.spec.xmlSpec.getName()
                                + " feature does not include parameter '" + refSpec.getTargetParam(i) + "' targeted from "
                                + spec.xmlSpec.getName() + " through reference " + refSpec.getName());
                    }
                }
            }
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
    private Map<String, String> props = Collections.emptyMap();
    private Map<ResolvedFeatureId, ResolvedFeature> featuresById = new HashMap<>();
    private Map<ResolvedSpecId, SpecFeatures> featuresBySpec = new LinkedHashMap<>();
    private boolean checkRefs;
    private ResolvedSpecId lastHandledSpecId;

    private Map<ArtifactCoords.Gav, List<ResolvedFeatureGroupConfig>> fgConfigStacks = new HashMap<>();

    private ConfigModelBuilder(String model, String name) {
        this.model = model;
        this.name = name;
    }

    public void overwriteProps(Map<String, String> props) {
        if(props.isEmpty()) {
            return;
        }
        if(this.props.isEmpty()) {
            this.props = new HashMap<>();
        }
        this.props.putAll(props);
    }

    public boolean pushConfig(ArtifactCoords.Gav gav, ResolvedFeatureGroupConfig fgConfig) {
        List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(gav);
        if(fgConfigStack == null) {
            fgConfigStack = new ArrayList<>();
            fgConfigStacks.put(gav, fgConfigStack);
            fgConfigStack.add(fgConfig);
            return true;
        }
        int i = fgConfigStack.size() - 1;
        while(i >= 0) {
            final ResolvedFeatureGroupConfig pushedFgConfig = fgConfigStack.get(i--);
            if(pushedFgConfig.name.equals(fgConfig.name)) {
                if(fgConfig.isSubsetOf(pushedFgConfig)) {
                    return false;
                } else {
                    break;
                }
            }
        }
        fgConfigStack.add(fgConfig);
        return true;
    }

    public ResolvedFeatureGroupConfig popConfig(ArtifactCoords.Gav gav) {
        final List<ResolvedFeatureGroupConfig> stack = fgConfigStacks.get(gav);
        if(stack == null) {
            throw new IllegalStateException("Feature group stack is null for " + gav);
        }
        if(stack.isEmpty()) {
            throw new IllegalStateException("Feature group stack is empty for " + gav);
        }
        return stack.remove(stack.size() - 1);
    }

    ResolvedFeature includeFeature(ResolvedFeatureId id, ResolvedFeatureSpec spec, FeatureConfig config, Set<ResolvedFeatureId> resolvedDeps) throws ProvisioningDescriptionException {
        if(id != null) {
            final ResolvedFeature feature = featuresById.get(id);
            if(feature != null) {
                if(config.hasParams()) {
                    for(Map.Entry<String, String> entry : config.getParams().entrySet()) {
                        feature.params.put(entry.getKey(), entry.getValue());
                    }
                }
                if(!resolvedDeps.isEmpty()) {
                    for(ResolvedFeatureId depId : feature.dependencies) {
                        feature.addDependency(depId);
                    }
                }
                return feature;
            }
        }
        final ResolvedFeature feature = new ResolvedFeature(id, spec, config.getParams(), resolvedDeps);
        if(id != null) {
            featuresById.put(id, feature);
        }
        addToSpecFeatures(feature);
        return feature;
    }

    private void addToSpecFeatures(final ResolvedFeature feature) {
        SpecFeatures features = featuresBySpec.get(feature.spec.id);
        if(features == null) {
            features = new SpecFeatures(feature.spec);
            featuresBySpec.put(feature.spec.id, features);
        }
        features.list.add(feature);
    }

    void merge(ConfigModelBuilder other) throws ProvisioningException {
        if(!other.props.isEmpty()) {
            if(props.isEmpty()) {
                props = other.props;
            } else {
                for(Map.Entry<String, String> prop : other.props.entrySet()) {
                    if(!props.containsKey(prop.getKey())) {
                        props.put(prop.getKey(), prop.getValue());
                    }
                }
            }
        }
        if(!other.featuresBySpec.isEmpty()) {
            for(Map.Entry<ResolvedSpecId, SpecFeatures> entry : other.featuresBySpec.entrySet()) {
                for(ResolvedFeature feature : entry.getValue().list) {
                    merge(feature);
                }
            }
        }
    }

    private void merge(ResolvedFeature feature) throws ProvisioningException {
        if(feature.id == null) {
            addToSpecFeatures(feature);
            return;
        }
        final ResolvedFeature localFeature = featuresById.get(feature.id);
        if(localFeature == null) {
            featuresById.put(feature.id, feature);
            addToSpecFeatures(feature);
            return;
        }
        if(feature.hasParams()) {
            for(Map.Entry<String, String> entry : feature.params.entrySet()) {
                if(!localFeature.params.containsKey(entry.getKey())) {
                    localFeature.params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if(!feature.dependencies.isEmpty()) {
            for(ResolvedFeatureId depId : feature.dependencies) {
                localFeature.addDependency(depId);
            }
        }
    }

    boolean isFilteredOut(ResolvedSpecId specId, final ResolvedFeatureId id) {
        final List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(specId.gav);
        if (fgConfigStack == null) {
            return false;
        }
        int i = fgConfigStack.size() - 1;
        while (i >= 0) {
            final ResolvedFeatureGroupConfig fgConfig = fgConfigStack.get(i--);
            if (fgConfig.inheritFeatures) {
                if (id != null && fgConfig.excludedFeatures.contains(id)) {
                    return true;
                }
                if (fgConfig.excludedSpecs.contains(specId)) {
                    if (id != null && fgConfig.includedFeatures.containsKey(id)) {
                        continue;
                    }
                    return true;
                }
            } else {
                if (id != null && fgConfig.includedFeatures.containsKey(id)) {
                    continue;
                }
                if (!fgConfig.includedSpecs.contains(specId)) {
                    return true;
                } else if (id != null && fgConfig.excludedFeatures.contains(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasProperties() {
        return !props.isEmpty();
    }

    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public boolean hasFeatures() {
        return !featuresById.isEmpty();
    }

    @Override
    public void handle(ProvisionedConfigHandler handler) throws ProvisioningException {
        if(featuresById.isEmpty()) {
            return;
        }
        //System.out.println(model + ':' + name + "> handle");
        if(checkRefs) {
            for(SpecFeatures specFeatures : featuresBySpec.values()) {
                specFeatures.checkRefs();
            }
        }
        for(SpecFeatures features : featuresBySpec.values()) {
            handleSpec(features, handler);
        }
        lastHandledSpecId = null;
        handler.done();
        for(SpecFeatures features : featuresBySpec.values()) {
            features.beingHandled = false;
            for(ResolvedFeature feature : features.list) {
                feature.beingHandled = false;
            }
        }
    }

    public ProvisionedConfig build() throws ProvisioningException {
        return this;
    }

    private void handleSpec(SpecFeatures features, ProvisionedConfigHandler handler) throws ProvisioningException {
        if(features.beingHandled) {
            return;
        }
        features.beingHandled = true;
        handleFeature(features.list.get(0), handler);
        int i = 1;
        while(i < features.list.size()) {
            handleFeature(features.list.get(i++), handler);
        }
    }

    private void handleFeature(ResolvedFeature feature, ProvisionedConfigHandler handler) throws ProvisioningException {
        if(feature.beingHandled) {
            return;
        }
        feature.beingHandled = true;

        if(!feature.dependencies.isEmpty()) {
            for(ResolvedFeatureId depId : feature.dependencies) {
                handleRef(feature, depId, handler);
            }
        }
        List<ResolvedFeatureId> refIds = feature.resolveRefs();
        if(!refIds.isEmpty()) {
            for(ResolvedFeatureId refId : refIds) {
                handleRef(feature, refId, handler);
            }
        }

        if(!feature.spec.id.equals(lastHandledSpecId)) {
            if (lastHandledSpecId == null || !feature.spec.id.gav.equals(lastHandledSpecId.gav)) {
                handler.nextFeaturePack(feature.spec.id.gav);
            }
            handler.nextSpec(feature.spec);
            lastHandledSpecId = feature.getSpecId();
        }
        handler.nextFeature(feature);
    }

    private void handleRef(ResolvedFeature feature, ResolvedFeatureId refId, ProvisionedConfigHandler handler) throws ProvisioningException {
        if (feature.spec.id.equals(refId.specId)) {
            final ResolvedFeature dep = featuresById.get(refId);
            if (dep == null) {
                throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved dependency on ").append(refId).toString());
            }
            handleFeature(dep, handler);
            return;
        }
        final SpecFeatures targetSpecFeatures = featuresBySpec.get(refId.specId);
        if(targetSpecFeatures == null) {
            throw new ProvisioningDescriptionException("Failed to locate instances of feature specifition " + refId.specId);
        }
        if (!targetSpecFeatures.beingHandled) {
            handleSpec(targetSpecFeatures, handler);
            return;
        }
        final ResolvedFeature dep = featuresById.get(refId);
        if (dep == null) {
            throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved dependency on ").append(refId).toString());
        }
        if (!dep.beingHandled) {
            handleFeature(dep, handler);
        }
    }

    private static StringBuilder errorFor(ResolvedFeature feature) {
        final StringBuilder buf = new StringBuilder();
        if (feature.id != null) {
            buf.append(feature.id);
        } else {
            buf.append(feature.spec.id).append(" configuration");
        }
        return buf;
    }
}
