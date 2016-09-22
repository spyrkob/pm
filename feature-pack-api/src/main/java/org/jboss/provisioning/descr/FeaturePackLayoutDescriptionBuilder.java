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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private Map<Gav, FeaturePackDescription> featurePacks = Collections.emptyMap();
    private Map<String, Map<String, String>> gavs = Collections.emptyMap();

    FeaturePackLayoutDescriptionBuilder() {
    }

    public FeaturePackLayoutDescriptionBuilder addFeaturePack(FeaturePackDescription fp) throws ProvisioningDescriptionException {
        return addFeaturePack(fp, true);
    }

    public FeaturePackLayoutDescriptionBuilder addFeaturePack(FeaturePackDescription fp, boolean addLast) throws ProvisioningDescriptionException {
        assert fp != null : "fp is null";
        final Gav fpGav = fp.getGAV();
        checkGav(fpGav);
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonMap(fpGav, fp);
                break;
            case 1:
                featurePacks = new LinkedHashMap<Gav, FeaturePackDescription>(featurePacks);
            default:
                if(addLast && featurePacks.containsKey(fpGav)) {
                    featurePacks.remove(fpGav);
                }
                featurePacks.put(fpGav, fp);
        }
        return this;
    }

    private void checkGav(final Gav fpGav) throws ProvisioningDescriptionException {
        Map<String, String> group = gavs.get(fpGav.getGroupId());
        if(group == null) {
            final Map<String, String> result = Collections.singletonMap(fpGav.getArtifactId(), fpGav.getVersion());
            switch(gavs.size()) {
                case 0:
                    gavs = Collections.singletonMap(fpGav.getGroupId(), result);
                    break;
                case 1:
                    gavs = new HashMap<String, Map<String, String>>(gavs);
                default:
                    gavs.put(fpGav.getGroupId(), result);
            }
        } else if (group.containsKey(fpGav.getArtifactId())) {
            if (!group.get(fpGav.getArtifactId()).equals(fpGav.getVersion())) {
                throw new ProvisioningDescriptionException("The installation requires two versions of artifact "
                        + fpGav.getGroupId() + ':' + fpGav.getArtifactId() + ": " + fpGav.getVersion() + " and "
                        + group.get(fpGav.getArtifactId()));
            }
        } else {
            if(group.size() == 1) {
                group = new HashMap<String, String>(group);
                if(gavs.size() == 1) {
                    gavs = Collections.singletonMap(fpGav.getGroupId(), group);
                } else {
                    gavs.put(fpGav.getGroupId(), group);
                }
            }
            group.put(fpGav.getArtifactId(), fpGav.getVersion());
        }
    }

    public FeaturePackLayoutDescription build() throws ProvisioningDescriptionException {
        return new FeaturePackLayoutDescription(Collections.unmodifiableMap(featurePacks));
    }
}
