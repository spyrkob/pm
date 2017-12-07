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

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.spec.CapabilitySpec;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.util.PmCollections;


/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigModelResolver implements ProvisionedConfig {

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

    private final class FeatureGroupScopeStack {
        private List<Map<ResolvedFeatureId, ResolvedFeature>> list;
        private int last;

        FeatureGroupScopeStack() {
            list = Collections.singletonList(new HashMap<>());
        }

        Map<ResolvedFeatureId, ResolvedFeature> peek() {
            return list.get(last);
        }

        Map<ResolvedFeatureId, ResolvedFeature> startGroup() {
            final Map<ResolvedFeatureId, ResolvedFeature> group;
            if (list.size() == last + 1) {
                group = new LinkedHashMap<>();
                list = PmCollections.add(list, group);
                ++last;
            } else {
                group = list.get(++last);
            }
            return group;
        }

        Map<ResolvedFeatureId, ResolvedFeature> endGroup() throws ProvisioningException {
            if (last != 0) {
                final Map<ResolvedFeatureId, ResolvedFeature> endedGroup = list.get(last--);
                final Map<ResolvedFeatureId, ResolvedFeature> parentGroup = list.get(last);
                for (Map.Entry<ResolvedFeatureId, ResolvedFeature> entry : endedGroup.entrySet()) {
                    final ResolvedFeature parentFeature = parentGroup.get(entry.getKey());
                    if (parentFeature == null) {
                        parentGroup.put(entry.getKey(), entry.getValue());
                        if(last == 0) {
                            addToSpecFeatures(entry.getValue());
                        }
                    } else {
                        parentFeature.merge(entry.getValue(), true);
                    }
                }
                endedGroup.clear();
            }
            return list.get(last);
        }

        void addFeature(ResolvedFeature feature) throws ProvisioningDescriptionException {
            if(feature.id == null) {
                addToSpecFeatures(feature);
                return;
            }
            list.get(last).put(feature.id, feature);
            if (last == 0) {
                addToSpecFeatures(feature);
            }
        }

        void merge(ResolvedFeature feature) throws ProvisioningException {
            if(feature.id == null) {
                addToSpecFeatures(feature);
                return;
            }
            final ResolvedFeature localFeature = list.get(last).get(feature.id);
            if(localFeature == null) {
                list.get(last).put(feature.id, feature);
                addToSpecFeatures(feature);
                return;
            }
            localFeature.merge(feature, false);
        }

        void reset() {
            list = Collections.emptyList();
        }
    }

    public static ConfigModelResolver anonymous() {
        return new ConfigModelResolver(null, null);
    }

    public static ConfigModelResolver forName(String name) {
        return new ConfigModelResolver(null, name);
    }

    public static ConfigModelResolver forModel(String model) {
        return new ConfigModelResolver(model, null);
    }

    public static ConfigModelResolver forConfig(String model, String name) {
        return new ConfigModelResolver(model, name);
    }

    final String model;
    final String name;
    private Map<String, String> props = Collections.emptyMap();
    private FeatureGroupScopeStack featureGroupStack = new FeatureGroupScopeStack();
    private Map<ResolvedFeatureId, ResolvedFeature> featuresById = featureGroupStack.peek();
    private Map<ResolvedSpecId, SpecFeatures> featuresBySpec = new LinkedHashMap<>();

    private CapabilityResolver capResolver = new CapabilityResolver();
    private Map<String, CapabilityProviders> capProviders = Collections.emptyMap();

    // features in the order they should be processed by the provisioning handlers
    private List<ResolvedFeature> orderedFeatures;
    private boolean orderReferencedSpec = true;
    private int featureIncludeCount = 0;
    private boolean inBatch;

    private Map<ArtifactCoords.Gav, List<ResolvedFeatureGroupConfig>> fgConfigStacks = new HashMap<>();

    private ConfigModelResolver(String model, String name) {
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

    void startGroup() {
        featuresById = featureGroupStack.startGroup();
    }

    void endGroup() throws ProvisioningException {
        featuresById = featureGroupStack.endGroup();
    }

    boolean pushConfig(ArtifactCoords.Gav gav, ResolvedFeatureGroupConfig fgConfig) {
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
            if(pushedFgConfig.name == null) {
                if(fgConfig.name == null) {
                    if(fgConfig.isSubsetOf(pushedFgConfig)) {
                        return false;
                    } else {
                        break;
                    }
                }
            } else if(pushedFgConfig.name.equals(fgConfig.name)) {
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

    ResolvedFeatureGroupConfig popConfig(ArtifactCoords.Gav gav) {
        final List<ResolvedFeatureGroupConfig> stack = fgConfigStacks.get(gav);
        if(stack == null) {
            throw new IllegalStateException("Feature group stack is null for " + gav);
        }
        if(stack.isEmpty()) {
            throw new IllegalStateException("Feature group stack is empty for " + gav);
        }
        return stack.remove(stack.size() - 1);
    }

    ResolvedFeature includeFeature(ResolvedFeatureId id, ResolvedFeatureSpec spec, Map<String, Object> resolvedParams, Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps) throws ProvisioningException {
        if(id != null) {
            final ResolvedFeature feature = featuresById.get(id);
            if(feature != null) {
                feature.merge(resolvedDeps, resolvedParams, true);
                return feature;
            }
        }
        final ResolvedFeature feature = new ResolvedFeature(id, spec, resolvedParams, resolvedDeps, ++featureIncludeCount);
        featureGroupStack.addFeature(feature);
        return feature;
    }

    boolean includes(ResolvedFeatureId id) {
        return featuresById.containsKey(id);
    }

    private void addToSpecFeatures(final ResolvedFeature feature) {
        SpecFeatures features = featuresBySpec.get(feature.spec.id);
        if(features == null) {
            features = new SpecFeatures(feature.spec);
            featuresBySpec.put(feature.spec.id, features);
        }
        features.list.add(feature);
    }

    void merge(ConfigModelResolver other) throws ProvisioningException {
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
                    featureGroupStack.merge(feature.copy(++featureIncludeCount));
                }
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

    public ProvisionedConfig build(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        if(featuresById.isEmpty()) {
            return this;
        }
        try {
            return doBuild(rt);
        } catch(ProvisioningException e) {
            throw new ProvisioningException(Errors.failedToBuildConfigSpec(model, name), e);
        }
    }

    private ProvisionedConfig doBuild(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        for (SpecFeatures features : featuresBySpec.values()) {
            // resolve references
            features.spec.resolveRefMappings(rt);
            // resolve and register capability providers
            if(features.spec.xmlSpec.providesCapabilities()) {
                for(CapabilitySpec cap : features.spec.xmlSpec.getProvidedCapabilities()) {
                    if(cap.isStatic()) {
                        getProviders(cap.toString(), true).add(features);
                    } else {
                        for(ResolvedFeature feature : features.list) {
                            final List<String> resolvedCaps = capResolver.resolve(cap, feature);
                            if(resolvedCaps.isEmpty()) {
                                continue;
                            }
                            for(String resolvedCap : resolvedCaps) {
                                getProviders(resolvedCap, true).add(feature);
                            }
                        }
                    }
                }
            }
        }
        orderedFeatures = new ArrayList<>(featuresById.size());
        for(SpecFeatures features : featuresBySpec.values()) {
            orderFeaturesInSpec(features, false);
        }

        featuresById = Collections.emptyMap();
        featureGroupStack.reset();
        featuresBySpec = Collections.emptyMap();
        return this;
    }

    private CapabilityProviders getProviders(String cap, boolean add) throws ProvisioningException {
        CapabilityProviders providers = capProviders.get(cap);
        if(providers != null) {
            return providers;
        }
        if(!add) {
            throw new ProvisioningException(Errors.noCapabilityProvider(cap));
        }
        providers = new CapabilityProviders();
        if(capProviders.isEmpty()) {
            capProviders = Collections.singletonMap(cap, providers);
            return providers;
        }
        if(capProviders.size() == 1) {
            final Map.Entry<String, CapabilityProviders> first = capProviders.entrySet().iterator().next();
            capProviders = new HashMap<>(2);
            capProviders.put(first.getKey(), first.getValue());
        }
        capProviders.put(cap, providers);
        return providers;
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
    private List<CircularRefInfo> orderFeaturesInSpec(SpecFeatures features, boolean force) throws ProvisioningException {
        if(!force) {
            if (!features.isFree()) {
                return null;
            }
            features.schedule();
        }

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
        if(!force) {
            features.free();
        }
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
            return Collections.singletonList(new CircularRefInfo(feature));
        }
        feature.schedule();

        List<CircularRefInfo> circularRefs = null;
        if(feature.spec.xmlSpec.requiresCapabilities()) {
            circularRefs = orderCapabilityProviders(feature, circularRefs);
        }
        if(!feature.deps.isEmpty()) {
            circularRefs = orderReferencedFeatures(feature, feature.deps.keySet(), false, circularRefs);
        }
        List<ResolvedFeatureId> refIds = feature.resolveRefs();
        if(!refIds.isEmpty()) {
            circularRefs = orderReferencedFeatures(feature, refIds, true, circularRefs);
        }

        List<CircularRefInfo> initiatedCircularRefs = Collections.emptyList();
        if(circularRefs != null) {
            // there is a one or more circular feature reference loop(s)

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
                        initiatedCircularRefs = PmCollections.add(initiatedCircularRefs, next);
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
                initiatedCircularRefs.sort((o1, o2) -> o1.nextOnPath.includeNo - o2.nextOnPath.includeNo);
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

    private List<CircularRefInfo> orderCapabilityProviders(ResolvedFeature feature, List<CircularRefInfo> circularRefs)
            throws ProvisioningException {
        for (CapabilitySpec capSpec : feature.spec.xmlSpec.getRequiredCapabilities()) {
            final List<String> resolvedCaps = capResolver.resolve(capSpec, feature);
            if (resolvedCaps.isEmpty()) {
                continue;
            }
            for (String resolvedCap : resolvedCaps) {
                final CapabilityProviders providers;
                try {
                    providers = getProviders(resolvedCap, false);
                } catch (ProvisioningException e) {
                    throw new ProvisioningException(Errors.noCapabilityProvider(feature, capSpec, resolvedCap));
                }
                final List<CircularRefInfo> circles = orderProviders(providers);
                if (circularRefs == null) {
                    circularRefs = circles;
                } else {
                    if (circularRefs.size() == 1) {
                        final CircularRefInfo first = circularRefs.get(0);
                        circularRefs = new ArrayList<>(1 + circles.size());
                        circularRefs.add(first);
                    }
                    circularRefs.addAll(circles);
                }
            }
        }
        return circularRefs;
    }

    private List<CircularRefInfo> orderProviders(CapabilityProviders providers) throws ProvisioningException {
        if(!providers.isProvided()) {
            List<CircularRefInfo> firstLoop = null;
            if(!providers.specs.isEmpty()) {
                for(SpecFeatures specFeatures : providers.specs) {
                    final List<CircularRefInfo> loop = orderFeaturesInSpec(specFeatures, !specFeatures.isFree());
                    if(providers.isProvided()) {
                        return null;
                    }
                    if(firstLoop == null) {
                        firstLoop = loop;
                    }
                }
            }
            if (!providers.features.isEmpty()) {
                for (ResolvedFeature provider : providers.features) {
                    final List<CircularRefInfo> loop = orderFeature(provider);
                    if(providers.isProvided()) {
                        return null;
                    }
                    if(firstLoop == null) {
                        firstLoop = loop;
                    }
                }
            }
            return firstLoop;
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
    private List<CircularRefInfo> orderReferencedFeatures(ResolvedFeature feature, Collection<ResolvedFeatureId> refIds, boolean specRefs, List<CircularRefInfo> circularRefs) throws ProvisioningException {
        for(ResolvedFeatureId refId : refIds) {
            final List<CircularRefInfo> loopedOnFeature = orderReferencedFeature(feature, refId, specRefs);
            if(loopedOnFeature != null) {
                if(circularRefs == null) {
                    circularRefs = loopedOnFeature;
                } else {
                    if(circularRefs.size() == 1) {
                        final CircularRefInfo first = circularRefs.get(0);
                        circularRefs = new ArrayList<>(1 + loopedOnFeature.size());
                        circularRefs.add(first);
                    }
                    circularRefs.addAll(loopedOnFeature);
                }
            }
        }
        return circularRefs;
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
    private List<CircularRefInfo> orderReferencedFeature(ResolvedFeature feature, ResolvedFeatureId refId, boolean specRef) throws ProvisioningException {
        if(orderReferencedSpec && specRef && !feature.spec.id.equals(refId.specId)) {
            final SpecFeatures targetSpecFeatures = featuresBySpec.get(refId.specId);
            if (targetSpecFeatures == null) {
                throw new ProvisioningDescriptionException(Errors.unresolvedFeatureDep(feature, refId));
            }
            final List<CircularRefInfo> specLoops = orderFeaturesInSpec(targetSpecFeatures, false);
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
            throw new ProvisioningDescriptionException(Errors.unresolvedFeatureDep(feature, refId));
        }
        return orderFeature(dep);
    }
}
