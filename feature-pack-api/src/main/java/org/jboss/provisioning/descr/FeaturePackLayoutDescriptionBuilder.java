/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.descr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;

/**
 * This class collects feature packs descriptions and produces an installation
 * description.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutDescriptionBuilder {

    public static FeaturePackLayoutDescriptionBuilder newInstance() {
        return new FeaturePackLayoutDescriptionBuilder();
    }

    private Map<Gav.GaPart, FeaturePackDescription> featurePacks = Collections.emptyMap();

    FeaturePackLayoutDescriptionBuilder() {
    }

    public FeaturePackLayoutDescriptionBuilder addFeaturePack(FeaturePackDescription fp) throws ProvisioningDescriptionException {
        return addFeaturePack(fp, true);
    }

    public FeaturePackLayoutDescriptionBuilder addFeaturePack(FeaturePackDescription fp, boolean addLast) throws ProvisioningDescriptionException {
        assert fp != null : "fp is null";
        final Gav.GaPart fpGa = fp.getGav().getGaPart();
        if(featurePacks.containsKey(fpGa)) {
            throw new ProvisioningDescriptionException(Errors.featurePackVersionConflict(fp.getGav(), featurePacks.get(fpGa).getGav()));
        }
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonMap(fpGa, fp);
                break;
            case 1:
                featurePacks = new LinkedHashMap<Gav.GaPart, FeaturePackDescription>(featurePacks);
            default:
                if(addLast && featurePacks.containsKey(fpGa)) {
                    featurePacks.remove(fpGa);
                }
                featurePacks.put(fpGa, fp);
        }
        return this;
    }

    public FeaturePackLayoutDescription build() throws ProvisioningDescriptionException {
        return new FeaturePackLayoutDescription(Collections.unmodifiableMap(featurePacks));
    }
}
