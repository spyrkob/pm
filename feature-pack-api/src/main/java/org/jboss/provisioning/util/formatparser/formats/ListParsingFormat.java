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
public class ListParsingFormat extends ParsingFormatBase {

    private static final ListParsingFormat INSTANCE = new ListParsingFormat(WildcardParsingFormat.getInstance());

    public static ListParsingFormat getInstance() {
        return INSTANCE;
    }

    public static ListParsingFormat getInstance(ParsingFormat itemFormat) {
        assert itemFormat != null : "item format is null";
        return new ListParsingFormat(itemFormat);
    }

    private final ParsingFormat itemFormat;

    protected ListParsingFormat(ParsingFormat itemFormat) {
        super("List");
        this.itemFormat = itemFormat;
    }

    @Override
    public void pushed(ParsingContext ctx) throws FormatParsingException {
        if(ctx.charNow() != '[') {
            throw new FormatParsingException(FormatErrors.unexpectedStartingCharacter(this, '[', ctx.charNow()));
        }
    }

    @Override
    public void react(ParsingContext ctx) throws FormatParsingException {
        switch(ctx.charNow()) {
            case ',' :
                ctx.popFormats();
                break;
            case ']':
                ctx.end();
                break;
            default:
                ctx.bounce();
        }
    }

    @Override
    public void deal(ParsingContext ctx) throws FormatParsingException {
        if(!Character.isWhitespace(ctx.charNow())) {
            ctx.pushFormat(itemFormat);
        }
    }

    @Override
    public void eol(ParsingContext ctx) throws FormatParsingException {
        throw new FormatParsingException(FormatErrors.formatNotCompleted(this));
    }

    @Override
    public String toString() {
        return name + "<" + itemFormat + ">";
    }
}
