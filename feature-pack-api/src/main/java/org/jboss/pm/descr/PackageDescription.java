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

package org.jboss.pm.descr;

import java.io.IOException;
import java.util.List;

/**
 * @author Alexey Loubyansky
 *
 */
public class PackageDescription extends GroupDescription {

    public static class Builder extends GroupDescription.Builder {

        protected Builder() {
            super();
        }

        protected Builder(String name) {
            super(name);
        }

        @Override
        public PackageDescription build() {
            return new PackageDescription(name, dependencies);
        }
    }

    public static Builder packageBuilder() {
        return new Builder();
    }

    public static Builder packageBuilder(String name) {
        return new Builder(name);
    }

    protected PackageDescription(String name, List<String> dependencies) {
        super(name, dependencies);
    }

    @Override
    void logContent(DescrLogger logger) throws IOException {
        logger.print("Package ");
        logger.println(name);
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
