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

package org.jboss.provisioning.feature;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class SpecId {

    static final char SEPARATOR = '#';

    public static SpecId fromString(String str) throws ProvisioningDescriptionException {
        final int length = str.length();
        if(length == 0) {
            throw new ProvisioningDescriptionException("The string does not follow expected format fp_dep_name#spec_name");
        }

        int i = 0;
        char c = str.charAt(i++);
        final StringBuilder buf = new StringBuilder(length);
        while(c != SEPARATOR) {
            buf.append(c);
            if(i == length) {
                return new SpecId(null, buf.toString());
            }
            c = str.charAt(i++);
        }

        if(i == 1 || i == length) {
            throw new ProvisioningDescriptionException("The string does not follow expected format fp_dep_name#spec_name");
        }

        final String fpDepName = buf.toString();
        buf.setLength(0);
        while(i < length) {
            c = str.charAt(i++);
            if(c == SEPARATOR) {
                throw new ProvisioningDescriptionException("The string does not follow expected format fp_dep_name#spec_name");
            }
            buf.append(c);
        }
        return new SpecId(fpDepName, buf.toString());
    }

    public static SpecId create(String name) {
        return new SpecId(null, name);
    }

    public static SpecId create(String fpDepName, String name) {
        return new SpecId(fpDepName, name);
    }

    final String fpDepName;
    final String name;

    private SpecId(String fpDepName, String name) {
        this.fpDepName = fpDepName;
        this.name = name;
    }

    public String getFpDepName() {
        return fpDepName;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fpDepName == null) ? 0 : fpDepName.hashCode());
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
        SpecId other = (SpecId) obj;
        if (fpDepName == null) {
            if (other.fpDepName != null)
                return false;
        } else if (!fpDepName.equals(other.fpDepName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return fpDepName == null ? name : fpDepName + SEPARATOR + name;
    }
}
