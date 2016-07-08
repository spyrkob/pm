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

package org.jboss.pm.descr;

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
public class FeaturePackDescription {

    public static class Builder {

        private GAV gav;
        private Set<GAV> dependencies = Collections.emptySet();
        private Set<String> topGroups = Collections.emptySet();
        private Map<String, GroupDescription> groups = Collections.emptyMap();

        protected Builder() {
            this(null);
        }

        protected Builder(GAV gav) {
            this.gav = gav;
        }

        public Builder setGAV(GAV gav) {
            this.gav = gav;
            return this;
        }

        public Builder addTopGroup(GroupDescription group) {
            assert group != null : "group is null";
            switch(topGroups.size()) {
                case 0:
                    topGroups = Collections.singleton(group.getName());
                    break;
                case 1:
                    topGroups = new HashSet<String>(topGroups);
                default:
                    topGroups.add(group.getName());
            }
            addGroup(group);
            return this;
        }

        public Builder addGroup(GroupDescription group) {
            assert group != null : "group is null";
            switch(groups.size()) {
                case 0:
                    groups = Collections.singletonMap(group.getName(), group);
                    break;
                case 1:
                    groups = new HashMap<String, GroupDescription>(groups);
                default:
                    groups.put(group.getName(), group);
            }
            return this;
        }

        public Builder addDependency(GAV gav) {
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

        public FeaturePackDescription build() {
            return new FeaturePackDescription(gav, Collections.unmodifiableSet(topGroups), Collections.unmodifiableMap(groups), Collections.unmodifiableSet(dependencies));
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Set<GAV> dependencies;
    private final Set<String> topGroups;
    private final Map<String, GroupDescription> groups;

    protected FeaturePackDescription(GAV gav, Set<String> topGroups, Map<String, GroupDescription> groups, Set<GAV> dependencies) {
        assert gav != null : "GAV is null";
        assert dependencies != null : "dependencies is null";
        assert topGroups != null : "topGroups is null";
        assert groups != null : "groups is null";
        this.gav = gav;
        this.topGroups = topGroups;
        this.groups = groups;
        this.dependencies = dependencies;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasTopGroups() {
        return !topGroups.isEmpty();
    }

    public Set<String> getTopGroupNames() {
        return topGroups;
    }

    public boolean hasGroups() {
        return !groups.isEmpty();
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public GroupDescription getGroupDescription(String name) {
        return groups.get(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<GAV> getDependencies() {
        return dependencies;
    }

    void logContent(DescrLogger logger) throws IOException {
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
