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

package org.jboss.provisioning.plugin.wildfly;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
class ModuleParseResult {
    final List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
    final List<String> resourceRoots = new ArrayList<>();
    final List<ArtifactName> artifacts = new ArrayList<>();
    ModuleIdentifier identifier;

    List<ModuleDependency> getDependencies() {
        return dependencies;
    }

    List<String> getResourceRoots() {
        return resourceRoots;
    }

    List<ArtifactName> getArtifacts() {
        return artifacts;
    }

    ModuleIdentifier getIdentifier() {
        return identifier;
    }

    static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
            this.moduleId = moduleId;
            this.optional = optional;
        }

        ModuleIdentifier getModuleId() {
            return moduleId;
        }

        boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return "[" + moduleId + (optional ? ",optional=true" : "") + "]";
        }
    }

    static class ArtifactName {

        private final String artifactCoords;
        private final String options;

        ArtifactName(String artifactCoords, String options) {
            this.artifactCoords = artifactCoords;
            this.options = options;
        }

        String getArtifactCoords() {
            return artifactCoords;
        }

        String getOptions() {
            return options;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(artifactCoords);
            if (options != null) {
                sb.append('?').append(options);
            }
            return sb.toString();
        }
    }
}
