/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.provisioning.plugin.wildfly.featurepack.build.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.Config;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.CopyArtifact;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.FileFilter;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.FilePermission;

/**
 * Representation of the feature pack build config
 *
 * @author Stuart Douglas
 * @author Alexey Loubyansky
 */
public class WildFlyFeaturePackBuild {

    public static class Builder {

        private List<FeaturePackDependencyDescription> dependencies = Collections.emptyList();
        private final Config config = new Config();
        private List<CopyArtifact> copyArtifacts = Collections.emptyList();
        private List<FilePermission> filePermissions = Collections.emptyList();
        private List<String> mkDirs = Collections.emptyList();
        private List<FileFilter> windows = Collections.emptyList();
        private List<FileFilter> unix = Collections.emptyList();

        private Builder() {
        }

        public Builder addDependency(FeaturePackDependencyDescription dependency) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependency);
                    break;
                case 1:
                    dependencies = new ArrayList<FeaturePackDependencyDescription>(dependencies);
                default:
                    dependencies.add(dependency);
            }
            return this;
        }

        public Config getConfig() {
            return config;
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

        public Builder addWindows(FileFilter filter) {
            switch(windows.size()) {
                case 0:
                    windows = Collections.singletonList(filter);
                    break;
                case 1:
                    windows = new ArrayList<FileFilter>(windows);
                default:
                    windows.add(filter);
            }
            return this;
        }

        public Builder addUnix(FileFilter filter) {
            switch(unix.size()) {
                case 0:
                    unix = Collections.singletonList(filter);
                    break;
                case 1:
                    unix = new ArrayList<FileFilter>(unix);
                default:
                    unix.add(filter);
            }
            return this;
        }

        public WildFlyFeaturePackBuild build() {
            return new WildFlyFeaturePackBuild(Collections.unmodifiableList(dependencies), config,
                    Collections.unmodifiableList(copyArtifacts), Collections.unmodifiableList(filePermissions),
                    Collections.unmodifiableList(mkDirs), Collections.unmodifiableList(windows), Collections.unmodifiableList(unix));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<FeaturePackDependencyDescription> dependencies;
    private final Config config;
    private final List<CopyArtifact> copyArtifacts;
    private final List<FilePermission> filePermissions;
    private final List<String> mkDirs;
    private final List<FileFilter> windows;
    private final List<FileFilter> unix;

    private WildFlyFeaturePackBuild(List<FeaturePackDependencyDescription> dependencies, Config config,
            List<CopyArtifact> copyArtifacts, List<FilePermission> filePermissions, List<String> mkDirs,
            List<FileFilter> windows, List<FileFilter> unix) {
        this.dependencies = dependencies;
        this.config = config;
        this.copyArtifacts = copyArtifacts;
        this.filePermissions = filePermissions;
        this.mkDirs = mkDirs;
        this.windows = windows;
        this.unix = unix;
    }

    public List<FeaturePackDependencyDescription> getDependencies() {
        return dependencies;
    }

    public Config getConfig() {
        return config;
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

    public List<FileFilter> getWindows() {
        return windows;
    }

    public List<FileFilter> getUnix() {
        return unix;
    }
}
