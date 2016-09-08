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

package org.jboss.provisioning;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArtifactCoords implements Comparable<ArtifactCoords> {

    private static final Pattern COORDS_PATTERN = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

    public static ArtifactCoords fromGAV(GAV gav) {
        return fromGAV(gav, "zip");
    }

    public static ArtifactCoords fromGAV(GAV gav, String extension) {
        return new ArtifactCoords(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), null, extension);
    }

    public static ArtifactCoords fromString(String str) {
        return new ArtifactCoords(str);
    }

    private static String get(String value, String defaultValue) {
        return (value == null || value.length() <= 0) ? defaultValue : value;
    }

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String extension;

    private ArtifactCoords(String str) {
        final Matcher m = COORDS_PATTERN.matcher(str);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad artifact coordinates " + str
                    + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
        }
        groupId = m.group(1);
        artifactId = m.group(2);
        extension = get(m.group(4), "jar");
        classifier = get(m.group(6), "");
        version = m.group(7);
    }

    public ArtifactCoords(String groupId, String artifactId, String version, String classifier, String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = get(classifier, "");
        this.extension = get(extension, "jar");
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactCoords other = (ArtifactCoords) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (extension == null) {
            if (other.extension != null)
                return false;
        } else if (!extension.equals(other.extension))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public int compareTo(ArtifactCoords o) {
        // compare groupIds
        int result = groupId.compareTo(o.groupId);
        if (result != 0) {
            return result;
        }

        // groupIds are the same, compare artifactIds
        result = artifactId.compareTo(o.artifactId);
        if (result != 0) {
            return result;
        }

        // artifactIds are the same, compare classifiers
        if (classifier != null) {
            if (o.classifier == null) {
                result = 1;
            } else {
                result = classifier.compareTo(o.classifier);
            }
        } else {
            if (o.classifier != null) {
                result = -1;
            }
        }
        if (result != 0) {
            return result;
        }

        // classifiers are the same, compare extensions
        if (extension != null) {
            if (o.extension == null) {
                result = 1;
            } else {
                result = extension.compareTo(o.extension);
            }
        } else {
            if (o.extension != null) {
                result = -1;
            }
        }
        if (result != 0) {
            return result;
        }

        if (version != null) {
            if (o.version == null) {
                result = 1;
            } else {
                result = version.compareTo(o.version);
            }
        } else {
            if (o.version != null) {
                result = -1;
            }
        }

        return result;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        if(!classifier.isEmpty()) {
            buf.append(':');
            if(extension != null) {
                buf.append(extension);
            }
            buf.append(':').append(classifier);
        }
        if(version != null) {
            buf.append(':').append(version);
        }
        return buf.toString();
    }
}
