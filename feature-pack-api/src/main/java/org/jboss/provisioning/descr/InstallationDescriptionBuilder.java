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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.provisioning.GAV;

/**
 * This class collects feature packs descriptions and produces an installation
 * description.
 *
 * @author Alexey Loubyansky
 */
public class InstallationDescriptionBuilder {

    public static InstallationDescriptionBuilder newInstance() {
        return new InstallationDescriptionBuilder();
    }

    private Map<GAV, FeaturePackDescription> featurePacks = Collections.emptyMap();
    private Map<String, Map<String, String>> gavs = Collections.emptyMap();

    InstallationDescriptionBuilder() {
    }

    public InstallationDescriptionBuilder addFeaturePack(FeaturePackDescription fp) throws InstallationDescriptionException {
        return addFeaturePack(fp, true);
    }

    public InstallationDescriptionBuilder addFeaturePack(FeaturePackDescription fp, boolean addLast) throws InstallationDescriptionException {
        assert fp != null : "fp is null";
        final GAV fpGav = fp.getGAV();
        checkGav(fpGav);
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonMap(fpGav, fp);
                break;
            case 1:
                featurePacks = new LinkedHashMap<GAV, FeaturePackDescription>(featurePacks);
            default:
                if(addLast && featurePacks.containsKey(fpGav)) {
                    featurePacks.remove(fpGav);
                }
                featurePacks.put(fpGav, fp);
        }
        return this;
    }

    private void checkGav(final GAV fpGav) throws InstallationDescriptionException {
        Map<String, String> group = gavs.get(fpGav.getGroupId());
        if(group == null) {
            final Map<String, String> result = Collections.singletonMap(fpGav.getArtifactId(), fpGav.getVersion());
            switch(gavs.size()) {
                case 0:
                    gavs = Collections.singletonMap(fpGav.getGroupId(), result);
                    break;
                case 1:
                    gavs = new HashMap<String, Map<String, String>>(gavs);
                default:
                    gavs.put(fpGav.getGroupId(), result);
            }
        } else if (group.containsKey(fpGav.getArtifactId())) {
            if (!group.get(fpGav.getArtifactId()).equals(fpGav.getVersion())) {
                throw new InstallationDescriptionException("The installation requires two versions of artifact "
                        + fpGav.getGroupId() + ':' + fpGav.getArtifactId() + ": " + fpGav.getVersion() + " and "
                        + group.get(fpGav.getArtifactId()));
            }
        } else {
            if(group.size() == 1) {
                group = new HashMap<String, String>(group);
                if(gavs.size() == 1) {
                    gavs = Collections.singletonMap(fpGav.getGroupId(), group);
                } else {
                    gavs.put(fpGav.getGroupId(), group);
                }
            }
            group.put(fpGav.getArtifactId(), fpGav.getVersion());
        }
    }

    public InstallationDescription build() throws InstallationDescriptionException {
        return new InstallationDescription(Collections.unmodifiableMap(featurePacks));
    }
}
