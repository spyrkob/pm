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
import org.jboss.provisioning.feature.FeatureParameterSpec;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder {

    private class SpecFeatures {
        final FeatureSpec spec;
        private Map<String, ResolvedSpecId> featureRefTargets = Collections.emptyMap();
        List<ConfiguredFeature> list = new ArrayList<>();
        boolean liningUp;

        private SpecFeatures(FeatureSpec spec) {
            this.spec = spec;
        }

        void setRefTarget(String name, ResolvedSpecId targetSpec) throws ProvisioningDescriptionException {
            if(featureRefTargets.containsKey(name)) {
                return;
            }
            switch(featureRefTargets.size()) {
                case 0:
                    featureRefTargets = Collections.singletonMap(name, targetSpec);
                    break;
                case 1:
                    final Map.Entry<String, ResolvedSpecId> entry = featureRefTargets.entrySet().iterator().next();
                    featureRefTargets = new HashMap<>(2);
                    featureRefTargets.put(entry.getKey(), entry.getValue());
                default:
                    if(featureRefTargets.put(name, targetSpec) != null) {
                        throw new ProvisioningDescriptionException("Duplicate feature reference name '" + name + "'");
                    }
            }
        }

        void checkRefs() throws ProvisioningDescriptionException {
            if (featureRefTargets.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ResolvedSpecId> entry : featureRefTargets.entrySet()) {
                final FeatureReferenceSpec refSpec = spec.getRef(entry.getKey());
                final SpecFeatures targetSpec = featuresBySpec.get(entry.getValue());
                if(targetSpec == null) {
                    throw new ProvisioningDescriptionException(spec.getName() + " feature declares reference "
                            + refSpec.getName() + " which targets unknown feature " + entry.getValue());
                }
                if (!targetSpec.spec.hasId()) {
                    throw new ProvisioningDescriptionException(spec.getName() + " feature declares reference "
                            + refSpec.getName() + " which targets feature " + targetSpec.spec.getName()
                            + " that has no ID parameters");
                }
                if (targetSpec.spec.getIdParams().size() != refSpec.getParamsMapped()) {
                    throw new ProvisioningDescriptionException("Parameters of reference " + refSpec.getName() + " of feature "
                            + spec.getName() + " must correspond to the ID parameters of the target feature "
                            + targetSpec.spec.getName());
                }
                for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                    if (!spec.hasParam(refSpec.getLocalParam(i))) {
                        throw new ProvisioningDescriptionException(spec.getName() + " feature does not include parameter "
                                + refSpec.getLocalParam(i) + " mapped in reference " + refSpec.getName());
                    }
                    if (!targetSpec.spec.hasParam(refSpec.getTargetParam(i))) {
                        throw new ProvisioningDescriptionException(targetSpec.spec.getName()
                                + " feature does not include parameter '" + refSpec.getTargetParam(i) + "' targeted from "
                                + spec.getName() + " through reference " + refSpec.getName());
                    }
                }
            }
        }
    }

    private static class ConfiguredFeature {
        final ResolvedFeatureId id;
        final FeatureSpec spec;
        Map<String, String> params;
        Set<ResolvedFeatureId> dependencies = Collections.emptySet();
        boolean liningUp;

        ConfiguredFeature(ResolvedFeatureId id, FeatureSpec spec, Map<String, String> params) {
            this.id = id;
            this.spec = spec;
            this.params = params;
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
        ConfiguredFeature feature = new ConfiguredFeature(id, spec, config.getParams());
        if(id != null) {
            featuresById.put(id, feature);
        }
        SpecFeatures features = featuresBySpec.get(specId);
        if(features == null) {
            features = new SpecFeatures(spec);
            featuresBySpec.put(specId, features);
        }
        features.list.add(feature);

        System.out.println(model + ':' + name + "> processed " + id);

        return true;
    }

    void setFeatureRefTarget(ResolvedSpecId srcSpecId, String refName, ResolvedSpecId targetSpecId) throws ProvisioningDescriptionException {
        featuresBySpec.get(srcSpecId).setRefTarget(refName, targetSpecId);
        checkRefs = true;
    }

    public void lineUp() throws ProvisioningException {
        System.out.println(model + ':' + name + "> lining up");
        if(checkRefs) {
            for(SpecFeatures specFeatures : featuresBySpec.values()) {
                specFeatures.checkRefs();
            }
        }
        for(SpecFeatures features : featuresBySpec.values()) {
            lineUp(features);
        }
    }

    private void lineUp(SpecFeatures features) throws ProvisioningDescriptionException {
        if(features.liningUp) {
            return;
        }
        features.liningUp = true;
        for(ConfiguredFeature feature : features.list) {
            lineUp(features, feature);
        }
    }

    private void lineUp(SpecFeatures specFeatures, ConfiguredFeature feature) throws ProvisioningDescriptionException {
        if(feature.liningUp) {
            return;
        }
        feature.liningUp = true;

        if(!specFeatures.featureRefTargets.isEmpty()) {
            for(Map.Entry<String, ResolvedSpecId> refEntry : specFeatures.featureRefTargets.entrySet()) {
                final FeatureReferenceSpec refSpec = specFeatures.spec.getRef(refEntry.getKey());
                final ResolvedFeatureId refId = getRefId(feature, refSpec, refEntry.getValue());
                if(refId != null) {
                    final SpecFeatures targetSpecFeatures = featuresBySpec.get(refId.specId);
                    if(!targetSpecFeatures.liningUp) {
                        lineUp(targetSpecFeatures);
                    } else {
                        final ConfiguredFeature dep = featuresById.get(refId);
                        if(dep == null) {
                            throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved reference ").append(refId).toString());
                        }
                        lineUp(targetSpecFeatures, dep);
                    }
                }
            }
        }
        if(!feature.dependencies.isEmpty()) {
            for(ResolvedFeatureId depId : feature.dependencies) {
                final ConfiguredFeature dependency = featuresById.get(depId);
                if(dependency == null) {
                    throw new ProvisioningDescriptionException(errorFor(feature).append(" has unsatisfied dependency on ").append(depId).toString());
                }
                lineUp(featuresBySpec.get(depId.specId), dependency);
            }
        }

        final StringBuilder buf = errorFor(feature);
        System.out.println(buf.toString());
    }

    private StringBuilder errorFor(ConfiguredFeature feature) {
        final StringBuilder buf = new StringBuilder();
        if (feature.id != null) {
            buf.append(feature.id);
        } else {
            buf.append(feature.spec.getName()).append(" configuration");
        }
        return buf;
    }

    private static ResolvedFeatureId getRefId(ConfiguredFeature feature, FeatureReferenceSpec refSpec, ResolvedSpecId specId) throws ProvisioningDescriptionException {
        if(refSpec.getParamsMapped() == 1) {
            final FeatureParameterSpec param = feature.spec.getParam(refSpec.getLocalParam(0));
            final String paramValue = getParamValue(feature.id.specId, feature.params, param);
            if(paramValue == null) {
                if (!refSpec.isNillable()) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Reference ").append(refSpec).append(" of ");
                    if (feature.spec.hasId()) {
                        buf.append(getFeatureId(feature.id.specId, feature.spec.getIdParams(), feature.params));
                    } else {
                        buf.append(feature.spec.getName()).append(" configuration ");
                    }
                    buf.append(" cannot be null");
                    throw new ProvisioningDescriptionException(buf.toString());
                }
                return null;
            }
            return new ResolvedFeatureId(specId, Collections.singletonMap(refSpec.getTargetParam(0), paramValue));
        }
        Map<String, String> params = new HashMap<>(refSpec.getParamsMapped());
        for(int i = 0; i < refSpec.getParamsMapped(); ++i) {
            final FeatureParameterSpec param = feature.spec.getParam(refSpec.getLocalParam(i));
            final String paramValue = getParamValue(feature.id.specId, feature.params, param);
            if(paramValue == null) {
                if (!refSpec.isNillable()) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Reference ").append(refSpec).append(" of ");
                    if (feature.spec.hasId()) {
                        buf.append(getFeatureId(feature.id.specId, feature.spec.getIdParams(), feature.params));
                    } else {
                        buf.append(feature.spec.getName()).append(" configuration ");
                    }
                    buf.append(" cannot be null");
                    throw new ProvisioningDescriptionException(buf.toString());
                }
                return null;
            }
            params.put(refSpec.getTargetParam(i), paramValue);
        }
        return new ResolvedFeatureId(specId, params);
    }

    private static String getParamValue(ResolvedSpecId specId, Map<String, String> params, final FeatureParameterSpec param)
            throws ProvisioningDescriptionException {
        String value = params.get(param.getName());
        if(value == null) {
            value = param.getDefaultValue();
        }
        if(value == null && (param.isFeatureId() || !param.isNillable())) {
            throw new ProvisioningDescriptionException(specId + " configuration is missing required parameter " + param.getName());
        }
        return value;
    }

    private static ResolvedFeatureId getFeatureId(ResolvedSpecId specId, List<FeatureParameterSpec> idSpecs, Map<String, String> params) throws ProvisioningDescriptionException {
        if(idSpecs.size() == 1) {
            final FeatureParameterSpec idSpec = idSpecs.get(0);
            return new ResolvedFeatureId(specId, Collections.singletonMap(idSpec.getName(), getParamValue(specId, params, idSpec)));
        }
        final Map<String, String> resolvedParams = new HashMap<>(idSpecs.size());
        for(FeatureParameterSpec param : idSpecs) {
            resolvedParams.put(param.getName(), getParamValue(specId, params, param));
        }
        return new ResolvedFeatureId(specId, resolvedParams);
    }
}
