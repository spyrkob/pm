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
package org.jboss.provisioning.wildfly.build;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.plugin.wildfly.config.FileFilter;
import org.jboss.provisioning.plugin.wildfly.config.FilePermission;
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
        private List<CopyArtifact> copyArtifacts = Collections.emptyList();
        private List<FilePermission> filePermissions = Collections.emptyList();
        private List<String> mkDirs = Collections.emptyList();
        private List<FileFilter> windowsLineEndFilters = Collections.emptyList();
        private List<FileFilter> unixLineEndFilters = Collections.emptyList();
        private boolean packageSchemas;
        private Set<String> schemaGroups = Collections.emptySet();
        private Set<String> defaultPackages = new HashSet<>();

        private Builder() {
        }

        public Builder addDefaultPackage(String packageName) {
            defaultPackages.add(packageName);
            return this;
        }

        public Builder addDependency(FeaturePackDependencySpec dependency) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependency);
                    break;
                case 1:
                    dependencies = new ArrayList<FeaturePackDependencySpec>(dependencies);
                default:
                    dependencies.add(dependency);
            }
            return this;
        }

        public Builder addCopyArtifact(CopyArtifact copy) {
            switch(copyArtifacts.size()) {
                case 0:
                    copyArtifacts = Collections.singletonList(copy);
                    break;
                case 1:
                    copyArtifacts = new ArrayList<CopyArtifact>(copyArtifacts);
                default:
                    copyArtifacts.add(copy);
            }
            return this;
        }

        public Builder addCopyArtifacts(List<CopyArtifact> copyArtifacts) {
            for(CopyArtifact ca : copyArtifacts) {
                addCopyArtifact(ca);
            }
            return this;
        }

        public Builder addFilePermissions(FilePermission filePermission) {
            switch(filePermissions.size()) {
                case 0:
                    filePermissions = Collections.singletonList(filePermission);
                    break;
                case 1:
                    filePermissions = new ArrayList<FilePermission>(filePermissions);
                default:
                    filePermissions.add(filePermission);
            }
            return this;
        }

        public Builder addFilePermissions(List<FilePermission> filePermissions) {
            for(FilePermission fp : filePermissions) {
                addFilePermissions(fp);
            }
            return this;
        }

        public Builder addMkdirs(String mkdirs) {
            switch(mkDirs.size()) {
                case 0:
                    mkDirs = Collections.singletonList(mkdirs);
                    break;
                case 1:
                    mkDirs = new ArrayList<String>(mkDirs);
                default:
                    mkDirs.add(mkdirs);
            }
            return this;
        }

        public Builder addWindowsLineEndFilter(FileFilter filter) {
            switch(windowsLineEndFilters.size()) {
                case 0:
                    windowsLineEndFilters = Collections.singletonList(filter);
                    break;
                case 1:
                    windowsLineEndFilters = new ArrayList<FileFilter>(windowsLineEndFilters);
                default:
                    windowsLineEndFilters.add(filter);
            }
            return this;
        }

        public Builder addUnixLineEndFilter(FileFilter filter) {
            switch(unixLineEndFilters.size()) {
                case 0:
                    unixLineEndFilters = Collections.singletonList(filter);
                    break;
                case 1:
                    unixLineEndFilters = new ArrayList<FileFilter>(unixLineEndFilters);
                default:
                    unixLineEndFilters.add(filter);
            }
            return this;
        }

        public Builder setPackageSchemas() {
            this.packageSchemas = true;
            return this;
        }

        public Builder addSchemaGroup(String groupId) {
            switch(schemaGroups.size()) {
                case 0:
                    schemaGroups = Collections.singleton(groupId);
                    break;
                case 1:
                    schemaGroups = new HashSet<String>(schemaGroups);
                default:
                    schemaGroups.add(groupId);
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
    private final List<CopyArtifact> copyArtifacts;
    private final List<FilePermission> filePermissions;
    private final List<String> mkDirs;
    private final List<FileFilter> windowsLineEndFilters;
    private final List<FileFilter> unixLineEndFilters;
    private final boolean packageSchemas;
    private final Set<String> schemaGroups;
    private final Set<String> defaultPackages;

    private WildFlyFeaturePackBuild(Builder builder) {
        this.dependencies = Collections.unmodifiableList(builder.dependencies);
        this.copyArtifacts = Collections.unmodifiableList(builder.copyArtifacts);
        this.filePermissions = Collections.unmodifiableList(builder.filePermissions);
        this.mkDirs = Collections.unmodifiableList(builder.mkDirs);
        this.windowsLineEndFilters = Collections.unmodifiableList(builder.windowsLineEndFilters);
        this.unixLineEndFilters = Collections.unmodifiableList(builder.unixLineEndFilters);
        this.packageSchemas = builder.packageSchemas;
        this.schemaGroups = Collections.unmodifiableSet(builder.schemaGroups);
        this.defaultPackages = Collections.unmodifiableSet(builder.defaultPackages);
    }

    public Collection<String> getDefaultPackages() {
        return defaultPackages;
    }

    public List<FeaturePackDependencySpec> getDependencies() {
        return dependencies;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    public List<FilePermission> getFilePermissions() {
        return filePermissions;
    }

    public List<String> getMkDirs() {
        return mkDirs;
    }

    public List<FileFilter> getWindowsLineEndFilters() {
        return windowsLineEndFilters;
    }

    public List<FileFilter> getUnixLineEndFilters() {
        return unixLineEndFilters;
    }

    public boolean isPackageSchemas() {
        return packageSchemas;
    }

    public boolean isSchemaGroup(String groupId) {
        return schemaGroups.contains(groupId);
    }
}
