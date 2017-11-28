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
public class NameValueParsingFormat extends ParsingFormatBase {

    public static final String NAME = "NameValue";

    private static final NameValueParsingFormat INSTANCE = new NameValueParsingFormat();

    public static NameValueParsingFormat getInstance() {
        return INSTANCE;
    }

    protected NameValueParsingFormat() {
        super(NAME);
    }

    @Override
    public void react(ParsingContext ctx) throws FormatParsingException {
        if(ctx.charNow() == '=') {
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
            ctx.pushFormat(WildcardParsingFormat.getInstance());
        }
    }

    @Override
    public void eol(ParsingContext ctx) throws FormatParsingException {
        throw new FormatParsingException(FormatErrors.formatIncomplete(this));
    }
}
