/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.descr;

/**
 * Describes a dependency of one feature-pack on another.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyDescription {

    public static FeaturePackDependencyDescription create(ProvisionedFeaturePackDescription descr) {
        return create(null, descr);
    }

    public static FeaturePackDependencyDescription create(String name, ProvisionedFeaturePackDescription descr) {
        return new FeaturePackDependencyDescription(name, descr);
    }

    private final String name;
    private final ProvisionedFeaturePackDescription descr;

    private FeaturePackDependencyDescription(String name, ProvisionedFeaturePackDescription descr) {
        this.name = name;
        this.descr = descr;
    }

    /**
     * Name of the dependency, which is optional, can be null if the name was not provided
     * by the author of the feature-pack.
     * The name can be used in feature-pack package descriptions to express feature-pack package
     * dependencies on the packages from the feature-pack dependency identified by the name.
     *
     * @return  name of the dependency or null if the dependency was not given a name
     */
    public String getName() {
        return name;
    }

    /**
     * Description of the feature-pack dependency.
     *
     * @return  dependency description
     */
    public ProvisionedFeaturePackDescription getTarget() {
        return descr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((descr == null) ? 0 : descr.hashCode());
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
        FeaturePackDependencyDescription other = (FeaturePackDependencyDescription) obj;
        if (descr == null) {
            if (other.descr != null)
                return false;
        } else if (!descr.equals(other.descr))
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
        final StringBuilder buf = new StringBuilder();
        buf.append("[dependency ");
        if(name != null) {
            buf.append(name).append(' ');
        }
        return buf.append(descr).append(']').toString();
    }
}
