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

package org.jboss.pm.def;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Loubyansky
 *
 */
public class PackageDef extends GroupDef {

    public static class PackageDefBuilder extends GroupDef.GroupDefBuilder {

        private List<String> contentPaths = Collections.emptyList();

        protected PackageDefBuilder(String name) {
            super(name);
        }

        public PackageDefBuilder addContentPath(String contentPath) {
            assert contentPath != null : "contentPath is null";
            switch(contentPaths.size()) {
                case 0:
                    contentPaths = Collections.singletonList(contentPath);
                    break;
                case 1:
                    contentPaths = new ArrayList<String>(contentPaths);
                default:
                    contentPaths.add(contentPath);
            }
            return this;
        }

        @Override
        public PackageDef build() {
            return new PackageDef(name, dependencies, Collections.unmodifiableList(contentPaths));
        }
    }

    public static PackageDefBuilder packageBuilder(String name) {
        return new PackageDefBuilder(name);
    }

    private final List<String> contentPaths;

    protected PackageDef(String name, List<String> dependencies, List<String> contentPaths) {
        super(name, dependencies);
        assert contentPaths != null : "contentPaths is null";
        this.contentPaths = contentPaths;
    }

    @Override
    public boolean hasContent() {
        return !contentPaths.isEmpty();
    }

    @Override
    public List<String> getContentPaths() {
        return contentPaths;
    }

    @Override
    void logContent(DefLogger logger) throws IOException {
        logger.print("Package ");
        logger.println(name);
        if(!contentPaths.isEmpty()) {
            logger.increaseOffset();
            logger.println("Content");
            logger.increaseOffset();
            for(String path : contentPaths) {
                logger.println(path);
            }
            logger.decreaseOffset();
            logger.decreaseOffset();
        }
        if(!dependencies.isEmpty()) {
            logger.increaseOffset();
            logger.println("Dependencies");
            logger.increaseOffset();
            for(String dependency : dependencies) {
                logger.println(dependency);
            }
            logger.decreaseOffset();
            logger.decreaseOffset();
        }
    }
}
