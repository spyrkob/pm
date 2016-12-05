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
package org.jboss.provisioning.plugin.wildfly.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

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

        final List<String> mkDirs = featurePackDescription.getMkDirs();
        if (!mkDirs.isEmpty()) {
            final ElementNode mkdirsElement = new ElementNode(tasksElement,
                    WildFlyPostFeaturePackTasksParser20.Element.MKDIRS.getLocalName(),
                    WildFlyPostFeaturePackTasksParser20.NAMESPACE_2_0);
            tasksElement.addChild(mkdirsElement);
            for (String mkdir : mkDirs) {
                final ElementNode dirElement = new ElementNode(mkdirsElement,
                        WildFlyPostFeaturePackTasksParser20.Element.DIR.getLocalName(),
                        WildFlyPostFeaturePackTasksParser20.NAMESPACE_2_0);
                mkdirsElement.addChild(dirElement);
                dirElement.addAttribute(WildFlyPostFeaturePackTasksParser20.Attribute.NAME.getLocalName(), new AttributeValue(
                        mkdir));
            }
        }

        FilePermissionsXMLWriter20.INSTANCE.write(featurePackDescription.getFilePermissions(), tasksElement);
        try(FormattingXmlStreamWriter writer = new FormattingXmlStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        Files.newBufferedWriter(outputFile)))) {
            writer.writeStartDocument();
            tasksElement.marshall(writer);
            writer.writeEndDocument();
        }
    }
}
