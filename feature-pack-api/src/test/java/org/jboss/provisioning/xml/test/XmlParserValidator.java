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
package org.jboss.provisioning.xml.test;

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

import org.jboss.provisioning.xml.XmlParser;
import org.junit.Assert;
import org.xml.sax.SAXException;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class XmlParserValidator<T> {

    private final Validator validator;

    private final Path schemaPath;

    private final XmlParser<T> parser;

    public XmlParserValidator(Path schemaPath, XmlParser<T> parser) {
        super();
        this.schemaPath = schemaPath;
        this.parser = parser;
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

    public T validateAndParse(String xmlFile, String xsdValidationExceptionMessage,
            String parseExceptionMessage) throws Exception {

        Path p = Paths.get(xmlFile);

        XMLStreamException parseException = null;
        SAXException xsdValidationException = null;

        try {
            validate(p);
        } catch (SAXException e) {

            if (xsdValidationExceptionMessage == null) {
                throw e;
            }

            xsdValidationException = e;
            Assert.assertEquals(xsdValidationExceptionMessage, e.getMessage());
        }

        T result = null;
        try {
            result = parser.parse(Files.newBufferedReader(p, Charset.forName("utf-8")));
        } catch (XMLStreamException e) {
            parseException = e;
            String m = String.format("[%s] should contain [%s]", e.getMessage(), parseExceptionMessage);
            if(parseExceptionMessage == null) {
                Assert.fail(e.getMessage());
            } else {
                Assert.assertTrue(m, e.getMessage().contains(parseExceptionMessage));
            }
        }

        /* Make sure XSD and parser both either accept or reject the document */

        String msg = parser.getClass().getSimpleName() + " "
                + (parseException == null ? "accepts" : "does not accept") + " the file [" + xmlFile
                + "] while the schema " + schemaPath.toString() + " "
                + (xsdValidationException == null ? "does" : "does not");

        Assert.assertTrue(msg, (xsdValidationException == null && parseException == null)
                || (xsdValidationException != null && parseException != null));

        return result;
    }
}
