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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.util.DescrFormatter;

/**
 * Describes what is different in the feature-pack comparing to another feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackSpecificDescription {

    static class Builder {

        private final GAV gav;
        private Map<GAV, ProvisionedFeaturePackDescription> dependencies = Collections.emptyMap();
        private Map<String, PackageDescription> uniquePackages = Collections.emptyMap();
        private Map<String, PackageSpecificDescription> conflictingPackages = Collections.emptyMap();
        private Map<String, PackageDescription> matchedPackages = Collections.emptyMap();

        private Builder(GAV gav) {
            this.gav = gav;
        }

        Builder addAllDependencies(Collection<ProvisionedFeaturePackDescription> deps) {
            if(deps.isEmpty()) {
                return this;
            } else {
                for(ProvisionedFeaturePackDescription dep: deps) {
                    addDependency(dep);
                }
            }
            return this;
        }

        Builder addDependency(ProvisionedFeaturePackDescription dep) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(dep.getGAV(), dep);
                    break;
                case 1:
                    dependencies = new HashMap<GAV, ProvisionedFeaturePackDescription>(dependencies);
                default:
                    dependencies.put(dep.getGAV(), dep);
            }
            return this;
        }

        Builder addConflictingPackage(PackageSpecificDescription pkg) {
            assert pkg != null : "pkg is null";
            switch(conflictingPackages.size()) {
                case 0:
                    conflictingPackages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    conflictingPackages = new HashMap<String, PackageSpecificDescription>(conflictingPackages);
                default:
                    conflictingPackages.put(pkg.getName(), pkg);
            }
            return this;
        }

        Builder addMatchedPackage(PackageDescription pkg) {
            assert pkg != null : "pkg is null";
            switch(matchedPackages.size()) {
                case 0:
                    matchedPackages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    matchedPackages = new HashMap<String, PackageDescription>(matchedPackages);
                default:
                    matchedPackages.put(pkg.getName(), pkg);
            }
            return this;
        }

        Builder addUniquePackage(PackageDescription pkg) {
            assert pkg != null : "pkg is null";
            switch(uniquePackages.size()) {
                case 0:
                    uniquePackages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    uniquePackages = new HashMap<String, PackageDescription>(uniquePackages);
                default:
                    uniquePackages.put(pkg.getName(), pkg);
            }
            return this;
        }

        Builder addAllUniquePackages(Collection<PackageDescription> packages) {
            if(packages.isEmpty()) {
                return this;
            }
            for(PackageDescription pkg : packages) {
                addUniquePackage(pkg);
            }
            return this;
        }

        FeaturePackSpecificDescription build() {
            return new FeaturePackSpecificDescription(gav,
                    Collections.unmodifiableMap(dependencies),
                    Collections.unmodifiableMap(uniquePackages),
                    Collections.unmodifiableMap(conflictingPackages),
                    Collections.unmodifiableMap(matchedPackages));
        }
    }

    static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Map<GAV, ProvisionedFeaturePackDescription> dependencies;
    private final Map<String, PackageDescription> uniquePackages;
    private final Map<String, PackageSpecificDescription> conflictingPackages;
    private final Map<String, PackageDescription> matchedPackages;

    FeaturePackSpecificDescription(GAV gav,
            Map<GAV, ProvisionedFeaturePackDescription> dependencies,
            Map<String, PackageDescription> uniquePackages,
            Map<String, PackageSpecificDescription> conflictingPackages,
            Map<String, PackageDescription> matchedPackages) {
        this.gav = gav;
        this.dependencies = dependencies;
        this.uniquePackages = uniquePackages;
        this.conflictingPackages = conflictingPackages;
        this.matchedPackages = matchedPackages;
    }

    public GAV getGav() {
        return gav;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<GAV> getDependencyGAVs() {
        return dependencies.keySet();
    }

    public Collection<ProvisionedFeaturePackDescription> getDependencies() {
        return dependencies.values();
    }

    public boolean hasUniquePackages() {
        return !uniquePackages.isEmpty();
    }

    public Set<String> getUniquePackageNames() {
        return uniquePackages.keySet();
    }

    public Collection<PackageDescription> getUniquePackages() {
        return uniquePackages.values();
    }

    public PackageDescription getUniquePackage(String name) {
        return uniquePackages.get(name);
    }

    public boolean hasConflictingPackages() {
        return !conflictingPackages.isEmpty();
    }

    public Set<String> getConflictingPackageNames() {
        return conflictingPackages.keySet();
    }

    public PackageSpecificDescription getConflictingPackage(String name) {
        return conflictingPackages.get(name);
    }

    public boolean hasMatchedPackages() {
        return !matchedPackages.isEmpty();
    }

    public Set<String> getMatchedPackageNames() {
        return matchedPackages.keySet();
    }

    public PackageDescription getMatchedPackage(String name) {
        return matchedPackages.get(name);
    }

    public boolean isMatchedPackage(String name) {
        return matchedPackages.containsKey(name);
    }

    public String logContent() throws IOException {
        final DescrFormatter out = new DescrFormatter();
        out.print("Feature-pack ").println(gav.toString());
        out.increaseOffset();
        if(!dependencies.isEmpty()) {
            out.println("Dependencies:");
            out.increaseOffset();
            for(GAV gav : dependencies.keySet()) {
                out.println(gav.toString());
            }
            out.decreaseOffset();
        }
        if(!matchedPackages.isEmpty()) {
            final List<String> names = new ArrayList<String>(matchedPackages.keySet());
            names.sort(null);
            out.println("Matched packages");
            out.increaseOffset();
            for(String name : names) {
                out.println(name);
            }
            out.decreaseOffset();
        }
        if(!uniquePackages.isEmpty()) {
            final List<String> names = new ArrayList<String>(uniquePackages.keySet());
            names.sort(null);
            out.println("Unique packages");
            out.increaseOffset();
            for(String name : names) {
                out.println(name);
            }
            out.decreaseOffset();
        }
        if(!conflictingPackages.isEmpty()) {
            final List<String> names = new ArrayList<String>(conflictingPackages.keySet());
            names.sort(null);
            out.println("Package differences");
            out.increaseOffset();
            for(String name : names) {
                conflictingPackages.get(name).logContent(out);
            }
            out.decreaseOffset();
        }
        out.decreaseOffset();
        return out.toString();
    }
}
