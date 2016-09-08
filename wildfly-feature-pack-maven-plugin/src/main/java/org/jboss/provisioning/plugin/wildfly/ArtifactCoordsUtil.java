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

package org.jboss.provisioning.plugin.wildfly;

import java.util.Arrays;

import org.jboss.provisioning.ArtifactCoords;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArtifactCoordsUtil {

    public static String toJBossModules(ArtifactCoords coords, boolean includeVersion) {
        final StringBuilder buf = new StringBuilder();
        buf.append(coords.getGroupId()).append(':').append(coords.getArtifactId());
        if(coords.getClassifier() != null && !coords.getClassifier().isEmpty()) {
            buf.append(':');
            if(includeVersion && coords.getVersion() != null) {
                buf.append(coords.getVersion());
            }
            buf.append(':').append(coords.getClassifier());
        } else if(includeVersion && coords.getVersion() != null) {
            buf.append(':').append(coords.getVersion());
        }
        return buf.toString();
    }

    public static ArtifactCoords fromJBossModules(String str, String extension) {
        final String[] parts = str.split(":");
        if(parts.length < 2) {
            throw new IllegalArgumentException("Unexpected artifact coordinates format: " + str);
        }
        final String groupId = parts[0];
        final String artifactId = parts[1];
        String version = null;
        String classifier = null;
        if(parts.length > 2) {
            if(!parts[2].isEmpty()) {
                version = parts[2];
            }
            if(parts.length > 3 && !parts[3].isEmpty()) {
                classifier = parts[3];
                if(parts.length > 4) {
                    throw new IllegalArgumentException("Unexpected artifact coordinates format: " + str);
                }
            }
        }
        return new ArtifactCoords(groupId, artifactId, version, classifier, extension);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.asList("group:artifact:version".split(":")));
    }
}
