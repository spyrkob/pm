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
package org.jboss.provisioning.wildfly.build;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;

/**
 * Representation of the feature pack build config
 *
 * @author Stuart Douglas
 * @author Alexey Loubyansky
 */
public class WildFlyFeaturePackBuild {

    public static class Builder {

        private List<FeaturePackDependencySpec> dependencies = Collections.emptyList();
        private Set<String> schemaGroups = Collections.emptySet();
        private Set<String> defaultPackages = Collections.emptySet();
        private List<ConfigSpec> configs = Collections.emptyList();

        private Builder() {
        }

        public Builder addDefaultPackage(String packageName) {
            switch(defaultPackages.size()) {
                case 0:
                    defaultPackages = Collections.singleton(packageName);
                    break;
                case 1:
                    final String first = defaultPackages.iterator().next();
                    defaultPackages = new HashSet<>();
                    defaultPackages.add(first);
                default:
                    defaultPackages.add(packageName);
            }
            return this;
        }

        public Builder addDependency(FeaturePackDependencySpec dependency) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependency);
                    break;
                case 1:
                    final FeaturePackDependencySpec first = dependencies.get(0);
                    dependencies = new ArrayList<FeaturePackDependencySpec>(2);
                    dependencies.add(first);
                default:
                    dependencies.add(dependency);
            }
            return this;
        }

        public Builder addSchemaGroup(String groupId) {
            switch(schemaGroups.size()) {
                case 0:
                    schemaGroups = Collections.singleton(groupId);
                    break;
                case 1:
                    final String first = schemaGroups.iterator().next();
                    schemaGroups = new HashSet<>();
                    schemaGroups.add(first);
                default:
                    schemaGroups.add(groupId);
            }
            return this;
        }

        public Builder addConfig(ConfigSpec config) {
            switch(configs.size()) {
                case 0:
                    configs = Collections.singletonList(config);
                    break;
                case 1:
                    final ConfigSpec first = configs.get(0);
                    configs = new ArrayList<>();
                    configs.add(first);
                default:
                    configs.add(config);
            }
            return this;
        }

        public WildFlyFeaturePackBuild build() {
            return new WildFlyFeaturePackBuild(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<FeaturePackDependencySpec> dependencies;
    private final Set<String> schemaGroups;
    private final Set<String> defaultPackages;
    private final List<ConfigSpec> configs;

    private WildFlyFeaturePackBuild(Builder builder) {
        this.dependencies = builder.dependencies.size() > 1 ? Collections.unmodifiableList(builder.dependencies) : builder.dependencies;
        this.schemaGroups = builder.schemaGroups.size() > 1 ? Collections.unmodifiableSet(builder.schemaGroups) : builder.schemaGroups;
        this.defaultPackages = builder.defaultPackages.size() > 1 ? Collections.unmodifiableSet(builder.defaultPackages) : builder.defaultPackages;
        this.configs = builder.configs.size() > 1 ? Collections.unmodifiableList(builder.configs) : builder.configs;
    }

    public Collection<String> getDefaultPackages() {
        return defaultPackages;
    }

    public List<FeaturePackDependencySpec> getDependencies() {
        return dependencies;
    }

    public boolean hasSchemaGroups() {
        return !schemaGroups.isEmpty();
    }

    public boolean isSchemaGroup(String groupId) {
        return schemaGroups.contains(groupId);
    }

    public Set<String> getSchemaGroups() {
        return schemaGroups;
    }

    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    public List<ConfigSpec> getConfigs() {
        return configs;
    }
}
