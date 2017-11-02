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

package org.jboss.provisioning.spec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.util.PmCollections;

/**
 * @author Alexey Loubyansky
 *
 */
public class CapabilitySpec {

    public static CapabilitySpec fromString(String str) throws ProvisioningDescriptionException {
        return fromString(str, false);
    }

    public static CapabilitySpec fromString(String str, boolean optional) throws ProvisioningDescriptionException {
        if(str == null) {
            throw new ProvisioningDescriptionException("str is null");
        }
        if(str.isEmpty()) {
            throw new ProvisioningDescriptionException("str is empty");
        }

        List<String> parts = Collections.emptyList();
        List<Boolean> partTypes = Collections.emptyList();
        int strI = 0;
        final StringBuilder buf = new StringBuilder();
        boolean staticPart = true;
        while(strI < str.length()) {
            final char ch = str.charAt(strI++);
            switch(ch) {
                case '.': {
                    if(buf.length() == 0) {
                        formatError(str);
                    }
                    if(staticPart) {
                        if(buf.charAt(buf.length() - 1) == '.') {
                            formatError(str);
                        }
                        if(strI < str.length() && str.charAt(strI) != '$') {
                            buf.append('.');
                            break;
                        }
                    }
                    parts = PmCollections.add(parts, buf.toString());
                    partTypes = PmCollections.add(partTypes, staticPart);
                    buf.setLength(0);
                    staticPart = true;
                    break;
                }
                case '$': {
                    if(strI > 1 && str.charAt(strI - 2) != '.') {
                        formatError(str);
                    }
                    staticPart = false;
                    break;
                } default: {
                    if(Character.isWhitespace(ch)) {
                        throw new ProvisioningDescriptionException("Whitespaces are not allowed in a capability expression '" + str + "'");
                    }
                    buf.append(ch);
                }
            }
        }
        if(buf.length() == 0) {
            formatError(str);
        }
        parts = PmCollections.add(parts, buf.toString());
        partTypes = PmCollections.add(partTypes, staticPart);
        return new CapabilitySpec(parts, partTypes, optional);
    }

    private static void formatError(String str) throws ProvisioningDescriptionException {
        throw new ProvisioningDescriptionException("Capability '" + str + "' doesn't follow format [$]part[.[$]part]");
    }

    private final String[] parts;
    private final Boolean[] partTypes; // true - static part, false - param part
    private final boolean optional;

    private CapabilitySpec(List<String> parts, List<Boolean> partTypes, boolean optional) throws ProvisioningDescriptionException {
        this.parts = parts.toArray(new String[parts.size()]);
        this.partTypes = partTypes.toArray(new Boolean[partTypes.size()]);
        this.optional = optional;
        if(optional && isStatic()) {
            throw new ProvisioningDescriptionException("Static capability cannot be optional: " + toString());
        }
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isStatic() {
        return parts.length == 1 && partTypes[0];
    }

    public String resolve(Map<String, String> params) throws ProvisioningException {
        if(isStatic()) {
            return toString();
        }
        final StringBuilder buf = new StringBuilder();
        if(partTypes[0]) {
            buf.append(parts[0]);
        } else {
            final String value = params.get(parts[0]);
            if (value == null) {
                if (optional) {
                    return null;
                }
                throw new ProvisioningException(Errors.capabilityMissingParameter(this, parts[0]));
            }
            if (value.trim().isEmpty()) {
                throw new ProvisioningException(Errors.capabilityMissingParameter(this, parts[0]));
            }
            buf.append(value);
        }
        for(int i = 1; i < parts.length; ++i) {
            buf.append('.');
            if(partTypes[i]) {
                buf.append(parts[i]);
            } else {
                final String value = params.get(parts[i]);
                if (value == null) {
                    if (optional) {
                        return null;
                    }
                    throw new ProvisioningException(Errors.capabilityMissingParameter(this, parts[i]));
                }
                if (value.trim().isEmpty()) {
                    throw new ProvisioningException(Errors.capabilityMissingParameter(this, parts[i]));
                }
                buf.append(value);
            }
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (optional ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(partTypes);
        result = prime * result + Arrays.hashCode(parts);
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
        CapabilitySpec other = (CapabilitySpec) obj;
        if (optional != other.optional)
            return false;
        if (!Arrays.equals(partTypes, other.partTypes))
            return false;
        if (!Arrays.equals(parts, other.parts))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if(parts.length == 1 && partTypes[0]) {
            return parts[0];
        }
        final StringBuilder buf = new StringBuilder();
        if(!partTypes[0] ) {
            buf.append('$');
        }
        buf.append(parts[0]);
        for(int i = 1; i < parts.length; ++i) {
            buf.append('.');
            if(!partTypes[i]) {
                buf.append('$');
            }
            buf.append(parts[i]);
        }
        return buf.toString();
    }
}
