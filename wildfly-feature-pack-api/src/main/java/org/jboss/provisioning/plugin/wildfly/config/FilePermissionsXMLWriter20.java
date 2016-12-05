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



import java.util.List;

import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;

import static org.jboss.provisioning.plugin.wildfly.config.FilePermissionsModelParser20.Attribute;
import static org.jboss.provisioning.plugin.wildfly.config.FilePermissionsModelParser20.Element;

/**
 * Writes a list of file permissions as XML.
 *
 * @author Eduardo Martins
 * @author Alexey Loubyansky
 */
public class FilePermissionsXMLWriter20 {

    public static final FilePermissionsXMLWriter20 INSTANCE = new FilePermissionsXMLWriter20();

    private FilePermissionsXMLWriter20() {
    }

    public void write(List<FilePermission> filePermissions, ElementNode parentElementNode) {
        if (!filePermissions.isEmpty()) {
            final ElementNode filePermissionsElementNode = new ElementNode(parentElementNode, FilePermissionsModelParser20.ELEMENT_LOCAL_NAME);
            for (FilePermission filePermission : filePermissions) {
                writeFilePermission(filePermission, filePermissionsElementNode);
            }
            parentElementNode.addChild(filePermissionsElementNode);
        }
    }

    protected void writeFilePermission(FilePermission filePermission, ElementNode filePermissionsElementNode) {
        final ElementNode filePermissionElementNode = new ElementNode(filePermissionsElementNode, Element.PERMISSION.getLocalName());
        if (!filePermission.getFilters().isEmpty()) {
            for (FileFilter fileFilter : filePermission.getFilters()) {
                FileFilterXmlWriter20.INSTANCE.write(fileFilter, filePermissionElementNode);
            }
        }
        filePermissionElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(filePermission.getValue()));
        filePermissionsElementNode.addChild(filePermissionElementNode);
    }

}
