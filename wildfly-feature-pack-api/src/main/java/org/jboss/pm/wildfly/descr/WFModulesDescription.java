/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.pm.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFModulesDescription {

    public static class Builder {

        private String relativeDir;
        private List<String> names = Collections.emptyList();

        Builder() {
        }

        public void setRelativeDir(String relativeDir) {
            this.relativeDir = relativeDir;
        }

        public void addModule(String name) {
            switch(names.size()) {
                case 0:
                    names = Collections.singletonList(name);
                    break;
                case 1:
                    names = new ArrayList<String>(names);
                default:
                    names.add(name);
            }
        }

        public WFModulesDescription build() {
            return new WFModulesDescription(relativeDir, Collections.unmodifiableList(names));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String relativeDir;
    private final List<String> names;

    public WFModulesDescription(String relativeDir, List<String> names) {
        this.relativeDir = relativeDir;
        this.names = names;
    }

    public String getRelativeDir() {
        return relativeDir;
    }

    public List<String> getNames() {
        return names;
    }
}
