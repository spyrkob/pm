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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.pm.GAV;
import org.jboss.pm.def.FeaturePackDef;
import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.def.PackageDef;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFFeaturePackDefBuilder {

    private final WFInstallationDefBuilder wfBuilder;
    private List<WFPackageDefBuilder> packages = Collections.emptyList();
    private final String groupId;
    private String artifactId;
    private String version;

    private FeaturePackDef.FeaturePackDefBuilder fpBuilder;


    public WFFeaturePackDefBuilder(String groupId, String artifactId, String version, WFInstallationDefBuilder wfBuilder) {
        assert groupId != null : "groupId is null";
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.wfBuilder = wfBuilder;
    }

    public void addPackage(WFPackageDefBuilder pkgBuilder) {
        switch(packages.size()) {
            case 0:
                packages = Collections.singletonList(pkgBuilder);
                break;
            case 1:
                packages = new ArrayList<WFPackageDefBuilder>(packages);
            default:
                packages.add(pkgBuilder);
        }
    }

    public WFInstallationDefBuilder getInstallationBuilder() {
        return wfBuilder;
    }

    public FeaturePackDef build(DefBuildContext ctx) throws InstallationDefException {
        fpBuilder = FeaturePackDef.builder();
        ctx.fpBuilder = this;
        try {
            for(WFPackageDefBuilder pkgBuilder : packages) {
                fpBuilder.addGroup(pkgBuilder.build(ctx));
            }
            return fpBuilder.setGAV(new GAV(groupId, artifactId, version)).build();
        } finally {
            fpBuilder = null;
            ctx.fpBuilder = null;
        }
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    String getArtifactId() {
        return artifactId;
    }

    void setVersion(String version) {
        this.version = version;
    }

    void addModulePackage(PackageDef module) {
        fpBuilder.addGroup(module);
    }
}
