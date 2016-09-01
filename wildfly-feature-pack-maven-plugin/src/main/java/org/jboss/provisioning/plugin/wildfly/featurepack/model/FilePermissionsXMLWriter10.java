/*
 * Copyright 2014 Red Hat, Inc.
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



import java.util.List;

import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;

import static org.jboss.provisioning.plugin.wildfly.featurepack.model.FilePermissionsModelParser10.Attribute;
import static org.jboss.provisioning.plugin.wildfly.featurepack.model.FilePermissionsModelParser10.Element;

/**
 * Writes a list of file permissions as XML.
 *
 * @author Eduardo Martins
 */
public class FilePermissionsXMLWriter10 {

    public static final FilePermissionsXMLWriter10 INSTANCE = new FilePermissionsXMLWriter10();

    private FilePermissionsXMLWriter10() {
    }

    public void write(List<FilePermission> filePermissions, ElementNode parentElementNode) {
        if (!filePermissions.isEmpty()) {
            final ElementNode filePermissionsElementNode = new ElementNode(parentElementNode, FilePermissionsModelParser10.ELEMENT_LOCAL_NAME);
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
                FileFilterXMLWriter10.INSTANCE.write(fileFilter, filePermissionElementNode);
            }
        }
        filePermissionElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(filePermission.getValue()));
        filePermissionsElementNode.addChild(filePermissionElementNode);
    }

}
