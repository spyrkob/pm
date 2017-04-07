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

package org.jboss.provisioning.config.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigType {

    public static class Occurence {

        public static Occurence create(ConfigTypeId typeId, boolean required, boolean maxOccursUnbounded) {
            return new Occurence(typeId, required, maxOccursUnbounded);
        }

        final ConfigTypeId typeId;
        final boolean required;
        final boolean maxOccursUnbounded;

        private Occurence(ConfigTypeId typeId, boolean required, boolean maxOccursUnbounded) {
            this.typeId = typeId;
            this.required = required;
            this.maxOccursUnbounded = maxOccursUnbounded;
        }

        public ConfigTypeId getTypeId() {
            return typeId;
        }

        public boolean isRequired() {
            return required;
        }

        public boolean isMaxOccursUnbounded() {
            return maxOccursUnbounded;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (maxOccursUnbounded ? 1231 : 1237);
            result = prime * result + (required ? 1231 : 1237);
            result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
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
            Occurence other = (Occurence) obj;
            if (maxOccursUnbounded != other.maxOccursUnbounded)
                return false;
            if (required != other.required)
                return false;
            if (typeId == null) {
                if (other.typeId != null)
                    return false;
            } else if (!typeId.equals(other.typeId))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[typeId=" + typeId + ", required=" + required + ", maxOccursUnbounded=" + maxOccursUnbounded + "]";
        }
    }

    public static class Dependency {

        public static Dependency create(String depId, ConfigTypeId typeId, boolean optional) {
            return new Dependency(depId, typeId, optional);
        }

        final String depId;
        final ConfigTypeId typeId;
        final boolean optional;

        private Dependency(String depId, ConfigTypeId typeId, boolean optional) {
            this.depId = depId;
            this.typeId = typeId;
            this.optional = optional;
        }

        public String getDepId() {
            return depId;
        }

        public ConfigTypeId getTypeId() {
            return typeId;
        }

        public boolean isOptional() {
            return optional;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((depId == null) ? 0 : depId.hashCode());
            result = prime * result + (optional ? 1231 : 1237);
            result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
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
            Dependency other = (Dependency) obj;
            if (depId == null) {
                if (other.depId != null)
                    return false;
            } else if (!depId.equals(other.depId))
                return false;
            if (optional != other.optional)
                return false;
            if (typeId == null) {
                if (other.typeId != null)
                    return false;
            } else if (!typeId.equals(other.typeId))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[depId=" + depId + ", typeId=" + typeId + ", optional=" + optional + "]";
        }
    }

    public static class Builder {

        private final ConfigTypeId typeId;
        private Map<ConfigTypeId, Occurence> subconfigs = Collections.emptyMap();
        private Map<String, Dependency> dependencies = Collections.emptyMap();

        private Builder(ConfigTypeId typeId) {
            this.typeId = typeId;
        }

        public Builder addOccurence(ConfigTypeId typeId) {
            return addOccurence(typeId, true);
        }

        public Builder addOccurence(ConfigTypeId typeId, boolean required) {
            return addOccurence(typeId, required, false);
        }

        public Builder addOccurence(ConfigTypeId typeId, boolean required, boolean maxOccursUnbounded) {
            switch(subconfigs.size()) {
                case 0:
                    subconfigs = Collections.singletonMap(typeId, new Occurence(typeId, required, maxOccursUnbounded));
                    break;
                case 1:
                    subconfigs = new LinkedHashMap<>(subconfigs);
                default:
                    subconfigs.put(typeId, new Occurence(typeId, required, maxOccursUnbounded));
            }
            return this;
        }

        public Builder addDependency(String depId, ConfigTypeId typeId) {
            return addDependency(depId, typeId, false);
        }

        public Builder addDependency(String depId, ConfigTypeId typeId, boolean optional) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(depId, new Dependency(depId, typeId, optional));
                    break;
                case 1:
                    dependencies = new LinkedHashMap<>(dependencies);
                default:
                    dependencies.put(depId, new Dependency(depId, typeId, optional));
            }
            return this;
        }

        public FeatureConfigType build() {
            return new FeatureConfigType(this);
        }

        public FeatureConfigType build(FeatureConfigSchema.Builder schema) {
            final FeatureConfigType type = build();
            schema.add(type);
            return type;
        }

    }

    public static Builder builder(String groupId, String artifactId, String name) {
        return builder(ConfigTypeId.create(groupId, artifactId, name));
    }

    public static Builder builder(ArtifactCoords.Ga ga, String name) {
        return builder(ConfigTypeId.create(ga, name));
    }

    public static Builder builder(ConfigTypeId typeId) {
        return new Builder(typeId);
    }

    final ConfigTypeId id;

    // Subconfigs (local configs) by spec id
    final Map<ConfigTypeId, Occurence> occurences;

    // dependencies on external configs by dependency id
    final Map<String, Dependency> dependencies;

//    private final Set<String> packages;
//    private final Map<String, Set<String>> externalPackages;
//    private final Map<String, PackageParameter> params;

    private FeatureConfigType(Builder builder) {
        this.id = builder.typeId;
        this.occurences = builder.subconfigs.size() > 1 ? Collections.unmodifiableMap(builder.subconfigs) : builder.subconfigs;
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableMap(builder.dependencies) : builder.dependencies;
    }

    public ConfigTypeId getTypeId() {
        return id;
    }

    public FeatureConfig.Builder configBuilder() {
        return new FeatureConfig.Builder(this, null);
    }

    public FeatureConfig.Builder configBuilder(String name) {
        return new FeatureConfig.Builder(this, name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((occurences == null) ? 0 : occurences.hashCode());
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
        FeatureConfigType other = (FeatureConfigType) obj;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (occurences == null) {
            if (other.occurences != null)
                return false;
        } else if (!occurences.equals(other.occurences))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[typeId=" + id + ", subconfigs=" + occurences + ", dependencies=" + dependencies + "]";
    }
}
