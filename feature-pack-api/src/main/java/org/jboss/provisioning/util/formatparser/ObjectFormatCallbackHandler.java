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

import java.util.Collections;
import java.util.Map;

import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public class ObjectFormatCallbackHandler extends FormatContentHandler {

    Map<String, Object> map = Collections.emptyMap();

    public ObjectFormatCallbackHandler(ParsingFormat format, int strIndex) {
        super(format, strIndex);
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandler#addChild(org.jboss.provisioning.spec.type.ParsingCallbackHandler)
     */
    @Override
    public void addChild(FormatContentHandler childHandler) throws FormatParsingException {
        if(!childHandler.getFormat().getName().equals(NameValueParsingFormat.getInstance().getName())) {
            throw new FormatParsingException("Object can't accept " + childHandler.getFormat());
        }
        NameValueFormatCallbackHandler nameValue = (NameValueFormatCallbackHandler) childHandler;
        map = PmCollections.putLinked(map, nameValue.name, nameValue.value);
    }

    /* (non-Javadoc)
     * @see org.jboss.provisioning.spec.type.ParsingCallbackHandler#getParsedValue()
     */
    @Override
    public Object getParsedValue() throws FormatParsingException {
        return map;
    }
}
