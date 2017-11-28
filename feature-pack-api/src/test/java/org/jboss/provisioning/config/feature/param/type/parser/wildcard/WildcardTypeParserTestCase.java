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

package org.jboss.provisioning.config.feature.param.type.parser.wildcard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.config.feature.param.type.parser.TypeParserTestBase;
import org.jboss.provisioning.util.formatparser.ParsingFormat;
import org.jboss.provisioning.util.formatparser.formats.WildcardParsingFormat;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class WildcardTypeParserTestCase extends TypeParserTestBase {

    @Override
    protected ParsingFormat getTestFormat() {
        return WildcardParsingFormat.getInstance();
    }

    @Test
    public void testEmptyString() throws Exception {
        testFormat("", null);
    }

    @Test
    public void testString() throws Exception {
        testFormat("a b c", "a b c");
    }

    @Test
    public void testList() throws Exception {
        testFormat("[a b c]", Collections.singletonList("a b c"));
        testFormat("[a,b,c]", Arrays.asList("a", "b", "c"));
    }

    @Test
    public void testObject() throws Exception {
        testFormat("{a=b}", Collections.singletonMap("a", "b"));
        final Map<String, String> map = new HashMap<>(2);
        map.put("a", "b");
        map.put("c", "d");
        testFormat("{a=b, c = d }", map);
    }
}
