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
public class ConfigPath {

    public static ConfigPath fromString(String str) {
        if(str == null) {
            throw new IllegalArgumentException("str is null");
        }
        if(str.isEmpty()) {
            throw new IllegalArgumentException("str is empty");
        }

        int i = str.indexOf('/');
        if(i == 0) {
            throw new IllegalArgumentException("Path does not follow format name(/name)*");
        }

        if(i < 0) {
            return create(str);
        }

        final List<String> names = new ArrayList<>();
        int c = 0;
        while (i > 0) {
            if (i == str.length() - 1) {
                throw new IllegalArgumentException("Path does not follow format name(/name)*");
            }
            names.add(str.substring(c, i));
            c = i + 1;
            i = str.indexOf('/', c);
        }
        names.add(str.substring(c));
        return create(names.toArray(new String[names.size()]));
    }

    public static ConfigPath create(String... names) {
        return new ConfigPath(names);
    }

    final String[] names;

    private ConfigPath(String[] names) {
        this.names = names;
    }

    public int length() {
        return names.length;
    }

    public ConfigPath resolve(String... names) {
        final String[] tmp = new String[this.names.length + names.length];
        System.arraycopy(this.names, 0, tmp, 0, this.names.length);
        System.arraycopy(names, 0, tmp, this.names.length, names.length);
        return new ConfigPath(tmp);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        if(names.length > 0) {
            buf.append(names[0]);
            if(names.length > 1) {
                for(int i = 1; i < names.length; ++i) {
                    buf.append('/').append(names[i]);
                }
            }
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(names);
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
        ConfigPath other = (ConfigPath) obj;
        if (!Arrays.equals(names, other.names))
            return false;
        return true;
    }
}
