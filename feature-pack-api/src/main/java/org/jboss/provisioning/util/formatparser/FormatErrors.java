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
 * @author Alexey Loubyansky
 *
 */
public class FormatErrors {

    public static String parsingFailed(String str, int errorIndex, ParsingFormat format, int formatStartIndex) {
        return new StringBuilder()
                .append("Parsing of '").append(str).append("' failed at index ").append(errorIndex)
                .append(" while parsing format ").append(format).append(" started on index ").append(formatStartIndex)
                .toString();
    }

    public static String formatEndedPrematurely(ParsingFormat format) {
        return new StringBuilder()
                .append("Format ").append(format).append(" has ended prematurely")
                .toString();
    }

    public static String formatNotCompleted(ParsingFormat format) {
        return new StringBuilder()
                .append("Format ").append(format).append(" not completed")
                .toString();
    }

    public static String unexpectedStartingCharacter(ParsingFormat format, char expected, char actual) {
        return new StringBuilder()
                .append("Format ").append(format).append(" expects '").append(expected).append("' as it's starting character, not '").append(actual).append("'")
                .toString();
    }
}
