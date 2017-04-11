/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.config.schema;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigRef {

    public static ConfigRef create(String spot, String featureId) {
        return new ConfigRef(spot, featureId);
    }

    final String spot;
    final String featureId;

    private ConfigRef(String spot, String featureId) {
        this.spot = spot;
        this.featureId = featureId;
    }

    public String getSpot() {
        return spot;
    }

    public String getFeatureId() {
        return featureId;
    }

    @Override
    public String toString() {
        return spot + "#" + featureId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((spot == null) ? 0 : spot.hashCode());
        result = prime * result + ((featureId == null) ? 0 : featureId.hashCode());
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
        ConfigRef other = (ConfigRef) obj;
        if (spot == null) {
            if (other.spot != null)
                return false;
        } else if (!spot.equals(other.spot))
            return false;
        if (featureId == null) {
            if (other.featureId != null)
                return false;
        } else if (!featureId.equals(other.featureId))
            return false;
        return true;
    }
}
