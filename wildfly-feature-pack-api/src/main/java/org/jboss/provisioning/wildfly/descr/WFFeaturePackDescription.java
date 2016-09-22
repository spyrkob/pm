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
package org.jboss.provisioning.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFFeaturePackDescription {

    public static class Builder {
        private String groupId;
        private String artifactId;
        private String version;
        private List<WFPackageDescription> packages = Collections.emptyList();

        Builder() {
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void addPackage(WFPackageDescription pkgBuilder) {
            switch(packages.size()) {
                case 0:
                    packages = Collections.singletonList(pkgBuilder);
                    break;
                case 1:
                    packages = new ArrayList<WFPackageDescription>(packages);
                default:
                    packages.add(pkgBuilder);
            }
        }

        public WFFeaturePackDescription build() {
            return new WFFeaturePackDescription(groupId, artifactId, version, Collections.unmodifiableList(packages));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final List<WFPackageDescription> packages;

    private WFFeaturePackDescription(String groupId, String artifactId, String version, List<WFPackageDescription> packages) {
        assert groupId != null : "groupId is null";
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packages = packages;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public List<WFPackageDescription> getPackages() {
        return packages;
    }
}
