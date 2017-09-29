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

package org.jboss.provisioning.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;


/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeatureGroupBuilderSupport<B extends FeatureGroupBuilderSupport<B>> {

    protected Map<String, FeatureGroupSpec.Builder> externalGroups = Collections.emptyMap();
    protected List<FeatureGroupConfig> localGroups = Collections.emptyList();
    protected List<FeatureConfig> features = Collections.emptyList();

    protected FeatureGroupBuilderSupport(FeatureGroupBuilderSupport<B> src) {
        externalGroups = src.externalGroups;
        localGroups = src.localGroups;
        switch(src.features.size()) {
            case 0:
                break;
            case 1:
                features = Collections.singletonList(new FeatureConfig(src.features.get(0)));
                break;
            default:
                features = new ArrayList<>(src.features.size());
                for(FeatureConfig fc : src.features) {
                    features.add(new FeatureConfig(fc));
                }
        }
    }

    protected FeatureGroupBuilderSupport() {}

    @SuppressWarnings("unchecked")
    public B addFeatureGroup(String fpDep, FeatureGroupConfig group) {
        if(fpDep == null) {
            return addFeatureGroup(group);
        }
        getExternalFgBuilder(fpDep).addFeatureGroup(group);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addFeatureGroup(FeatureGroupConfig dep) {
        switch (localGroups.size()) {
            case 0:
                localGroups = Collections.singletonList(dep);
                break;
            case 1:
                localGroups = new ArrayList<>(localGroups);
            default:
                localGroups.add(dep);
        }
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addFeature(String fpDep, FeatureConfig fc) {
        if(fpDep == null) {
            return addFeature(fc);
        }
        getExternalFgBuilder(fpDep).addFeature(fc);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addFeature(FeatureConfig feature) {
        switch(features.size()) {
            case 0:
                features = Collections.singletonList(feature);
                break;
            case 1:
                features = new ArrayList<>(features);
            default:
                features.add(feature);
        }
        return (B) this;
    }

    protected Map<String, FeatureGroupSpec> buildExternalDependencies() {
        if (externalGroups.isEmpty()) {
            return Collections.emptyMap();
        }
        if (externalGroups.size() == 1) {
            final Map.Entry<String, FeatureGroupSpec.Builder> entry = externalGroups.entrySet().iterator().next();
            return Collections.singletonMap(entry.getKey(), entry.getValue().build());
        }
        final Iterator<Map.Entry<String, FeatureGroupSpec.Builder>> i = externalGroups.entrySet().iterator();
        final Map<String, FeatureGroupSpec> tmp = new HashMap<>(externalGroups.size());
        while (i.hasNext()) {
            final Map.Entry<String, FeatureGroupSpec.Builder> entry = i.next();
            tmp.put(entry.getKey(), entry.getValue().build());
        }
        return Collections.unmodifiableMap(tmp);
    }

    private FeatureGroupSpec.Builder getExternalFgBuilder(String fpDep) {
        FeatureGroupSpec.Builder specBuilder;
        if(externalGroups.isEmpty()) {
            specBuilder = FeatureGroupSpec.builder();
            externalGroups = Collections.singletonMap(fpDep, specBuilder);
        } else {
            specBuilder = externalGroups.get(fpDep);
            if(specBuilder == null) {
                specBuilder = FeatureGroupSpec.builder();
                if(externalGroups.size() == 1) {
                    final Map.Entry<String, FeatureGroupSpec.Builder> first = externalGroups.entrySet().iterator().next();
                    externalGroups = new LinkedHashMap<>(2);
                    externalGroups.put(first.getKey(), first.getValue());
                }
                externalGroups.put(fpDep, specBuilder);
            }
        }
        return specBuilder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Map<String, FeatureGroupSpec> externalGroups = buildExternalDependencies();
        result = prime * result + ((externalGroups == null) ? 0 : externalGroups.hashCode());
        result = prime * result + ((features == null) ? 0 : features.hashCode());
        result = prime * result + ((localGroups == null) ? 0 : localGroups.hashCode());
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureGroupBuilderSupport other = (FeatureGroupBuilderSupport) obj;
        final Map<String, FeatureGroupSpec> externalGroups = buildExternalDependencies();
        final Map<String, FeatureGroupSpec> otherExternalGroups = other.buildExternalDependencies();
        if (externalGroups == null) {
            if (otherExternalGroups != null)
                return false;
        } else if (!externalGroups.equals(otherExternalGroups))
            return false;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        if (localGroups == null) {
            if (other.localGroups != null)
                return false;
        } else if (!localGroups.equals(other.localGroups))
            return false;
        return true;
    }
}
