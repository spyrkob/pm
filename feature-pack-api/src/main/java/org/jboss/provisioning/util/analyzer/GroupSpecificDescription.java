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

package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.util.DescrFormatter;


/**
 * Describes what is different in a given group comparing to another group.
 *
 * @author Alexey Loubyansky
 */
public class GroupSpecificDescription {

    static class Builder {

        private final String name;
        private List<String> dependencies = Collections.emptyList();
        private boolean contentDifferent;

        private Builder(String name) {
            this.name = name;
        }

        Builder addDependency(String dependencyName) {
            assert dependencyName != null : "dependency is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependencyName);
                    break;
                case 1:
                    dependencies = new ArrayList<String>(dependencies);
                default:
                    dependencies.add(dependencyName);
            }
            return this;
        }

        Builder setContentDifferent(boolean contentDifferent) {
            this.contentDifferent = contentDifferent;
            return this;
        }

        GroupSpecificDescription build() {
            return new GroupSpecificDescription(name, Collections.unmodifiableList(dependencies), contentDifferent);
        }
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    private final String name;
    private final List<String> dependencies;
    private final boolean contentDifferent;

    public GroupSpecificDescription(String name, List<String> dependencies, boolean contentDifferent) {
        super();
        this.name = name;
        this.dependencies = dependencies;
        this.contentDifferent = contentDifferent;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isContentDifferent() {
        return contentDifferent;
    }

    public String logContent() throws IOException {
        final DescrFormatter out = new DescrFormatter();
        logContent(out);
        return out.toString();
    }

    void logContent(DescrFormatter out) throws IOException {
        out.print("GroupSpecificDescription ").println(name);
        if(!dependencies.isEmpty()) {
            out.increaseOffset();
            out.println("Dependencies");
            out.increaseOffset();
            for(String dependency : dependencies) {
                out.println(dependency);
            }
            out.decreaseOffset();
            out.decreaseOffset();
        }
    }
}
