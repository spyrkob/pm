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
package org.jboss.provisioning.plugin.wildfly.config;



import java.util.List;

import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;

import static org.jboss.provisioning.plugin.wildfly.config.GeneratorConfigParser20.Attribute;
import static org.jboss.provisioning.plugin.wildfly.config.GeneratorConfigParser20.Element;

/**
 *
 * @author Alexey Loubyansky
 */
public class GeneratorConfigXMLWriter20 {

    public static final GeneratorConfigXMLWriter20 INSTANCE = new GeneratorConfigXMLWriter20();

    private GeneratorConfigXMLWriter20() {
    }

    public void write(GeneratorConfig generatorConfig, ElementNode parentElementNode) {
        if(generatorConfig == null) {
            return;
        }
        final ElementNode root = new ElementNode(parentElementNode, GeneratorConfigParser20.ELEMENT_LOCAL_NAME);
        parentElementNode.addChild(root);
        if(generatorConfig.getStandaloneConfig() != null) {
            writeStandaloneConfig(generatorConfig.getStandaloneConfig(), root);
        }
        if(generatorConfig.getHostControllerConfig() != null) {
            writeHostControllerConfig(generatorConfig.getHostControllerConfig(), root);
        }
    }

    private void writeStandaloneConfig(StandaloneConfig stdConfig, ElementNode configGenNode) {
        final ElementNode stdConfigNode = new ElementNode(configGenNode, Element.STANDALONE.getLocalName());
        configGenNode.addChild(stdConfigNode);
        stdConfigNode.addAttribute(Attribute.SERVER_CONFIG.getLocalName(), new AttributeValue(stdConfig.getServerConfig()));
        writeScripts(stdConfig.getScripts(), stdConfigNode);
    }

    private void writeHostControllerConfig(HostControllerConfig stdConfig, ElementNode configGenNode) {
        final ElementNode hcConfigNode = new ElementNode(configGenNode, Element.HOST_CONTROLLER.getLocalName());
        configGenNode.addChild(hcConfigNode);
        hcConfigNode.addAttribute(Attribute.DOMAIN_CONFIG.getLocalName(), new AttributeValue(stdConfig.getDomainConfig()));
        hcConfigNode.addAttribute(Attribute.HOST_CONFIG.getLocalName(), new AttributeValue(stdConfig.getHostConfig()));
        writeScripts(stdConfig.getScripts(), hcConfigNode);
    }

    private void writeScripts(List<String> scripts, ElementNode parentNode) {
        final ElementNode scriptsNode = new ElementNode(parentNode, Element.SCRIPTS.getLocalName());
        parentNode.addChild(scriptsNode);
        for(String script : scripts) {
            final ElementNode scriptNode = new ElementNode(scriptsNode, Element.SCRIPT.getLocalName());
            scriptsNode.addChild(scriptNode);
            scriptNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(script));
        }
    }
}
