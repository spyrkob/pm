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
import java.util.Arrays;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.xml.ProvisioningXmlParser10.Attribute;
import org.jboss.provisioning.xml.ProvisioningXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningXmlWriter extends BaseXmlWriter {

    public static final ProvisioningXmlWriter INSTANCE = new ProvisioningXmlWriter();

    private ProvisioningXmlWriter() {
    }

    public void write(ProvisionedInstallationDescription installDescr, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = addElement(null, Element.INSTALLATION);

        if (installDescr.hasFeaturePacks()) {
            final Gav[] gavs = installDescr.getFeaturePackGavs().toArray(new Gav[0]);
            Arrays.sort(gavs);
            for (Gav gav : gavs) {
                final ElementNode fp = addElement(pkg, Element.FEATURE_PACK);
                writeFeaturePack(fp, installDescr.getFeaturePack(gav));
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

    private void writeFeaturePack(ElementNode fp, ProvisionedFeaturePackDescription featurePack) {
        addAttribute(fp, Attribute.GROUP_ID, featurePack.getGav().getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, featurePack.getGav().getArtifactId());
        if (featurePack.getGav().getVersion() != null) {
            addAttribute(fp, Attribute.VERSION, featurePack.getGav().getVersion());
        }

        if (featurePack.hasExcludedPackages()) {
            final ElementNode excludes = addElement(fp, Element.EXCLUDES);
            writePackageList(excludes, featurePack.getExcludedPackages());
        }

        if (featurePack.hasIncludedPackages()) {
            final ElementNode excludes = addElement(fp, Element.INCLUDES);
            writePackageList(excludes, featurePack.getIncludedPackages());
        }
    }

    private void writePackageList(ElementNode excludes, Set<String> packageNames) {
        for (String packageName : packageNames) {
            final ElementNode pkg = addElement(excludes, Element.PACKAGE);
            addAttribute(pkg, Attribute.NAME, packageName);
        }
    }
}
