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
package org.jboss.provisioning.plugin.wildfly.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;


/**
 *
 * @author Alexey Loubyansky
 */
public class WildFlyPackageTasks {

    public static class Builder {

        private List<CopyArtifact> copyArtifacts = Collections.emptyList();
        private List<FilePermission> filePermissions = Collections.emptyList();
        private List<String> mkDirs = Collections.emptyList();
        private List<FileFilter> windowsLineEndFilters = Collections.emptyList();
        private List<FileFilter> unixLineEndFilters = Collections.emptyList();
        private GeneratorConfig generatorConfig;

        private Builder() {
        }

        public Builder setGeneratorConfig(GeneratorConfig genConfig) {
            this.generatorConfig = genConfig;
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

        public WildFlyPackageTasks build() {
            return new WildFlyPackageTasks(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WildFlyPackageTasks load(Path configFile) throws ProvisioningException {
        try (InputStream configStream = Files.newInputStream(configFile)) {
            return new WildFlyPackageTasksParser().parse(configStream);
        } catch (XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(configFile), e);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.openFile(configFile), e);
        }
    }

    private final List<CopyArtifact> copyArtifacts;
    private final List<FilePermission> filePermissions;
    private final List<String> mkDirs;
    private final List<FileFilter> windowsLineEndFilters;
    private final List<FileFilter> unixLineEndFilters;
    private final GeneratorConfig generatorConfig;

    private WildFlyPackageTasks(Builder builder) {
        this.copyArtifacts = Collections.unmodifiableList(builder.copyArtifacts);
        this.filePermissions = Collections.unmodifiableList(builder.filePermissions);
        this.mkDirs = Collections.unmodifiableList(builder.mkDirs);
        this.windowsLineEndFilters = Collections.unmodifiableList(builder.windowsLineEndFilters);
        this.unixLineEndFilters = Collections.unmodifiableList(builder.unixLineEndFilters);
        this.generatorConfig = builder.generatorConfig;
    }

    public boolean hasCopyArtifacts() {
        return !copyArtifacts.isEmpty();
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    public boolean hasFilePermissions() {
        return !filePermissions.isEmpty();
    }

    public List<FilePermission> getFilePermissions() {
        return filePermissions;
    }

    public boolean hasMkDirs() {
        return !mkDirs.isEmpty();
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

    public GeneratorConfig getGeneratorConfig() {
        return generatorConfig;
    }
}
