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
package org.jboss.provisioning.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.ResolvedFeaturePackDescription;
import org.jboss.provisioning.descr.ResolvedInstallationDescription;
import org.jboss.provisioning.xml.ProvisionedInstallationXmlParser10.Attribute;
import org.jboss.provisioning.xml.ProvisionedInstallationXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedInstallationXmlWriter extends BaseXmlWriter {

    private static final ProvisionedInstallationXmlWriter INSTANCE = new ProvisionedInstallationXmlWriter();

    public static ProvisionedInstallationXmlWriter getInstance() {
        return INSTANCE;
    }

    private ProvisionedInstallationXmlWriter() {
    }

    public void write(ResolvedInstallationDescription installDescr, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = addElement(null, Element.INSTALLATION);

        if (installDescr.hasFeaturePacks()) {
            for(ResolvedFeaturePackDescription fp : installDescr.getFeaturePacks()) {
                final ElementNode fpElement = addElement(pkg, Element.FEATURE_PACK);
                writeFeaturePack(fpElement, fp);
            }
        }

        ensureParentDir(outputFile);
        try (FormattingXmlStreamWriter writer = new FormattingXmlStreamWriter(XMLOutputFactory.newInstance()
                .createXMLStreamWriter(Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)))) {
            writer.writeStartDocument();
            pkg.marshall(writer);
            writer.writeEndDocument();
        }
    }

    private void writeFeaturePack(ElementNode fp, ResolvedFeaturePackDescription featurePack) {
        addAttribute(fp, Attribute.GROUP_ID, featurePack.getGav().getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, featurePack.getGav().getArtifactId());
        if (featurePack.getGav().getVersion() != null) {
            addAttribute(fp, Attribute.VERSION, featurePack.getGav().getVersion());
        }

        if (featurePack.hasPackages()) {
            final ElementNode packages = addElement(fp, Element.PACKAGES);
            for (String pkgName : featurePack.getPackageNames()) {
                final ElementNode exclude = addElement(packages, Element.PACKAGE);
                addAttribute(exclude, Attribute.NAME, pkgName);
            }
        }
    }
}
