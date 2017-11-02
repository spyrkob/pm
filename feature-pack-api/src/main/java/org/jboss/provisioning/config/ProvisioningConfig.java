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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.xml.ProvisioningXmlWriter;

/**
 * The configuration of the installation to be provisioned.
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningConfig {

    public static class Builder {

        private Map<ArtifactCoords.Ga, FeaturePackConfig> featurePacks = Collections.emptyMap();

        private Builder() {
        }

        private Builder(ProvisioningConfig provisioningConfig) throws ProvisioningDescriptionException {
            for(FeaturePackConfig fp : provisioningConfig.getFeaturePacks()) {
                addFeaturePack(fp);
            }
        }

        public Builder addFeaturePack(ArtifactCoords.Gav fpGav) throws ProvisioningDescriptionException {
            return addFeaturePack(FeaturePackConfig.forGav(fpGav));
        }

        public Builder addFeaturePack(FeaturePackConfig fp) throws ProvisioningDescriptionException {
            final ArtifactCoords.Ga gaPart = fp.getGav().toGa();
            if(featurePacks.containsKey(gaPart)) {
                throw new ProvisioningDescriptionException(Errors.featurePackVersionConflict(fp.getGav(), featurePacks.get(gaPart).getGav()));
            }
            featurePacks = PmCollections.putLinked(featurePacks, gaPart, fp);
            return this;
        }

        public Builder removeFeaturePack(ArtifactCoords.Gav gav) throws ProvisioningException {
            final FeaturePackConfig fpConfig = featurePacks.get(gav.toGa());
            if(fpConfig == null) {
                throw new ProvisioningException(Errors.unknownFeaturePack(gav));
            }
            if(!fpConfig.getGav().equals(gav)) {
                throw new ProvisioningException(Errors.unknownFeaturePack(gav));
            }
            if(featurePacks.size() == 1) {
                featurePacks = Collections.emptyMap();
            } else {
                featurePacks.remove(gav.toGa());
            }
            return this;
        }

        public ProvisioningConfig build() {
            return new ProvisioningConfig(PmCollections.unmodifiable(featurePacks));
        }

        public void exportToXml(Path location) throws IOException {
            final ProvisioningConfig config = build();
            try {
                ProvisioningXmlWriter.getInstance().write(config, location);
            } catch (XMLStreamException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Allows to build a provisioning configuration starting from the passed in
     * initial configuration.
     *
     * @param provisioningConfig  initial state of the configuration to be built
     * @return  this builder instance
     * @throws ProvisioningDescriptionException
     */
    public static Builder builder(ProvisioningConfig provisioningConfig) throws ProvisioningDescriptionException {
        return new Builder(provisioningConfig);
    }

    private Map<ArtifactCoords.Ga, FeaturePackConfig> featurePacks;

    private ProvisioningConfig(Map<ArtifactCoords.Ga, FeaturePackConfig> featurePacks) {
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public boolean containsFeaturePack(ArtifactCoords.Ga gaPart) {
        return featurePacks.containsKey(gaPart);
    }

    public Set<ArtifactCoords.Ga> getFeaturePackGaParts() {
        return featurePacks.keySet();
    }

    public Collection<FeaturePackConfig> getFeaturePacks() {
        return featurePacks.values();
    }

    public FeaturePackConfig getFeaturePack(ArtifactCoords.Ga gaPart) {
        return featurePacks.get(gaPart);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featurePacks == null) ? 0 : featurePacks.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProvisioningConfig other = (ProvisioningConfig) obj;
        if (featurePacks == null) {
            if (other.featurePacks != null)
                return false;
        } else if (!featurePacks.equals(other.featurePacks))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return featurePacks.values().toString();
    }
}
