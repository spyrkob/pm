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

import java.util.ArrayList;
import java.util.List;

import org.jboss.provisioning.util.formatparser.formats.WildcardParsingFormat;

/**
 *
 * @author Alexey Loubyansky
 */
public class FormatParser implements ParsingContext {

    public static ParsingFormat resolveFormat(String expr) throws FormatParsingException {
        return (ParsingFormat) parse(
                ExtendedContentHandlerFactory.getInstance()
                .addContentHandler(FormatExprParsingFormat.NAME, FormatExprContentHandler.class),
                FormatExprParsingFormat.getInstance(), expr);
    }

    public static Object parse(String str) throws FormatParsingException {
        return parse(DefaultContentHandlerFactory.getInstance(), WildcardParsingFormat.getInstance(), str);
    }

    public static Object parse(ParsingFormat format, String str) throws FormatParsingException {
        return parse(DefaultContentHandlerFactory.getInstance(), format, str);
    }

    public static Object parse(FormatContentHandlerFactory cbFactory, ParsingFormat format, String str) throws FormatParsingException {
        return new FormatParser(cbFactory, format, str).parse();
    }

    private final ParsingFormat rootFormat;
    private final FormatContentHandlerFactory cbFactory;

    private List<FormatContentHandler> cbStack = new ArrayList<>();

    private String str;
    private int chI;
    private int formatIndex;

    private boolean breakHandling;
    private boolean bounced;

    public FormatParser(FormatContentHandlerFactory cbFactory, ParsingFormat rootFormat, String str) {
        this.rootFormat = rootFormat;
        this.cbFactory = cbFactory;
        this.str = str;
    }

    public Object parse() throws FormatParsingException {
        if(str == null) {
            return null;
        }

        chI = 0;

        final FormatContentHandler rootCb = cbFactory.forFormat(rootFormat, chI);
        if (!str.isEmpty()) {
            cbStack.add(rootCb);
            try {
                doParse();
            } catch(FormatParsingException e) {
                final ParsingFormat format;
                final int formatStart;
                if(formatIndex < 0) {
                    format = rootFormat;
                    formatStart = 0;
                } else {
                    FormatContentHandler ch = cbStack.get(formatIndex);
                    format = ch.format;
                    formatStart = ch.strIndex;
                }
                throw new FormatParsingException(FormatErrors.parsingFailed(str, chI, format, formatStart), e);
            }
        }
        return rootCb.getContent();
    }

    private void doParse() throws FormatParsingException {
        rootFormat.pushed(this);

        while (++chI < str.length()) {

            formatIndex = cbStack.size();
            breakHandling = false;
            bounced = false;
            while (formatIndex > 0 && !breakHandling) {
                final FormatContentHandler cb = cbStack.get(--formatIndex);
                cb.getFormat().react(this);
            }

            if (bounced || !breakHandling) {
                formatIndex = cbStack.size() - 1;
                if(formatIndex < 0) {
                    throw new FormatParsingException("EOL");
                }
//                if(bounced) {
//                    System.out.println(charNow() + " bounced to " + cbStack.get(formatIndex).getFormat());
//                }
                cbStack.get(formatIndex).getFormat().deal(this);
            }
        }

        for (int i = cbStack.size() - 1; i >= 0; --i) {
            final FormatContentHandler ended = cbStack.get(i);
            ended.getFormat().eol(this);
            if (i > 0) {
                cbStack.get(i - 1).addChild(ended);
            }
        }
    }

    @Override
    public void pushFormat(ParsingFormat format) throws FormatParsingException {
        if(formatIndex != cbStack.size() - 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append(cbStack.get(0).getFormat());
            if(formatIndex == 0) {
                buf.append('!');
            }
            for(int i = 1; i < cbStack.size(); ++i) {
                buf.append(", ").append(cbStack.get(i).getFormat());
                if(formatIndex == i) {
                    buf.append('!');
                }
            }
            throw new FormatParsingException("Child formats need to be popped: " + buf);
        }
        breakHandling = true;
        cbStack.add(cbFactory.forFormat(format, chI));
        //System.out.println("pushFormat: " + format + " [" + cbStack.get(formatIndex).getFormat() + ", " + charNow() + "]");
        ++formatIndex;
        format.pushed(this);
    }

    @Override
    public void popFormats() throws FormatParsingException {
        breakHandling = true;
        if(formatIndex == cbStack.size() - 1) {
            return;
        }
        for(int i = cbStack.size() - 1; i > formatIndex; --i) {
            final FormatContentHandler ended = cbStack.remove(i);
            //System.out.println("poppedFormat: " + ended.getFormat());
            if(!cbStack.isEmpty()) {
                cbStack.get(i - 1).addChild(ended);
            }
        }
    }

    @Override
    public void end() throws FormatParsingException {
        breakHandling = true;
        --formatIndex; // this is done before the loop for correct error reporting
        for(int i = cbStack.size() - 1; i >= formatIndex + 1; --i) {
            final FormatContentHandler ended = cbStack.remove(i);
            if(!cbStack.isEmpty()) {
                cbStack.get(i - 1).addChild(ended);
            }
        }

        if(!cbStack.isEmpty() && cbStack.get(formatIndex).format.isWrapper()) {
            while (formatIndex > 0) {
                final FormatContentHandler ended = cbStack.get(formatIndex);
                if(!ended.format.isWrapper()) {
                    break;
                }
                cbStack.remove(formatIndex--);
                cbStack.get(formatIndex).addChild(ended);
            }
        }

        if(cbStack.isEmpty() && chI < str.length() - 1) {
            throw new FormatParsingException(FormatErrors.formatEndedPrematurely(rootFormat));
        }
    }

    @Override
    public void bounce() {
        breakHandling = true;
        bounced = true;
    }

    @Override
    public char charNow() {
        return str.charAt(chI);
    }

    @Override
    public boolean startsNow(String str) {
        return this.str.startsWith(str, chI);
    }

    @Override
    public void content() throws FormatParsingException {
        cbStack.get(cbStack.size() - 1).character(charNow());
    }
}
