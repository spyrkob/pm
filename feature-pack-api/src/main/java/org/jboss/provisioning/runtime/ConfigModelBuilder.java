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


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder {

    private class SpecFeatures {
        final ResolvedFeatureSpec spec;
        List<ResolvedFeature> list = new ArrayList<>();
        boolean liningUp;

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
    private Map<ResolvedFeatureId, ResolvedFeature> featuresById = new HashMap<>();
    private Map<ResolvedSpecId, SpecFeatures> featuresBySpec = new LinkedHashMap<>();
    private boolean checkRefs;

    private Map<ArtifactCoords.Gav, List<ResolvedFeatureGroupConfig>> fgConfigStacks = new HashMap<>();

    private ConfigModelBuilder(String model, String name) {
        this.model = model;
        this.name = name;
    }

    public void pushConfig(ArtifactCoords.Gav gav, ResolvedFeatureGroupConfig fgConfig) {
        List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(gav);
        if(fgConfigStack == null) {
            fgConfigStack = new ArrayList<>();
            fgConfigStacks.put(gav, fgConfigStack);
        }
        fgConfigStack.add(fgConfig);
    }

    public void popConfig(ArtifactCoords.Gav gav) {
        final List<ResolvedFeatureGroupConfig> stack = fgConfigStacks.get(gav);
        if(stack == null) {
            throw new IllegalStateException("Feature group stack is null for " + gav);
        }
        if(stack.isEmpty()) {
            throw new IllegalStateException("Feature group stack is empty for " + gav);
        }
        stack.remove(stack.size() - 1);
    }

    public boolean processFeature(ResolvedFeatureSpec spec, FeatureConfig config, Set<ResolvedFeatureId> resolvedDeps) throws ProvisioningDescriptionException {
        final ResolvedFeatureId id = spec.xmlSpec.hasId() ? getFeatureId(spec.id, spec.xmlSpec.getIdParams(), config.getParams()) : null;
        if(id != null && featuresById.containsKey(id)) {
            // TODO overwrite params and merge resolved deps
            return false;
        }
        final List<ResolvedFeatureGroupConfig> fgConfigStack = fgConfigStacks.get(spec.id.gav);
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
        final ResolvedFeature feature = new ResolvedFeature(id, spec, config.getParams(), resolvedDeps);
        if(id != null) {
            featuresById.put(id, feature);
        }
        SpecFeatures features = featuresBySpec.get(spec.id);
        if(features == null) {
            features = new SpecFeatures(spec);
            featuresBySpec.put(spec.id, features);
        }
        features.list.add(feature);

        System.out.println(model + ':' + name + "> processed " + id);

        return true;
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
        for(ResolvedFeature feature : features.list) {
            lineUp(features, feature);
        }
    }

    private void lineUp(SpecFeatures specFeatures, ResolvedFeature feature) throws ProvisioningDescriptionException {
        if(feature.liningUp) {
            return;
        }
        feature.liningUp = true;

        List<ResolvedFeatureId> refIds = feature.resolveRefs();
        if(!refIds.isEmpty()) {
            for(ResolvedFeatureId refId : refIds) {
                final SpecFeatures targetSpecFeatures = featuresBySpec.get(refId.specId);
                if(!targetSpecFeatures.liningUp) {
                    lineUp(targetSpecFeatures);
                } else {
                    final ResolvedFeature dep = featuresById.get(refId);
                    if(dep == null) {
                        throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved reference ").append(refId).toString());
                    }
                    lineUp(targetSpecFeatures, dep);
                }
            }
        }
        if(!feature.dependencies.isEmpty()) {
            for(ResolvedFeatureId depId : feature.dependencies) {
                final ResolvedFeature dependency = featuresById.get(depId);
                if(dependency == null) {
                    throw new ProvisioningDescriptionException(errorFor(feature).append(" has unsatisfied dependency on ").append(depId).toString());
                }
                lineUp(featuresBySpec.get(depId.specId), dependency);
            }
        }

        final StringBuilder buf = errorFor(feature);
        System.out.println(buf.toString());
    }

    private StringBuilder errorFor(ResolvedFeature feature) {
        final StringBuilder buf = new StringBuilder();
        if (feature.id != null) {
            buf.append(feature.id);
        } else {
            buf.append(feature.spec.id).append(" configuration");
        }
        return buf;
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
}
