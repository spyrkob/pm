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

package org.jboss.provisioning.util.formatparser;

import java.util.Map;

import org.jboss.provisioning.util.formatparser.formats.CompositeParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.ListParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.ObjectParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.StringParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.WildcardParsingFormat;

/**
 *
 * @author Alexey Loubyansky
 */
public class FormatExprContentHandler extends FormatContentHandler {

    private StringBuilder name;
    private ParsingFormat type;
    private Map<?,?> elems;

    public FormatExprContentHandler(ParsingFormat format, int strIndex) {
        super(format, strIndex);
    }

    @Override
    public void character(char ch) throws FormatParsingException {
        if(name == null) {
            name = new StringBuilder();
        }
        name.append(ch);
    }

    @Override
    public void addChild(FormatContentHandler childHandler) throws FormatParsingException {
        if(type != null || elems != null) {
            throw new FormatParsingException("Type parameter of " + format + " has already been initialized to " + (type == null ? elems : type));
        }
        if (FormatExprParsingFormat.COMPOSITE_TYPE_FORMAT_NAME.equals(childHandler.format.getName())) {
            elems = (Map<?, ?>) childHandler.getContent();
        } else if (FormatExprParsingFormat.LIST_TYPE_FORMAT_NAME.equals(childHandler.format.getName())) {
            if(name == null) {
                name = new StringBuilder();
            }
            name.append("List");
            type = (ParsingFormat) childHandler.getContent();
        } else {
            type = (ParsingFormat) childHandler.getContent();
        }
    }

    @Override
    public Object getContent() throws FormatParsingException {
        return resolveFormat();
    }

    public ParsingFormat resolveFormat() throws FormatParsingException {
        final String name = this.name == null ? null : this.name.toString();

        if(elems != null) {
            final CompositeParsingFormat format = CompositeParsingFormat.newInstance(name);
            for(Map.Entry<?,?> elem : elems.entrySet()) {
                format.addElement((String)elem.getKey(), (ParsingFormat)elem.getValue());
            }
            return format;
        }

        if(name == null || name.length() == 0) {
            throw new FormatParsingException("Format type was not specified");
        }

        if(StringParsingFormat.NAME.equals(name)) {
            assertNoTypeParam(name);
            return StringParsingFormat.getInstance();
        }

        if(ListParsingFormat.NAME.equals(name)) {
            if(type == null) {
                return ListParsingFormat.getInstance();
            }
            return ListParsingFormat.newInstance(type);
        }

        if(WildcardParsingFormat.NAME.equals(name)) {
            assertNoTypeParam(name);
            return WildcardParsingFormat.getInstance();
        }

        if(ObjectParsingFormat.NAME.equals(name)) {
            assertNoTypeParam(name);
            return ObjectParsingFormat.getInstance();
        }

        throw new FormatParsingException("Unexpected format name " + name);
    }

    private void assertNoTypeParam(final String name) throws FormatParsingException {
        if(type != null) {
            throw new FormatParsingException(FormatErrors.formatExprDoesNotSupportTypeParam(name));
        }
    }
}
