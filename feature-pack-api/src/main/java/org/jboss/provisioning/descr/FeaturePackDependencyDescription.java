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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.provisioning.GAV;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyDescription {

    public static class Builder {

        private final GAV gav;
        private Set<String> excludedPackages = Collections.emptySet();

        private Builder(GAV gav) {
            this.gav = gav;
        }

        public Builder excludePackage(String name) {
            switch(excludedPackages.size()) {
                case 0:
                    excludedPackages = Collections.singleton(name);
                    break;
                case 1:
                    excludedPackages = new HashSet<String>(excludedPackages);
                default:
                    excludedPackages.add(name);
            }
            return this;
        }

        public FeaturePackDependencyDescription build() {
            return new FeaturePackDependencyDescription(gav, excludedPackages);
        }
    }

    public static Builder builder(GAV gav) {
        return new Builder(gav);
    }

    private final GAV gav;
    private final Set<String> excludedPackages;

    private FeaturePackDependencyDescription(GAV gav, Set<String> excludedPackages) {
        this.gav = gav;
        this.excludedPackages = excludedPackages;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasExcludedPackages() {
        return !excludedPackages.isEmpty();
    }

    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }
}
