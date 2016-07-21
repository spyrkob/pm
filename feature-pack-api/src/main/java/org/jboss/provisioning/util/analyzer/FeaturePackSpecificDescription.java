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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;
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
        private Map<String, GroupSpecificDescription> groups = Collections.emptyMap();

        private Builder(GAV gav) {
            this.gav = gav;
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

        Builder addGroup(GroupSpecificDescription group) {
            assert group != null : "group is null";
            switch(groups.size()) {
                case 0:
                    groups = Collections.singletonMap(group.getName(), group);
                    break;
                case 1:
                    groups = new HashMap<String, GroupSpecificDescription>(groups);
                default:
                    groups.put(group.getName(), group);
            }
            return this;
        }

        FeaturePackSpecificDescription build() {
            return new FeaturePackSpecificDescription(gav, Collections.unmodifiableSet(dependencies), Collections.unmodifiableMap(groups));
        }
    }

    static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Set<GAV> dependencies;
    private final Map<String, GroupSpecificDescription> groups;

    FeaturePackSpecificDescription(GAV gav, Set<GAV> dependencies, Map<String, GroupSpecificDescription> groups) {
        this.gav = gav;
        this.dependencies = dependencies;
        this.groups = groups;
    }

    public GAV getGav() {
        return gav;
    }

    public Set<GAV> getDependencies() {
        return dependencies;
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public GroupSpecificDescription getGroup(String name) {
        return groups.get(name);
    }

    public String logContent() throws IOException {
        final DescrFormatter out = new DescrFormatter();
        out.print("FeaturePackSpecificDescription ").println(gav.toString());
        out.increaseOffset();
        if(!groups.isEmpty()) {
            final List<String> names = new ArrayList<String>(groups.keySet());
            names.sort(null);
            for(String group : names) {
                groups.get(group).logContent(out);
            }
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
