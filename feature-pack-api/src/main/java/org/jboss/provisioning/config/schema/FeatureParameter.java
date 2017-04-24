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
public class FeatureParameter {

    public static FeatureParameter create(String name) {
        return new FeatureParameter(name, false, null);
    }

    public static FeatureParameter create(String name, String value) {
        return new FeatureParameter(name, false, value);
    }

    public static FeatureParameter createId(String name) {
        return new FeatureParameter(name, true, null);
    }

    public static FeatureParameter create(String name, boolean id, String defaultValue) {
        return new FeatureParameter(name, id, defaultValue);
    }

    final String name;
    final boolean id;
    final String defaultValue;

    private FeatureParameter(String name, boolean id, String defaultValue) {
        this.name = name;
        this.id = id;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public boolean isId() {
        return id;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result + (id ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        FeatureParameter other = (FeatureParameter) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null)
                return false;
        } else if (!defaultValue.equals(other.defaultValue))
            return false;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
