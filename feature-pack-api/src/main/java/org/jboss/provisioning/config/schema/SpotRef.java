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

import java.util.Arrays;

/**
 *
 * @author Alexey Loubyansky
 */
public class SpotRef {

    public static SpotRef create(String spot, boolean nillable, String[] pathParams) {
        return new SpotRef(spot, nillable, pathParams);
    }

    final String spot;
    final boolean nillable;
    final String[] pathParams;

    private SpotRef(String spot, boolean nillable, String[] pathParams) {
        this.spot = spot;
        this.nillable = nillable;
        this.pathParams = pathParams;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(spot)
        .append(" nillable=").append(nillable);
        if(pathParams != null && pathParams.length > 0) {
            buf.append(" path-params=").append(pathParams[0]);
            if (pathParams.length > 1) {
                for (int i = 1; i < pathParams.length; ++i) {
                    buf.append(',').append(pathParams[i]);
                }
            }
        }
        return buf.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (nillable ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(pathParams);
        result = prime * result + ((spot == null) ? 0 : spot.hashCode());
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
        SpotRef other = (SpotRef) obj;
        if (nillable != other.nillable)
            return false;
        if (!Arrays.equals(pathParams, other.pathParams))
            return false;
        if (spot == null) {
            if (other.spot != null)
                return false;
        } else if (!spot.equals(other.spot))
            return false;
        return true;
    }
}
