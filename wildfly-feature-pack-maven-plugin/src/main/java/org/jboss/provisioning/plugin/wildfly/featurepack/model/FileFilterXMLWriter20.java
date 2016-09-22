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
package org.jboss.provisioning.plugin.wildfly.featurepack.model;


import static org.jboss.provisioning.plugin.wildfly.featurepack.model.FileFilterModelParser20.Attribute;

import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 * Writes a file filter as XML element.
 *
 * @author Eduardo Martins
 * @author Alexey Loubyansky
 */
public class FileFilterXMLWriter20 {

    public static final FileFilterXMLWriter20 INSTANCE = new FileFilterXMLWriter20();

    private FileFilterXMLWriter20() {
    }

    public void write(FileFilter fileFilter, ElementNode parentElementNode) {
        final ElementNode fileFilterElementNode = new ElementNode(parentElementNode, FileFilterModelParser20.ELEMENT_LOCAL_NAME);
        fileFilterElementNode.addAttribute(Attribute.PATTERN.getLocalName(), new AttributeValue(fileFilter.getPattern()));
        fileFilterElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(fileFilter.isInclude())));
        parentElementNode.addChild(fileFilterElementNode);
    }

}
