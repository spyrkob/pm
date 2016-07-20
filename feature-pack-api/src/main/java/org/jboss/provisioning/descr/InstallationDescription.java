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
import java.util.Collection;
import java.util.Map;

import org.jboss.provisioning.GAV;

/**
 *
 * @author Alexey Loubyansky
 */
public class InstallationDescription {

    private final Map<GAV, FeaturePackDescription> featurePacks;

    InstallationDescription(Map<GAV, FeaturePackDescription> featurePacks) {
        assert featurePacks != null : "featurePacks is null";
        this.featurePacks = featurePacks;
    }

    public boolean hasFeaturePacks() {
        return !featurePacks.isEmpty();
    }

    public FeaturePackDescription getFeaturePack(GAV gav) {
        return featurePacks.get(gav);
    }

    public Collection<FeaturePackDescription> getFeaturePacks() {
        return featurePacks.values();
    }

    public String logContent() throws IOException {
        final DescrLogger logger = new DescrLogger();
        logger.println("Installation");
        logger.increaseOffset();
        for(FeaturePackDescription fp : featurePacks.values()) {
            fp.logContent(logger);
        }
        logger.decreaseOffset();
        return logger.toString();
    }
}
