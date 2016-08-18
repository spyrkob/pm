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

package org.jboss.provisioning.xml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jboss.provisioning.GAV;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ProvisioningXmlParserTest {

    private static Validator validator;

    @BeforeClass
    public static void beforeClass() throws SAXException, IOException {

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory
                .newSchema(new StreamSource(Files.newBufferedReader(getSchemaPath(), Charset.forName("utf-8"))));
        validator = schema.newValidator();
    }

    private static Path getSchemaPath() {
        return Paths.get("src/main/resources/schema/pm-provisioning-1_0.xsd");
    }

    @Test
    public void readBadNamespace() throws IOException {
        /*
         * urn:wildfly:pm-provisioning:1.0.1 used in provisioning-1.0.1.xml is not registered in ProvisioningXmlParser
         */
        validateAndParse("src/test/resources/provisioning/provisioning-1.0.1.xml",
                "cvc-elt.1: Cannot find the declaration of element 'installation'.",
                "Message: Unexpected element '{urn:wildfly:pm-provisioning:1.0.1}installation'");
    }

    @Test
    public void readMissingUniverseName() throws XMLStreamException, IOException {
        validateAndParse("src/test/resources/provisioning/provisioning-1.0-missing-universe-name.xml",
                "cvc-complex-type.4: Attribute 'name' must appear on element 'universe'.",
                "Message: Missing required attributes  NAME");
    }

    @Test
    public void readNoFp() throws IOException {
        validateAndParse("src/test/resources/provisioning/provisioning-1.0-no-fp.xml",
                "cvc-complex-type.2.4.b: The content of element 'universe' is not complete. One of '{\"urn:wildfly:pm-provisioning:1.0\":feature-pack}' is expected.",
                "There must be at least one feature-pack under universe");
    }

    @Test
    public void readNoUniverse() throws IOException {
        validateAndParse("src/test/resources/provisioning/provisioning-1.0-no-universe.xml",
                "cvc-complex-type.2.4.b: The content of element 'installation' is not complete. One of '{\"urn:wildfly:pm-provisioning:1.0\":universe}' is expected.",
                "There must be at least one universe under installation");
    }

    @Test
    public void readValid() throws IOException {
        ProvisioningMetaData found = validateAndParse("src/test/resources/provisioning/provisioning-1.0.xml", null, null);
        ProvisioningMetaData expected = new ProvisioningMetaData();
        expected.addFeaturePack(new GAV("org.jboss.group1", "fp1", "0.0.1"));
        expected.addFeaturePack(new GAV("org.jboss.group1", "fp2", "0.0.2"));
        expected.addFeaturePack(new GAV("org.jboss.group2", "fp3", "0.0.3"));

        Assert.assertEquals(expected, found);
    }

    protected void validate(Path p) throws SAXException, IOException {
        validator.validate(new StreamSource(Files.newBufferedReader(p, Charset.forName("utf-8"))));
    }

    private ProvisioningMetaData validateAndParse(String xmlFile, String xsdValidationExceptionMessage,
            String parseExceptionMessage) throws IOException {

        Path p = Paths.get(xmlFile);

        XMLStreamException parseException = null;
        SAXException xsdValidationException = null;

        try {
            validate(p);
        } catch (SAXException e) {
            xsdValidationException = e;
            Assert.assertEquals(xsdValidationExceptionMessage, e.getMessage());
        }

        ProvisioningMetaData result = null;
        try {
            result = new ProvisioningXmlParser().parse(Files.newBufferedReader(p, Charset.forName("utf-8")));
        } catch (XMLStreamException e) {
            parseException = e;
            String m = String.format("[%s] should contain [%s]", e.getMessage(), parseExceptionMessage);
            Assert.assertTrue(m, e.getMessage().contains(parseExceptionMessage));
        }

        /* Make sure XSD and parser both either accept or reject the document */

        String msg = ProvisioningXmlParser.class.getSimpleName() + " "
                + (parseException == null ? "accepts" : "does not accept") + " the file [" + xmlFile
                + "] while the schema " + getSchemaPath().toString() + " "
                + (xsdValidationException == null ? "does" : "does not");

        Assert.assertTrue(msg, (xsdValidationException == null && parseException == null)
                || (xsdValidationException != null && parseException != null));

        return result;
    }

}
