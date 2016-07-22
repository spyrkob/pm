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

package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.GroupDescription;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * Describes what is different in the feature-pack comparing to another feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackSpecificDescription {

    static class Builder {

        private final GAV gav;
        private Set<GAV> dependencies = Collections.emptySet();
        private Map<String, GroupDescription> uniqueGroups = Collections.emptyMap();
        private Map<String, GroupSpecificDescription> conflictingGroups = Collections.emptyMap();

        private Builder(GAV gav) {
            this.gav = gav;
        }

        Builder addAllDependencies(Collection<GAV> gavs) {
            if(gavs.isEmpty()) {
                return this;
            } else if(gavs.size() == 1) {
                for(GAV gav : gavs) {
                    addDependency(gav);
                }
            }
            return this;
        }

        Builder addDependency(GAV gav) {
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

        Builder addConflictingGroup(GroupSpecificDescription group) {
            assert group != null : "group is null";
            switch(conflictingGroups.size()) {
                case 0:
                    conflictingGroups = Collections.singletonMap(group.getName(), group);
                    break;
                case 1:
                    conflictingGroups = new HashMap<String, GroupSpecificDescription>(conflictingGroups);
                default:
                    conflictingGroups.put(group.getName(), group);
            }
            return this;
        }

        Builder addUniqueGroup(GroupDescription group) {
            assert group != null : "group is null";
            switch(uniqueGroups.size()) {
                case 0:
                    uniqueGroups = Collections.singletonMap(group.getName(), group);
                    break;
                case 1:
                    uniqueGroups = new HashMap<String, GroupDescription>(uniqueGroups);
                default:
                    uniqueGroups.put(group.getName(), group);
            }
            return this;
        }

        Builder addAllUniqueGroups(Collection<GroupDescription> groups) {
            if(groups.isEmpty()) {
                return this;
            }
            for(GroupDescription group : groups) {
                addUniqueGroup(group);
            }
            return this;
        }

        FeaturePackSpecificDescription build() {
            return new FeaturePackSpecificDescription(gav,
                    Collections.unmodifiableSet(dependencies),
                    Collections.unmodifiableMap(uniqueGroups),
                    Collections.unmodifiableMap(conflictingGroups));
        }
    }

    static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Set<GAV> dependencies;
    private final Map<String, GroupDescription> uniqueGroups;
    private final Map<String, GroupSpecificDescription> conflictingGroups;

    FeaturePackSpecificDescription(GAV gav, Set<GAV> dependencies, Map<String, GroupDescription> uniqueGroups, Map<String, GroupSpecificDescription> conflictingGroups) {
        this.gav = gav;
        this.dependencies = dependencies;
        this.uniqueGroups = uniqueGroups;
        this.conflictingGroups = conflictingGroups;
    }

    public GAV getGav() {
        return gav;
    }

    public Set<GAV> getDependencies() {
        return dependencies;
    }

    public Set<String> getUniqueGroupNames() {
        return uniqueGroups.keySet();
    }

    public GroupDescription getUniqueGroup(String name) {
        return uniqueGroups.get(name);
    }

    public Set<String> getConflictingGroupNames() {
        return conflictingGroups.keySet();
    }

    public GroupSpecificDescription getConflictingGroup(String name) {
        return conflictingGroups.get(name);
    }

    public String logContent() throws IOException {
        final DescrFormatter out = new DescrFormatter();
        out.print("Feature-pack ").println(gav.toString());
        out.increaseOffset();
        if(!uniqueGroups.isEmpty()) {
            final List<String> names = new ArrayList<String>(uniqueGroups.keySet());
            names.sort(null);
            out.println("Unique packages");
            out.increaseOffset();
            for(String group : names) {
                out.println(group);
            }
            out.decreaseOffset();
        }
        if(!conflictingGroups.isEmpty()) {
            final List<String> names = new ArrayList<String>(conflictingGroups.keySet());
            names.sort(null);
            out.println("Common packages");
            out.increaseOffset();
            for(String group : names) {
                conflictingGroups.get(group).logContent(out);
            }
            out.decreaseOffset();
        }
        if(!dependencies.isEmpty()) {
            out.println("Dependencies:");
            out.increaseOffset();
            for(GAV gav : dependencies) {
                out.println(gav.toString());
            }
            out.decreaseOffset();
        }
        out.decreaseOffset();
        return out.toString();
    }
}
