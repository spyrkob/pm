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

package org.jboss.provisioning.descr;

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
import org.jboss.provisioning.util.DescrFormatter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDescription {

    public static class Builder {

        private GAV gav;
        private Map<GAV, FeaturePackDependencyDescription> dependencies = Collections.emptyMap();
        private Set<String> topPackages = Collections.emptySet();
        private Map<String, PackageDescription> packages = Collections.emptyMap();

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

        public Builder addTopPackage(PackageDescription pkg) {
            addTopPackageName(pkg.getName());
            addPackage(pkg);
            return this;
        }

        public void addTopPackageName(String packageName) {
            assert packageName != null : "packageName is null";
            switch(topPackages.size()) {
                case 0:
                    topPackages = Collections.singleton(packageName);
                    break;
                case 1:
                    topPackages = new HashSet<String>(topPackages);
                default:
                    topPackages.add(packageName);
            }
        }

        public Builder addPackage(PackageDescription pkg) {
            assert pkg != null : "package is null";
            switch(packages.size()) {
                case 0:
                    packages = Collections.singletonMap(pkg.getName(), pkg);
                    break;
                case 1:
                    packages = new HashMap<String, PackageDescription>(packages);
                default:
                    packages.put(pkg.getName(), pkg);
            }
            return this;
        }

        public Builder addDependency(FeaturePackDependencyDescription dependency) {
            assert gav != null : "GAV is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(dependency.getGAV(), dependency);
                    break;
                case 1:
                    dependencies = new HashMap<GAV, FeaturePackDependencyDescription>(dependencies);
                default:
                    dependencies.put(dependency.getGAV(), dependency);
            }
            return this;
        }

        public Builder addAllDependencies(Collection<FeaturePackDependencyDescription> dependencies) {
            for(FeaturePackDependencyDescription dependency : dependencies) {
                addDependency(dependency);
            }
            return this;
        }

        public FeaturePackDescription build() {
            return new FeaturePackDescription(gav, Collections.unmodifiableSet(topPackages), Collections.unmodifiableMap(packages), Collections.unmodifiableMap(dependencies));
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Map<GAV, FeaturePackDependencyDescription> dependencies;
    private final Set<String> topPackages;
    private final Map<String, PackageDescription> packages;

    protected FeaturePackDescription(GAV gav, Set<String> topPackages, Map<String, PackageDescription> packages, Map<GAV, FeaturePackDependencyDescription> dependencies) {
        assert gav != null : "GAV is null";
        assert dependencies != null : "dependencies is null";
        assert topPackages != null : "topPackages is null";
        assert packages != null : "packages is null";
        this.gav = gav;
        this.topPackages = topPackages;
        this.packages = packages;
        this.dependencies = dependencies;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasTopPackages() {
        return !topPackages.isEmpty();
    }

    public Set<String> getTopPackageNames() {
        return topPackages;
    }

    public boolean isTopPackage(String name) {
        return topPackages.contains(name);
    }

    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    public boolean hasPackage(String name) {
        return packages.containsKey(name);
    }

    public Collection<PackageDescription> getPackages() {
        return packages.values();
    }

    public Set<String> getPackageNames() {
        return packages.keySet();
    }

    public PackageDescription getPackageDescription(String name) {
        return packages.get(name);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<GAV> getDependencyGAVs() {
        return dependencies.keySet();
    }

    public Collection<FeaturePackDependencyDescription> getDependencies() {
        return dependencies.values();
    }

    public FeaturePackDependencyDescription getDependency(GAV gav) {
        return dependencies.get(gav);
    }

    public String logContent() throws IOException {
        final DescrFormatter logger = new DescrFormatter();
        logContent(logger);
        return logger.toString();
    }

    void logContent(DescrFormatter logger) throws IOException {
        logger.print("FeaturePack ");
        logger.println(gav.toString());
        logger.increaseOffset();

        if(!packages.isEmpty()) {
            final List<String> names = new ArrayList<String>(packages.keySet());
            names.sort(null);
            for(String name : names) {
                packages.get(name).logContent(logger);
            }
        }

        if(!dependencies.isEmpty()) {
            logger.println("Dependencies:");
            logger.increaseOffset();
            for(GAV gav : dependencies.keySet()) {
                logger.println(gav.toString());
            }
            logger.decreaseOffset();
        }
        logger.decreaseOffset();
    }
}
