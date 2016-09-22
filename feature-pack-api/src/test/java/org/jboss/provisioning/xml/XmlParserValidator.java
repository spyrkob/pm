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
import java.io.Reader;
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

import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.junit.Assert;
import org.xml.sax.SAXException;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class XmlParserValidator {

    private final Validator validator;

    private final Path schemaPath;

    public XmlParserValidator(Path schemaPath) {
        super();
        this.schemaPath = schemaPath;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (Reader r = Files.newBufferedReader(schemaPath, Charset.forName("utf-8"))) {
            Schema schema = schemaFactory.newSchema(new StreamSource(r));
            validator = schema.newValidator();
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }


    public void validate(Path p) throws SAXException, IOException {
        validator.validate(new StreamSource(Files.newBufferedReader(p, Charset.forName("utf-8"))));
    }

    public ProvisionedInstallationDescription validateAndParse(String xmlFile, String xsdValidationExceptionMessage,
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

        ProvisionedInstallationDescription result = null;
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
                + "] while the schema " + schemaPath.toString() + " "
                + (xsdValidationException == null ? "does" : "does not");

        Assert.assertTrue(msg, (xsdValidationException == null && parseException == null)
                || (xsdValidationException != null && parseException != null));

        return result;
    }
}
