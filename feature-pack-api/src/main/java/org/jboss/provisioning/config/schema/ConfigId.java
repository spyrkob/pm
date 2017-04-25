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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigId {

    public static ConfigId create(ConfigPath path, String... values) {
        return new ConfigId(path, values);
    }

    public static ConfigId fromString(String str) {
        if(str == null) {
            throw new IllegalArgumentException("str is null");
        }
        if(str.isEmpty()) {
            throw new IllegalArgumentException("str is empty");
        }

        int i = str.indexOf('/');
        if(i == 0) {
            throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
        }

        if(i < 0) {
            final int e = str.indexOf('=');
            if(e <= 0 || e == str.length() - 1) {
                throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
            }
            return ConfigId.create(ConfigPath.create(new String[]{str.substring(0, e)}), new String[]{str.substring(e + 1)});
        }

        final List<String> names = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        int c = 0;
        while (i > 0) {
            final int e = str.indexOf('=', c);
            if (e < 0 || e == c || e >= i - 1) {
                throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
            }
            names.add(str.substring(c, e));
            values.add(str.substring(e + 1, i));
            c = i + 1;
            i = str.indexOf('/', c);
        }
        final int e = str.indexOf('=', c);
        if (e < 0 || e == c || e == str.length() - 1) {
            throw new IllegalArgumentException("The string doesn't follow format name=value(/name=value)*");
        }
        names.add(str.substring(c, e));
        values.add(str.substring(e + 1));

        return ConfigId.create(ConfigPath.create(names.toArray(new String[names.size()])),
                values.toArray(new String[values.size()]));
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
        if(values.length > 0) {
            buf.append(path.names[0]).append('=').append(values[0]);
            if(values.length > 1) {
                for(int i = 1; i < values.length; ++i) {
                    buf.append('/').append(path.names[i]).append('=').append(values[i]);
                }
            }
        }
        return buf.toString();
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
