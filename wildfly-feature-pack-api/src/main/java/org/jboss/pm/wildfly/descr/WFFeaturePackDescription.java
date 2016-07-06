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

package org.jboss.pm.wildfly.descr;

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
