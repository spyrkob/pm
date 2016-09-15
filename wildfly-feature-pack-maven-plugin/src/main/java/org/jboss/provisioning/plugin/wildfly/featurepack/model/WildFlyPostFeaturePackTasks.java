/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provisioning.plugin.wildfly.featurepack.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 * @author Alexey Loubyansky
 */
public class WildFlyPostFeaturePackTasks {

    public static class Builder {

        private ConfigDescription config;
        private List<FilePermission> filePermissions = Collections.emptyList();
        private List<String> mkDirs = Collections.emptyList();
        private List<FileFilter> windowsLineEndFilters = Collections.emptyList();
        private List<FileFilter> unixLineEndFilters = Collections.emptyList();

        private Builder() {
        }

        public Builder setConfig(ConfigDescription config) {
            this.config = config;
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

        public Builder addMkDirs(String mkdirs) {
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

        public Builder addMkDirs(List<String> mkdirs) {
            for(String mkdir : mkdirs) {
                addMkDirs(mkdir);
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

        public Builder addWindowsLineEndFilters(List<FileFilter> filters) {
            for(FileFilter filter : filters) {
                addWindowsLineEndFilter(filter);
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

        public Builder addUnixLineEndFilters(List<FileFilter> filters) {
            for(FileFilter filter : filters) {
                addUnixLineEndFilter(filter);
            }
            return this;
        }

        public WildFlyPostFeaturePackTasks build() {
            return new WildFlyPostFeaturePackTasks(config, Collections.unmodifiableList(filePermissions),
                    Collections.unmodifiableList(mkDirs), Collections.unmodifiableList(windowsLineEndFilters),
                    Collections.unmodifiableList(unixLineEndFilters));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ConfigDescription config;
    private final List<FilePermission> filePermissions;
    private final List<String> mkDirs;
    private final List<FileFilter> windowsLineEndFilters;
    private final List<FileFilter> unixLineEndFilters;

    private WildFlyPostFeaturePackTasks(ConfigDescription config, List<FilePermission> filePermissions,
            List<String> mkdirs, List<FileFilter> windowsLineEndFilters, List<FileFilter> unixLineEndFilters) {
        this.config = config;
        this.filePermissions = filePermissions;
        this.mkDirs = mkdirs;
        this.windowsLineEndFilters = windowsLineEndFilters;
        this.unixLineEndFilters = unixLineEndFilters;
    }

    public ConfigDescription getConfig() {
        return config;
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
}
