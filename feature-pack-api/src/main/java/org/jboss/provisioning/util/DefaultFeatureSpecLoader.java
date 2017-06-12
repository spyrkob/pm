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
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.feature.FeatureSpecLoader;
import org.jboss.provisioning.feature.SpecId;
import org.jboss.provisioning.xml.FeatureSpecXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultFeatureSpecLoader implements FeatureSpecLoader {

    private static String SPEC_XML = "spec.xml";

    protected final Path baseDir;

    public DefaultFeatureSpecLoader(Path baseDir) {
        this.baseDir = baseDir;
    }

    protected Path resolvePath(SpecId spec) {
        return baseDir.resolve(spec.getName()).resolve(SPEC_XML);
    }

    @Override
    public FeatureSpec load(SpecId spec) throws ProvisioningDescriptionException {
        final Path path = resolvePath(spec);
        if(!Files.exists(path)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(path));
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return FeatureSpecXmlParser.getInstance().parse(reader);
        } catch (IOException | XMLStreamException e) {
            throw new ProvisioningDescriptionException(Errors.readFile(path), e);
        }
    }
}
