/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.provisioning;

/**
 * GroupId, artifactId, version that are used to identify a feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class Gav implements Comparable<Gav> {

    public static Gav fromString(String str) {

        int i = str.indexOf(':');
        if(i <= 0) {
            throw new IllegalArgumentException("groupId is missing in '" + str + "'");
        }
        final String groupId = str.substring(0, i);
        final String artifactId;
        final String version;
        i = str.indexOf(':', i + 1);
        if(i < 0) {
            artifactId = str.substring(groupId.length() + 1);
            version = null;
        } else {
            artifactId = str.substring(groupId.length() + 1, i);
            version = str.substring(i + 1);
        }
        return new Gav(groupId, artifactId, version);
    }

    public static GaPart toGaPart(String groupId, String artifactId) {
        return new Gav(groupId, artifactId).getGaPart();
    }

    public class GaPart implements Comparable<GaPart> {

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public Gav getGav() {
            return Gav.this;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            if(groupId != null) {
                buf.append(groupId);
            }
            buf.append(':');
            if(artifactId != null) {
                buf.append(artifactId);
            }
            return buf.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
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
            GaPart other = (GaPart) obj;
            if (artifactId == null) {
                if (other.getArtifactId() != null)
                    return false;
            } else if (!artifactId.equals(other.getArtifactId()))
                return false;
            if (groupId == null) {
                if (other.getGroupId() != null)
                    return false;
            } else if (!groupId.equals(other.getGroupId()))
                return false;
            return true;
        }

        @Override
        public int compareTo(GaPart o) {
            if(o == null) {
                return 1;
            }
            int i = groupId.compareTo(o.getGroupId());
            if(i != 0) {
                return i;
            }
            return artifactId.compareTo(o.getArtifactId());
        }
    }

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final GaPart gaPart;

    public Gav(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public Gav(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.gaPart = new GaPart();
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

    public GaPart getGaPart() {
        return gaPart;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        if(groupId != null) {
            buf.append(groupId);
        }
        buf.append(':');
        if(artifactId != null) {
            buf.append(artifactId);
        }
        if(version != null) {
            buf.append(':').append(version);
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
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
        Gav other = (Gav) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
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
    public int compareTo(Gav o) {
        if(o == null) {
            return 1;
        }
        int i = groupId.compareTo(o.getGroupId());
        if(i != 0) {
            return i;
        }
        i = artifactId.compareTo(o.getArtifactId());
        if(i != 0) {
            return i;
        }
        if(version == null) {
            return o.getVersion() == null ? 0 : -1;
        }
        if(o.getVersion() == null) {
            return 1;
        }
        return version.compareTo(o.getVersion());
    }
}
