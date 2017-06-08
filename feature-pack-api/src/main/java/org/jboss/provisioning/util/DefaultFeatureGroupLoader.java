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

package org.jboss.provisioning.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.FeatureGroupSpec;
import org.jboss.provisioning.feature.FeatureGroupLoader;
import org.jboss.provisioning.xml.FeatureGroupXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultFeatureGroupLoader implements FeatureGroupLoader {

    private static String XML = ".xml";

    protected final Path baseDir;

    public DefaultFeatureGroupLoader(Path baseDir) {
        this.baseDir = baseDir;
    }

    protected Path resolvePath(String name) {
        return baseDir.resolve(name + XML);
    }

    @Override
    public FeatureGroupSpec load(String fgSource, String fgName) throws ProvisioningDescriptionException {
        final Path path = resolvePath(fgName);
        if(!Files.exists(path)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(path));
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return FeatureGroupXmlParser.getInstance().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningDescriptionException(Errors.readFile(path), e);
        }
    }
}
