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
import java.util.Map;

import org.jboss.pm.GAV;

/**
 *
 * @author Alexey Loubyansky
 */
public class InstallationDef {

    private final GAV gav;
    private final Map<GAV, FeaturePackDef> featurePacks;

    InstallationDef(GAV gav, Map<GAV, FeaturePackDef> featurePacks) {
        assert gav != null : "GAV is null";
        assert featurePacks != null : "featurePacks is null";
        this.gav = gav;
        this.featurePacks = featurePacks;
    }

    public GAV getGAV() {
        return gav;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public FeaturePackDef getFeaturePack(GAV gav) {
        return featurePacks.get(gav);
    }

    public String logContent() throws IOException {
        final DefLogger logger = new DefLogger();
        logger.println("Installation " + gav);
        logger.increaseOffset();
        for(FeaturePackDef fp : featurePacks.values()) {
            fp.logContent(logger);
        }
        logger.decreaseOffset();
        return logger.toString();
    }
}
