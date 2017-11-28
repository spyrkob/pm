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

import org.jboss.provisioning.util.formatparser.formats.ListParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.NameValueParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.ObjectParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.StringParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.WildcardParsingFormat;
import org.jboss.provisioning.util.formatparser.handlers.ListContentHandler;
import org.jboss.provisioning.util.formatparser.handlers.NameValueContentHandler;
import org.jboss.provisioning.util.formatparser.handlers.ObjectContentHandler;
import org.jboss.provisioning.util.formatparser.handlers.StringContentHandler;
import org.jboss.provisioning.util.formatparser.handlers.WildcardContentHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultContentHandlerFactory implements FormatContentHandlerFactory {

    private static final DefaultContentHandlerFactory INSTANCE = new DefaultContentHandlerFactory();

    public static DefaultContentHandlerFactory getInstance() {
        return INSTANCE;
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandlerFactory#forFormat(org.jboss.provisioning.spec.type.ParsingFormat)
     */
    @Override
    public FormatContentHandler forFormat(ParsingFormat format, int strIndex) throws FormatParsingException {
        final String name = format.getName();
        if(name.equals(StringParsingFormat.getInstance().getName())) {
            return new StringContentHandler(format, strIndex);
        } else if(name.equals(ListParsingFormat.getInstance().getName())) {
            return new ListContentHandler(format, strIndex);
        } else if(name.equals(ObjectParsingFormat.getInstance().getName())) {
            return new ObjectContentHandler(format, strIndex);
        } else if(name.equals(NameValueParsingFormat.getInstance().getName())) {
            return new NameValueContentHandler(format, strIndex);
        } else if(name.equals(WildcardParsingFormat.getInstance().getName())) {
            return new WildcardContentHandler(format, strIndex);
        } else {
            throw new FormatParsingException("Unknown format " + format);
        }
    }
}
