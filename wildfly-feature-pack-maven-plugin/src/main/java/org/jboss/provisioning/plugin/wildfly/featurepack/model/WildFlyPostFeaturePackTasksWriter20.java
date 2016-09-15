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

package org.jboss.provisioning.plugin.wildfly.featurepack.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXMLStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class WildFlyPostFeaturePackTasksWriter20 {

    public static final WildFlyPostFeaturePackTasksWriter20 INSTANCE = new WildFlyPostFeaturePackTasksWriter20();

    private WildFlyPostFeaturePackTasksWriter20() {
    }

    public void write(WildFlyPostFeaturePackTasks featurePackDescription, Path outputFile) throws XMLStreamException, IOException {
        final ElementNode tasksElement = new ElementNode(null, WildFlyPostFeaturePackTasksParser20.Element.TASKS.getLocalName(), WildFlyPostFeaturePackTasksParser20.NAMESPACE_2_0);
        ConfigXMLWriter20.INSTANCE.write(featurePackDescription.getConfig(), tasksElement);
        FilePermissionsXMLWriter20.INSTANCE.write(featurePackDescription.getFilePermissions(), tasksElement);
        try(FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        Files.newBufferedWriter(outputFile)))) {
            writer.writeStartDocument();
            tasksElement.marshall(writer);
            writer.writeEndDocument();
        }
    }
}
