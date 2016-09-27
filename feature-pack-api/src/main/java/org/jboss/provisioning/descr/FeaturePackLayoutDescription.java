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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes a layout of feature-packs from which
 * the target installation is provisioned.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutDescription {

    public static class Builder {

        private Map<Gav.GaPart, FeaturePackDescription> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        public Builder addFeaturePack(FeaturePackDescription fp) throws ProvisioningDescriptionException {
            return addFeaturePack(fp, true);
        }

        public Builder addFeaturePack(FeaturePackDescription fp, boolean addLast) throws ProvisioningDescriptionException {
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

    public static Builder builder() {
        return new Builder();
    }

    private final Map<Gav.GaPart, FeaturePackDescription> featurePacks;

    FeaturePackLayoutDescription(Map<Gav.GaPart, FeaturePackDescription> featurePacks) {
        assert featurePacks != null : "featurePacks is null";
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public boolean contains(Gav.GaPart fpGa) {
        return featurePacks.containsKey(fpGa);
    }

    public Gav getGav(Gav.GaPart fpGa) {
        final FeaturePackDescription fpDescr = featurePacks.get(fpGa);
        return fpDescr == null ? null : fpDescr.getGav();
    }

    public Collection<FeaturePackDescription> getFeaturePacks() {
        return featurePacks.values();
    }

    public String logContent() throws IOException {
        final DescrFormatter logger = new DescrFormatter();
        logger.println("Feature-pack layout");
        logger.increaseOffset();
        for(FeaturePackDescription fp : featurePacks.values()) {
            fp.logContent(logger);
        }
        logger.decreaseOffset();
        return logger.toString();
    }
}
