/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.plugin.wildfly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.WildFlyPostFeaturePackTasks;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.WildFlyPostFeaturePackTasksParser;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.build.FeaturePackBuildModelParser;
import org.jboss.provisioning.plugin.wildfly.featurepack.model.build.WildFlyFeaturePackBuild;

/**
 *
 * @author Alexey Loubyansky
 */
class Util {

    interface ArtifactProcessor {
        void process(ArtifactCoords coords) throws IOException;
    }

    static WildFlyFeaturePackBuild loadFeaturePackBuildConfig(Path configFile, Properties props) throws ProvisioningException {
        try (InputStream configStream = Files.newInputStream(configFile)) {
            props.putAll(System.getProperties());
            return new FeaturePackBuildModelParser(new MapPropertyResolver(props)).parse(configStream);
        } catch (XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(configFile), e);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.openFile(configFile), e);
        }
    }

    static WildFlyPostFeaturePackTasks loadWildFlyTasks(Path configFile, Properties props) throws ProvisioningException {
        try (InputStream configStream = Files.newInputStream(configFile)) {
            props.putAll(System.getProperties());
            return new WildFlyPostFeaturePackTasksParser(new MapPropertyResolver(props)).parse(configStream);
        } catch (XMLStreamException e) {
            throw new ProvisioningException(Errors.parseXml(configFile), e);
        } catch (IOException e) {
            throw new ProvisioningException(Errors.openFile(configFile), e);
        }
    }

    static void processModuleArtifacts(final Path moduleXml, ArtifactProcessor ap) throws IOException {
        try {
            final ModuleParseResult parsedModule = ModuleXmlParser.parse(moduleXml, "UTF-8");
            for(ModuleParseResult.ArtifactName artName : parsedModule.artifacts) {
                ap.process(ArtifactCoordsUtil.fromJBossModules(artName.getArtifactCoords(), "jar"));
            }
        } catch (XMLStreamException e) {
            throw new IOException(Errors.parseXml(moduleXml), e);
        }
    }

}
