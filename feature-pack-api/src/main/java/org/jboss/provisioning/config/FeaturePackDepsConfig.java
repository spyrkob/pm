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

import java.util.Collection;
import java.util.Map;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.PmCollections;

/**
 * @author Alexey Loubyansky
 *
 */
public class FeaturePackDepsConfig extends ConfigCustomizations {

    protected final Map<ArtifactCoords.Ga, FeaturePackConfig> fpDeps;
    protected final Map<String, FeaturePackConfig> fpDepsByName;
    private final Map<ArtifactCoords.Ga, String> fpGaToName;

    protected FeaturePackDepsConfig(FeaturePackDepsConfigBuilder<?> builder) {
        super(builder);
        this.fpDeps = PmCollections.unmodifiable(builder.fpDeps);
        this.fpDepsByName = PmCollections.unmodifiable(builder.fpDepsByName);
        this.fpGaToName = builder.fpGaToName;
    }

    public boolean hasFeaturePackDeps() {
        return !fpDeps.isEmpty();
    }

    public boolean hasFeaturePackDep(ArtifactCoords.Ga gaPart) {
        return fpDeps.containsKey(gaPart);
    }

    public FeaturePackConfig getFeaturePackDep(ArtifactCoords.Ga gaPart) {
        return fpDeps.get(gaPart);
    }

    public Collection<FeaturePackConfig> getFeaturePackDeps() {
        return fpDeps.values();
    }

    public FeaturePackConfig getFeaturePackDep(String name) throws ProvisioningDescriptionException {
        final FeaturePackConfig fpDep = fpDepsByName.get(name);
        if(fpDep == null) {
            throw new ProvisioningDescriptionException(Errors.unknownFeaturePackDependencyName(name));
        }
        return fpDep;
    }

    public String getFeaturePackDepName(ArtifactCoords.Ga fpGa) {
        return fpGaToName.get(fpGa);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((fpDeps == null) ? 0 : fpDeps.hashCode());
        result = prime * result + ((fpDepsByName == null) ? 0 : fpDepsByName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeaturePackDepsConfig other = (FeaturePackDepsConfig) obj;
        if (fpDeps == null) {
            if (other.fpDeps != null)
                return false;
        } else if (!fpDeps.equals(other.fpDeps))
            return false;
        if (fpDepsByName == null) {
            if (other.fpDepsByName != null)
                return false;
        } else if (!fpDepsByName.equals(other.fpDepsByName))
            return false;
        return true;
    }
}
