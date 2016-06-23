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

package org.jboss.pm.wildfly.def;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.pm.def.InstallationDef;
import org.jboss.pm.def.InstallationDefBuilder;
import org.jboss.pm.def.InstallationDefException;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFInstallationDefBuilder {

    public static WFInstallationDefBuilder newInstance() {
        return new WFInstallationDefBuilder();
    }

    private String modulesPath;
    private List<WFFeaturePackDefBuilder> featurePacks = Collections.emptyList();

    private WFInstallationDefBuilder() {
    }

    public void setModulesPath(String modulesPath) {
        if(this.modulesPath != null) {
            throw new IllegalStateException("Modules path has already been set");
        }
        this.modulesPath = modulesPath;
    }

    public void addFeaturePack(WFFeaturePackDefBuilder fpBuilder) {
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonList(fpBuilder);
                break;
            case 1:
                featurePacks = new ArrayList<WFFeaturePackDefBuilder>(featurePacks);
            default:
                featurePacks.add(fpBuilder);
        }
    }

    public String getModulesPath() {
        return modulesPath;
    }

    public InstallationDef build(File installationHome) throws InstallationDefException {

        final DefBuildContext ctx = new DefBuildContext(installationHome, modulesPath);
        final InstallationDefBuilder builder = InstallationDefBuilder.newInstance();
        for(WFFeaturePackDefBuilder fpBuilder : featurePacks) {
            builder.addFeaturePack(fpBuilder.build(ctx));
        }
        return builder.build();
    }
}