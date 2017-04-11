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
public class XmlFeatureOccurence {

    public static XmlFeatureOccurence create(String spot, String specName) {
        return new XmlFeatureOccurence(spot, specName, true, false);
    }

    public static XmlFeatureOccurence create(String spot, String specName, boolean required) {
        return new XmlFeatureOccurence(spot, specName, required, false);
    }

    public static XmlFeatureOccurence create(String spot, String specName, boolean required, boolean maxOccursUnbounded) {
        return new XmlFeatureOccurence(spot, specName, required, maxOccursUnbounded);
    }

    final String spot;
    final String specName;
    final boolean required;
    final boolean maxOccursUnbounded;

    private XmlFeatureOccurence(String configId, String specName, boolean required, boolean maxOccursUnbounded) {
        this.spot = configId;
        this.specName = specName;
        this.required = required;
        this.maxOccursUnbounded = maxOccursUnbounded;
    }

    public String getSpot() {
        return spot;
    }

    public String getSpecName() {
        return specName;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isMaxOccursUnbounded() {
        return maxOccursUnbounded;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (maxOccursUnbounded ? 1231 : 1237);
        result = prime * result + ((spot == null) ? 0 : spot.hashCode());
        result = prime * result + (required ? 1231 : 1237);
        result = prime * result + ((specName == null) ? 0 : specName.hashCode());
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
        XmlFeatureOccurence other = (XmlFeatureOccurence) obj;
        if (maxOccursUnbounded != other.maxOccursUnbounded)
            return false;
        if (spot == null) {
            if (other.spot != null)
                return false;
        } else if (!spot.equals(other.spot))
            return false;
        if (required != other.required)
            return false;
        if (specName == null) {
            if (other.specName != null)
                return false;
        } else if (!specName.equals(other.specName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[spot=" + spot + ", specName=" + specName + ", required=" + required + ", maxOccursUnbounded=" + maxOccursUnbounded + "]";
    }
}