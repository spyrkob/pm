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

package org.jboss.provisioning.util.formatparser.formats;

import org.jboss.provisioning.util.formatparser.FormatErrors;
import org.jboss.provisioning.util.formatparser.FormatParsingException;
import org.jboss.provisioning.util.formatparser.ParsingContext;
import org.jboss.provisioning.util.formatparser.ParsingFormat;
import org.jboss.provisioning.util.formatparser.ParsingFormatBase;

/**
 *
 * @author Alexey Loubyansky
 */
public class NameValueParsingFormat extends ParsingFormatBase {

    public static final String NAME = "NameValue";

    private static NameValueParsingFormat INSTANCE;

    public static NameValueParsingFormat getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new NameValueParsingFormat('=', WildcardParsingFormat.getInstance());
        }
        return INSTANCE;
    }

    public static NameValueParsingFormat getInstance(ParsingFormat valueFormat) {
        return new NameValueParsingFormat('=', valueFormat);
    }

    public static NameValueParsingFormat getInstance(char separator, ParsingFormat valueFormat) {
        return new NameValueParsingFormat(separator, valueFormat);
    }

    private final char separator;
    private final ParsingFormat valueFormat;

    protected NameValueParsingFormat(char separator, ParsingFormat valueFormat) {
        super(NAME);
        this.separator = separator;
        this.valueFormat = valueFormat;
    }

    public ParsingFormat getValueFormat() {
        return valueFormat;
    }

    @Override
    public void react(ParsingContext ctx) throws FormatParsingException {
        if(ctx.charNow() == separator) {
            ctx.popFormats();
        }
    }

    @Override
    public void pushed(ParsingContext ctx) throws FormatParsingException {
        ctx.pushFormat(StringParsingFormat.getInstance());
    }

    @Override
    public void deal(ParsingContext ctx) throws FormatParsingException {
        if(!Character.isWhitespace(ctx.charNow())) {
            ctx.pushFormat(valueFormat);
        }
    }

    @Override
    public void eol(ParsingContext ctx) throws FormatParsingException {
        throw new FormatParsingException(FormatErrors.formatIncomplete(this));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + separator;
        result = prime * result + ((valueFormat == null) ? 0 : valueFormat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NameValueParsingFormat other = (NameValueParsingFormat) obj;
        if (separator != other.separator)
            return false;
        if (valueFormat == null) {
            if (other.valueFormat != null)
                return false;
        } else if (!valueFormat.equals(other.valueFormat))
            return false;
        return true;
    }
}
