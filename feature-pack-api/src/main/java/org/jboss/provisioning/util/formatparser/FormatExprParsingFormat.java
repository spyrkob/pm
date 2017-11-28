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

/**
 *
 * @author Alexey Loubyansky
 */
public class FormatExprParsingFormat extends ParsingFormatBase {

    public static final String NAME = "FormatExpr";

    private static final FormatExprParsingFormat INSTANCE = new FormatExprParsingFormat();

    public static FormatExprParsingFormat getInstance() {
        return INSTANCE;
    }

    protected FormatExprParsingFormat() {
        super(NAME);
    }

    @Override
    public void pushed(ParsingContext ctx) throws FormatParsingException {
        final char charNow = ctx.charNow();
        if(Character.isWhitespace(charNow) || charNow == '<') {
            return;
        }
        ctx.content();
    }

    @Override
    public void react(ParsingContext ctx) throws FormatParsingException {
        switch(ctx.charNow()) {
            case '>':
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
        if(ctx.charNow() == '<') {
            ctx.pushFormat(INSTANCE);
        } else {
            ctx.content();
        }
    }

    @Override
    public void eol(ParsingContext ctx) throws FormatParsingException {
        //throw new FormatParsingException(FormatErrors.formatIncomplete(this));
    }
}
