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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.state.ProvisionedConfig;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelBuilder implements ProvisionedConfig {

    private static final byte FREE = 0;
    private static final byte PROCESSING = 1;

    private class SpecFeatures {
        final ResolvedFeatureSpec spec;
        List<ResolvedFeature> list = new ArrayList<>();
        private byte state = FREE;

        private SpecFeatures(ResolvedFeatureSpec spec) {
            this.spec = spec;
        }

        boolean isFree() {
            return state == FREE;
        }

        void schedule() {
            state = PROCESSING;
        }

        void free() {
            state = FREE;
        }
    }

    private static final class CircularRefInfo {
        final ResolvedFeature loopedOn;
        ResolvedFeature firstInConfig;
        ResolvedFeature nextOnPath;

        CircularRefInfo(ResolvedFeature start) {
            loopedOn = start;
            firstInConfig = start;
            nextOnPath = start;
        }

        void setNext(ResolvedFeature feature) {
            nextOnPath = feature;
            if(firstInConfig.includeNo > feature.includeNo) {
                firstInConfig = feature;
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

    // features in the order they should be processed by the provisioning handlers
    private List<ResolvedFeature> orderedFeatures;
    private boolean orderReferencedSpec = true;
    private int featureIncludeCount = 0;
    private boolean inBatch;

    private Map<ArtifactCoords.Gav, List<ResolvedFeatureGroupConfig>> fgConfigStacks = new HashMap<>();

    private ConfigModelBuilder(String model, String name) {
        this.model = model;
        this.name = name;
    }

    ResolvedFeatureSpec getResolvedSpec(ResolvedSpecId specId, boolean required) throws ProvisioningDescriptionException {
        final SpecFeatures specFeatures = featuresBySpec.get(specId);
        if(specFeatures == null && required) {
            throw new ProvisioningDescriptionException(specId + " is not found in the config model.");
        }
        return specFeatures == null ? null : specFeatures.spec;
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
                        feature.setParam(entry.getKey(), entry.getValue());
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
        final ResolvedFeature feature = new ResolvedFeature(id, spec, config.getParams(), resolvedDeps, ++featureIncludeCount);
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
                    localFeature.setParam(entry.getKey(), entry.getValue());
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
        return orderedFeatures != null && !orderedFeatures.isEmpty();
    }

    @Override
    public void handle(ProvisionedConfigHandler handler) throws ProvisioningException {
        if(orderedFeatures.isEmpty()) {
            return;
        }
        //System.out.println(model + ':' + name + "> handle");
        handler.prepare(this);
        ResolvedSpecId lastHandledSpecId = null;
        for(ResolvedFeature feature : orderedFeatures) {
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

    public ProvisionedConfig build() throws ProvisioningException {
        for (SpecFeatures features : featuresBySpec.values()) {
            try {
                features.spec.resolveRefMappings(this);
            } catch(ProvisioningDescriptionException e) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Failed to build config");
                if(name != null) {
                    buf.append(" name=").append(name);
                }
                if(model != null) {
                    buf.append(" model=").append(model);
                }
                throw new ProvisioningException(buf.toString(), e);
            }
        }
        if(featuresById.isEmpty()) {
            orderedFeatures = Collections.emptyList();
            featuresById = Collections.emptyMap();
            featuresBySpec = Collections.emptyMap();
            return this;
        }
        orderedFeatures = new ArrayList<>(featuresById.size());
        for(SpecFeatures features : featuresBySpec.values()) {
            orderSpec(features);
        }

        featuresById = Collections.emptyMap();
        featuresBySpec = Collections.emptyMap();
        return this;
    }

    /**
     * Attempts to order the features of the spec.
     * Terminates immediately when a feature reference loop is detected.
     *
     * @param features  spec features
     * @return  returns the feature id on which the feature reference loop was detected,
     *   returns null if no loop was detected (despite whether any feature was processed or not)
     * @throws ProvisioningException
     */
    private List<CircularRefInfo> orderSpec(SpecFeatures features) throws ProvisioningException {
        if(!features.isFree()) {
            return null;
        }
        features.schedule();

        List<CircularRefInfo> allCircularRefs = null;
        int i = 0;
        while(i < features.list.size() && allCircularRefs == null) {
            allCircularRefs = orderFeature(features.list.get(i++));
/*            if(circularRefs != null) {
                if(allCircularRefs == null) {
                    allCircularRefs = circularRefs;
                } else {
                    if(allCircularRefs.size() == 1) {
                        final CircularRefInfo first = allCircularRefs.get(0);
                        allCircularRefs = new ArrayList<>(1 + circularRefs.size());
                        allCircularRefs.add(first);
                    }
                    allCircularRefs.addAll(circularRefs);
                }
            }
*/        }
        features.free();
        return allCircularRefs;
    }

    /**
     * Attempts to order the feature. If the feature has already been scheduled
     * for ordering but haven't been ordered yet, it means there is a circular feature
     * reference loop, in which case the feature is not ordered and false is returned.
     *
     * @param feature  the feature to put in the ordered list
     * @return  whether the feature was added to the ordered list or not
     * @throws ProvisioningException
     */
    private List<CircularRefInfo> orderFeature(ResolvedFeature feature) throws ProvisioningException {

        if(feature.isOrdered()) {
            return null;
        }
        if(!feature.isFree()) {
            if(feature.id == null) {
                throw new IllegalStateException();
            }
            return Collections.singletonList(new CircularRefInfo(feature));
        }
        feature.schedule();

        List<CircularRefInfo> circularRefs = null;
        if(!feature.dependencies.isEmpty()) {
            circularRefs = orderRefs(feature, feature.dependencies, false);
        }
        List<ResolvedFeatureId> refIds = feature.resolveRefs(this);
        if(!refIds.isEmpty()) {
            final List<CircularRefInfo> cyclicSpecRefs = orderRefs(feature, refIds, true);
            if(circularRefs == null) {
                circularRefs = cyclicSpecRefs;
            } else {
                if(circularRefs.size() == 1) {
                    final CircularRefInfo first = circularRefs.get(0);
                    circularRefs = new ArrayList<>(1 + cyclicSpecRefs.size());
                    circularRefs.add(first);
                }
                circularRefs.addAll(cyclicSpecRefs);
            }
        }

        List<CircularRefInfo> initiatedCircularRefs = Collections.emptyList();
        if(circularRefs != null) {
            // there is a circular feature reference loop

            // check whether there is a loop that this feature didn't initiate
            // if there is such a loop then propagate the loops this feature didn't start to their origins
            if(circularRefs.size() == 1) {
                final CircularRefInfo next = circularRefs.get(0);
                if (next.loopedOn.id.equals(feature.id)) { // this feature initiated the loop
                    circularRefs = Collections.emptyList();
                    initiatedCircularRefs = Collections.singletonList(next);
                } else {
                    next.setNext(feature);
                    feature.free();
                }
            } else {
                final Iterator<CircularRefInfo> i = circularRefs.iterator();
                while (i.hasNext()) {
                    final CircularRefInfo next = i.next();
                    if (next.loopedOn.id.equals(feature.id)) {
                        // this feature initiated the loop
                        i.remove();
                        switch(initiatedCircularRefs.size()) {
                            case 0:
                                initiatedCircularRefs = Collections.singletonList(next);
                                break;
                            case 1:
                                final CircularRefInfo first = initiatedCircularRefs.get(0);
                                initiatedCircularRefs = new ArrayList<>(2);
                                initiatedCircularRefs.add(first);
                            default:
                                initiatedCircularRefs.add(next);
                        }
                    } else {
                        // the feature is in the middle of the loop
                        next.setNext(feature);
                        feature.free();
                    }
                }
            }
            if(!circularRefs.isEmpty()) {
                return circularRefs;
            }
            // all the loops were initiated by this feature
        }

        if (!initiatedCircularRefs.isEmpty()) {
            final boolean prevOrderRefSpec = orderReferencedSpec;
            orderReferencedSpec = false;
            // sort according to the appearance in the config
            initiatedCircularRefs.sort((o1, o2) -> o1.firstInConfig.includeNo - o2.firstInConfig.includeNo);
            if(initiatedCircularRefs.get(0).firstInConfig.includeNo < feature.includeNo) {
                feature.free();
                for(CircularRefInfo ref : initiatedCircularRefs) {
                    if(orderFeature(ref.firstInConfig) != null) {
                        throw new IllegalStateException();
                    }
                }
            } else {
                final boolean endBatch;
                if(inBatch) {
                    endBatch = false;
                } else {
                    inBatch = true;
                    feature.startBatch();
                    endBatch = true;
                }
                feature.ordered();
                orderedFeatures.add(feature);
                for(CircularRefInfo ref : initiatedCircularRefs) {
                    if(orderFeature(ref.nextOnPath) != null) {
                        throw new IllegalStateException();
                    }
                }
                if(endBatch) {
                    inBatch = false;
                    this.orderedFeatures.get(orderedFeatures.size() - 1).endBatch();
                }
            }
            orderReferencedSpec = prevOrderRefSpec;
        } else {
            feature.ordered();
            orderedFeatures.add(feature);
        }
        return null;
    }

    /**
     * Attempts to order the referenced features.
     *
     * @param feature  parent feature
     * @param refIds  referenced features ids
     * @param specRefs  whether these referenced features represent actual spec references or feature dependencies
     * @return  feature ids that form circular dependency loops
     * @throws ProvisioningException
     */
    private List<CircularRefInfo> orderRefs(ResolvedFeature feature, Collection<ResolvedFeatureId> refIds, boolean specRefs) throws ProvisioningException {
        List<CircularRefInfo> cyclicRefs = null;
        for(ResolvedFeatureId refId : refIds) {
            final List<CircularRefInfo> loopedOnFeature = orderRef(feature, refId, specRefs);
            if(loopedOnFeature != null) {
                if(cyclicRefs == null) {
                    cyclicRefs = loopedOnFeature;
                } else {
                    if(cyclicRefs.size() == 1) {
                        final CircularRefInfo first = cyclicRefs.get(0);
                        cyclicRefs = new ArrayList<>(1 + loopedOnFeature.size());
                        cyclicRefs.add(first);
                    }
                    cyclicRefs.addAll(loopedOnFeature);
                }
            }
        }
        return cyclicRefs;
    }

    /**
     * Attempts to order a feature reference.
     *
     * @param feature  parent feature
     * @param refId  referenced feature id
     * @param specRef  whether the referenced feature represents a spec reference or a feature dependency
     * @return  true if the referenced feature was ordered, false if the feature was not ordered because of the circular reference loop
     * @throws ProvisioningException
     */
    private List<CircularRefInfo> orderRef(ResolvedFeature feature, ResolvedFeatureId refId, boolean specRef) throws ProvisioningException {
        if(orderReferencedSpec && specRef && !feature.spec.id.equals(refId.specId)) {
            final SpecFeatures targetSpecFeatures = featuresBySpec.get(refId.specId);
            if (targetSpecFeatures == null) {
                throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved dependency on ").append(refId).toString());
            }
            final List<CircularRefInfo> specLoops = orderSpec(targetSpecFeatures);
            if (specLoops != null) {
                List<CircularRefInfo> featureLoops = null;
                for (int i = 0; i < specLoops.size(); ++i) {
                    final CircularRefInfo specLoop = specLoops.get(i);
                    if (specLoop.nextOnPath.id.equals(refId)) {
                        if (featureLoops == null) {
                            featureLoops = Collections.singletonList(specLoop);
                        } else {
                            if (featureLoops.size() == 1) {
                                final CircularRefInfo first = featureLoops.get(0);
                                featureLoops = new ArrayList<>(2);
                                featureLoops.add(first);
                            }
                            featureLoops.add(specLoop);
                        }
                    }
                }
                if (featureLoops != null) {
                    return featureLoops;
                }
            }
        }
        final ResolvedFeature dep = featuresById.get(refId);
        if (dep == null) {
            throw new ProvisioningDescriptionException(errorFor(feature).append(" has unresolved dependency on ").append(refId).toString());
        }
        return orderFeature(dep);
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
