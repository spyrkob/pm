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

package org.jboss.provisioning.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFInstallationDescription {

    public static class Builder {

        private String modulesPath;
        private List<WFFeaturePackDescription> featurePacks = Collections.emptyList();

        Builder() {
        }

        public void setModulesPath(String modulesPath) {
            if(this.modulesPath != null) {
                throw new IllegalStateException("Modules path has already been set");
            }
            this.modulesPath = modulesPath;
        }

        public void addFeaturePack(WFFeaturePackDescription fpBuilder) {
            switch(featurePacks.size()) {
                case 0:
                    featurePacks = Collections.singletonList(fpBuilder);
                    break;
                case 1:
                    featurePacks = new ArrayList<WFFeaturePackDescription>(featurePacks);
                default:
                    featurePacks.add(fpBuilder);
            }
        }

        public WFInstallationDescription build() {
            return new WFInstallationDescription(modulesPath, Collections.unmodifiableList(featurePacks));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String modulesPath;
    private final List<WFFeaturePackDescription> featurePacks;

    private WFInstallationDescription(String modulesPath, List<WFFeaturePackDescription> featurePacks) {
        this.modulesPath = modulesPath;
        this.featurePacks = featurePacks;
    }

    public String getModulesPath() {
        return modulesPath;
    }

    public List<WFFeaturePackDescription> getFeaturePacks() {
        return featurePacks;
    }
}