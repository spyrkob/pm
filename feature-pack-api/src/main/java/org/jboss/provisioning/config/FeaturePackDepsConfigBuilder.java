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

package org.jboss.provisioning.config;

import java.util.Collections;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeaturePackDepsConfigBuilder<B extends FeaturePackDepsConfigBuilder<B>> extends ConfigCustomizationsBuilder<B> {

    Map<ArtifactCoords.Ga, FeaturePackConfig> fpDeps = Collections.emptyMap();
    Map<String, FeaturePackConfig> fpDepsByName = Collections.emptyMap();
    Map<ArtifactCoords.Ga, String> fpGaToName = Collections.emptyMap();

    public B addFeaturePackDep(FeaturePackConfig dependency) throws ProvisioningDescriptionException {
        return addFeaturePackDep(null, dependency);
    }

    @SuppressWarnings("unchecked")
    public B addFeaturePackDep(String name, FeaturePackConfig dependency) throws ProvisioningDescriptionException {
        if(fpDeps.containsKey(dependency.getGav().toGa())) {
            throw new ProvisioningDescriptionException("Feature-pack already added " + dependency.getGav().toGa());
        }
        if(name != null) {
            if(fpDepsByName.containsKey(name)){
                throw new ProvisioningDescriptionException(Errors.duplicateDependencyName(name));
            }
            fpDepsByName = PmCollections.put(fpDepsByName, name, dependency);
            fpGaToName = PmCollections.put(fpGaToName, dependency.getGav().toGa(), name);
        }
        fpDeps = PmCollections.putLinked(fpDeps, dependency.getGav().toGa(), dependency);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B removeFeaturePackDep(ArtifactCoords.Gav gav) throws ProvisioningException {
        final FeaturePackConfig fpDep = fpDeps.get(gav.toGa());
        if(fpDep == null) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        }
        if(!fpDep.getGav().equals(gav)) {
            throw new ProvisioningException(Errors.unknownFeaturePack(gav));
        }
        if(fpDeps.size() == 1) {
            fpDeps = Collections.emptyMap();
            fpDepsByName = Collections.emptyMap();
            fpGaToName = Collections.emptyMap();
            return (B) this;
        }
        fpDeps.remove(gav.toGa());
        if(!fpGaToName.isEmpty()) {
            final String name = fpGaToName.get(gav.toGa());
            if(name != null) {
                if(fpDepsByName.size() == 1) {
                    fpDepsByName = Collections.emptyMap();
                    fpGaToName = Collections.emptyMap();
                } else {
                    fpDepsByName.remove(name);
                    fpGaToName.remove(gav.toGa());
                }
            }
        }
        return (B) this;
    }
}
