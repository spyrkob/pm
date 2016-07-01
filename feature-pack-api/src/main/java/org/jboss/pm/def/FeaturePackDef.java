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

package org.jboss.pm.def;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pm.GAV;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDef {

    public static class FeaturePackDefBuilder {

        private GAV gav;
        private Map<String, GroupDef> groups = Collections.emptyMap();
        private Set<GAV> dependencies = Collections.emptySet();

        protected FeaturePackDefBuilder() {
            this(null);
        }

        protected FeaturePackDefBuilder(GAV gav) {
            this.gav = gav;
        }

        public FeaturePackDefBuilder setGAV(GAV gav) {
            this.gav = gav;
            return this;
        }

        public FeaturePackDefBuilder addGroup(GroupDef group) {
            assert group != null : "group is null";
            switch(groups.size()) {
                case 0:
                    groups = Collections.singletonMap(group.getName(), group);
                    break;
                case 1:
                    groups = new HashMap<String, GroupDef>(groups);
                default:
                    groups.put(group.getName(), group);
            }
            return this;
        }

        public FeaturePackDefBuilder addDependency(GAV gav) {
            assert gav != null : "GAV is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singleton(gav);
                    break;
                case 1:
                    dependencies = new HashSet<GAV>(dependencies);
                default:
                    dependencies.add(gav);
            }
            return this;
        }

        public FeaturePackDef build() {
            return new FeaturePackDef(gav, Collections.unmodifiableMap(groups), Collections.unmodifiableSet(dependencies));
        }
    }

    public static FeaturePackDefBuilder builder() {
        return builder(null);
    }

    public static FeaturePackDefBuilder builder(GAV gav) {
        return new FeaturePackDefBuilder(gav);
    }

    private final GAV gav;
    private final Map<String, GroupDef> groups;
    private final Set<GAV> dependencies;

    protected FeaturePackDef(GAV gav, Map<String, GroupDef> groups, Set<GAV> dependencies) {
        assert gav != null : "GAV is null";
        assert groups != null : "groups is null";
        assert dependencies != null : "dependencies is null";
        this.gav = gav;
        this.groups = groups;
        this.dependencies = dependencies;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasGroups() {
        return !groups.isEmpty();
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public GroupDef getGroupDef(String name) {
        return groups.get(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<GAV> getDependencies() {
        return dependencies;
    }

    void logContent(DefLogger logger) throws IOException {
        logger.print("FeaturePack ");
        logger.println(gav.toString());
        logger.increaseOffset();

        if(!groups.isEmpty()) {
            final List<String> names = new ArrayList<String>(groups.keySet());
            names.sort(null);
            for(String group : names) {
                groups.get(group).logContent(logger);
            }
        }

        if(!dependencies.isEmpty()) {
            logger.println("Dependencies:");
            logger.increaseOffset();
            for(GAV gav : dependencies) {
                logger.println(gav.toString());
            }
            logger.decreaseOffset();
        }
        logger.decreaseOffset();
    }
}
