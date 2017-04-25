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
public class ConfigId {

    public static ConfigId create(ConfigPath path, String... values) {
        return new ConfigId(path, values);
    }

    final ConfigPath path;
    final String[] values;

    private ConfigId(ConfigPath path, String[] values) {
        if(path.length() != values.length) {
            throw new IllegalArgumentException("Path length does not match values length");
        }
        this.path = path;
        this.values = values;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(values.length > 0) {
            buf.append(path.names[0]).append('=').append(values[0]);
            if(values.length > 1) {
                for(int i = 1; i < values.length; ++i) {
                    buf.append(',').append(path.names[i]).append('=').append(values[i]);
                }
            }
        }
        return buf.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(values);
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        ConfigId other = (ConfigId) obj;
        if (!Arrays.equals(values, other.values))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }
}
