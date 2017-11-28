/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.config.feature.param.type.parser.formatexpr;

import org.jboss.provisioning.util.formatparser.FormatParser;
import org.jboss.provisioning.util.formatparser.formats.CompositeParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.ListParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.ObjectParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.StringParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.WildcardParsingFormat;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class FormatExprParserTestCase {

    @Test
    public void testWildcard() throws Exception {
        Assert.assertEquals(WildcardParsingFormat.getInstance(), FormatParser.resolveFormat("?"));
        Assert.assertEquals(WildcardParsingFormat.getInstance(), FormatParser.resolveFormat(" ? "));
    }

    @Test
    public void testString() throws Exception {
        Assert.assertEquals(StringParsingFormat.getInstance(), FormatParser.resolveFormat("String"));
        Assert.assertEquals(StringParsingFormat.getInstance(), FormatParser.resolveFormat(" String "));
    }

    @Test
    public void testListOfWildcards() throws Exception {
        Assert.assertEquals(ListParsingFormat.getInstance(), FormatParser.resolveFormat("List"));
        Assert.assertEquals(ListParsingFormat.getInstance(), FormatParser.resolveFormat("List<?>"));
        Assert.assertEquals(ListParsingFormat.getInstance(), FormatParser.resolveFormat(" List < ? > "));
    }

    @Test
    public void testListOfStrings() throws Exception {
        Assert.assertEquals(ListParsingFormat.getInstance(StringParsingFormat.getInstance()), FormatParser.resolveFormat("List<String>"));
        Assert.assertEquals(ListParsingFormat.getInstance(StringParsingFormat.getInstance()), FormatParser.resolveFormat(" List < String > "));
    }

    @Test
    public void testListOfListsOfStrings() throws Exception {
        Assert.assertEquals(
                ListParsingFormat.getInstance(
                        ListParsingFormat.getInstance(
                                StringParsingFormat.getInstance())), FormatParser.resolveFormat("List<List<String>>"));
    }

    @Test
    public void testSimpleNamedComposite() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance("FullName")
                .addElement("last-name", StringParsingFormat.getInstance())
                .addElement("first-name", StringParsingFormat.getInstance()), FormatParser.resolveFormat("FullName{first-name:String, last-name:String}"));
    }

    @Test
    public void testSimpleUnnamedComposite() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("last-name", StringParsingFormat.getInstance())
                .addElement("first-name", StringParsingFormat.getInstance()), FormatParser.resolveFormat("{first-name:String, last-name:String}"));
    }

    @Test
    public void testCompositeWithAttrListOfWildcards() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("str", StringParsingFormat.getInstance())
                .addElement("list", ListParsingFormat.getInstance()), FormatParser.resolveFormat("{str:String, list:List}"));
    }

    @Test
    public void testCompositeWithAttrListOfStrings() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("str", StringParsingFormat.getInstance())
                .addElement("list", ListParsingFormat.getInstance(StringParsingFormat.getInstance())), FormatParser.resolveFormat("{str:String, list:List<String>}"));
    }

    @Test
    public void testCompositeWithAttrWildcard() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("wildcard", WildcardParsingFormat.getInstance()), FormatParser.resolveFormat("{wildcard:?}"));
    }

    @Test
    public void testCompositeWithAttrObject() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("o", ObjectParsingFormat.getInstance()), FormatParser.resolveFormat("{o:Object}"));
    }

    @Test
    public void testCompositeWithAttrComposite() throws Exception {
        Assert.assertEquals(CompositeParsingFormat.newInstance()
                .addElement("str", StringParsingFormat.getInstance())
                .addElement("full-name", CompositeParsingFormat.newInstance()
                        .addElement("first-name", StringParsingFormat.getInstance())
                        .addElement("last-name", StringParsingFormat.getInstance())), FormatParser.resolveFormat("{str:String, full-name:{first-name:String,last-name:String}}"));
    }
}
