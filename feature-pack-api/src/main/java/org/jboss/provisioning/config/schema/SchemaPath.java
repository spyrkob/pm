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
public class SchemaPath {

    public static SchemaPath create(String... spot) {
        return new SchemaPath(spot);
    }

    final String[] spots;

    private SchemaPath(String[] spots) {
        this.spots = spots;
    }

    public String getName(int i) {
        return spots[i];
    }

    public String getSpotName() {
        return spots[spots.length - 1];
    }

    public boolean hasParent() {
        return spots.length > 1;
    }

    public SchemaPath getParent() {
        return new SchemaPath(Arrays.copyOf(spots, spots.length - 1));
    }

    public int length() {
        return spots.length;
    }

    public SchemaPath resolve(String... spot) {
        final String[] tmp = new String[spots.length + spot.length];
        System.arraycopy(spots, 0, tmp, 0, spots.length);
        System.arraycopy(spot, 0, tmp, spots.length, spot.length);
        return new SchemaPath(tmp);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(spots.length > 0) {
            buf.append(spots[0]);
            if(spots.length > 1) {
                for(int i = 1; i < spots.length; ++i) {
                    buf.append(',').append(spots[i]);
                }
            }
        }
        return buf.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(spots);
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
        SchemaPath other = (SchemaPath) obj;
        if (!Arrays.equals(spots, other.spots))
            return false;
        return true;
    }
}
