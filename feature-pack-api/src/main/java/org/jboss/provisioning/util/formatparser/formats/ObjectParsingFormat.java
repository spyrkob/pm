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
import org.jboss.provisioning.util.formatparser.ParsingFormatBase;

/**
 *
 * @author Alexey Loubyansky
 */
public class ObjectParsingFormat extends ParsingFormatBase {

    public static final String NAME = "Object";

    public static ObjectParsingFormat getInstance() {
        return new ObjectParsingFormat();
    }

    protected NameValueParsingFormat nameValueFormat = NameValueParsingFormat.getInstance();

    protected ObjectParsingFormat() {
        super(NAME);
    }

    protected ObjectParsingFormat(String name) {
        super(name);
    }

    public boolean isAcceptsElement(String name) {
        return true;
    }

    public ObjectParsingFormat setNameValueSeparator(char ch) {
        this.nameValueFormat = NameValueParsingFormat.newInstance(ch);
        return this;
    }

    @Override
    public void react(ParsingContext ctx) throws FormatParsingException {
        switch(ctx.charNow()) {
            case ',' :
                ctx.popFormats();
                break;
            case '}':
                ctx.end();
                break;
            default:
                ctx.bounce();
        }
    }

    @Override
    public void deal(ParsingContext ctx) throws FormatParsingException {
        if(Character.isWhitespace(ctx.charNow())) {
            return;
        }
        ctx.pushFormat(nameValueFormat);
    }

    @Override
    public void eol(ParsingContext ctx) throws FormatParsingException {
        throw new FormatParsingException(FormatErrors.formatIncomplete(this));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((nameValueFormat == null) ? 0 : nameValueFormat.hashCode());
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
        ObjectParsingFormat other = (ObjectParsingFormat) obj;
        if (nameValueFormat == null) {
            if (other.nameValueFormat != null)
                return false;
        } else if (!nameValueFormat.equals(other.nameValueFormat))
            return false;
        return true;
    }
}
