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

package org.jboss.provisioning.featurepack.config.schema.test;

import org.jboss.provisioning.config.schema.ConfigTypeId;
import org.jboss.provisioning.config.schema.FeatureConfig;
import org.jboss.provisioning.config.schema.FeatureConfigSchema;
import org.jboss.provisioning.config.schema.FeatureConfigType;
import org.jboss.provisioning.config.schema.LineupUtility;
import org.jboss.provisioning.config.schema.SchemaConfigBuilder;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SandboxTestCase {

    private static final String ARTIFACT_ID = "pm-test-artifact";
    private static final String GROUP_ID = "org.jboss.pm.test";

    /**
     * @throws Exception
     */
    /**
     * @throws Exception
     */
    /**
     * @throws Exception
     */
    /**
     * @throws Exception
     */
    @Test
    public void testMain() throws Exception {

        final FeatureConfigSchema.Builder schemaBuilder = FeatureConfigSchema.builder();
        final FeatureConfigType sysPropType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "system-property"))
                .build(schemaBuilder);

        final FeatureConfigType sysPropsType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "system-properties"))
                .addOccurence(sysPropType.getTypeId(), false, true)
                .build(schemaBuilder);

        final FeatureConfigType accessControlType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "access-control"))
                .build(schemaBuilder);

        final FeatureConfigType managementType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "management"))
                .addOccurence(accessControlType.getTypeId(), false)
                .build(schemaBuilder);

        final FeatureConfigType subsystemType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "subsystem"))
                .build(schemaBuilder);

        final FeatureConfigType profileType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "profile"))
                .addOccurence(subsystemType.getTypeId(), true, true)
                .build(schemaBuilder);

        final FeatureConfigType profilesType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "profiles"))
                .addOccurence(profileType.getTypeId(), true, true)
                .build(schemaBuilder);

        final FeatureConfigType interfaceType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "interface"))
                .build(schemaBuilder);

        final FeatureConfigType interfacesType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "interfaces"))
                .addOccurence(interfaceType.getTypeId(), true, true)
                .build(schemaBuilder);

        final FeatureConfigType socketGroupType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "socket-binding-group"))
                .addDependency("default-interface", interfaceType.getTypeId())
                .build(schemaBuilder);

        final FeatureConfigType socketGroupsType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "socket-binding-groups"))
                .addOccurence(socketGroupType.getTypeId(), true, true)
                .build(schemaBuilder);

        final FeatureConfigType serverGroupType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "server-group"))
                .addDependency("profile", profileType.getTypeId())
                .addDependency("socket-binding-group", socketGroupType.getTypeId())
                .build(schemaBuilder);

        final FeatureConfigType serverGroupsType = FeatureConfigType.builder(ConfigTypeId.create(GROUP_ID, ARTIFACT_ID, "server-groups"))
                .addOccurence(serverGroupType.getTypeId(), true, true)
                .build(schemaBuilder);

        final FeatureConfigType domainType = FeatureConfigType.builder(GROUP_ID, ARTIFACT_ID, "domain")
                .addOccurence(sysPropsType.getTypeId(), false)
                .addOccurence(managementType.getTypeId())
                .addOccurence(profilesType.getTypeId())
                .addOccurence(interfacesType.getTypeId())
                .addOccurence(socketGroupsType.getTypeId())
                .addOccurence(serverGroupsType.getTypeId())
                .build(schemaBuilder);

        final FeatureConfigSchema schema = schemaBuilder.build();

        final FeatureConfig domain = SchemaConfigBuilder.builder(schema, domainType.getTypeId())
                .configureNew(managementType.getTypeId())
                    .done()
                .configureNew(profilesType.getTypeId())
                    .configureNew(profileType.getTypeId(), "default")
                        .configureNew(subsystemType.getTypeId())
                            .done()
                        .done()
                    .done()
                .configureNew(interfacesType.getTypeId())
                    .configureNew(interfaceType.getTypeId(), "management")
                        .done()
                    .configureNew(interfaceType.getTypeId(), "public")
                        .done()
                    .configureNew(interfaceType.getTypeId(), "unsecure")
                        .done()
                    .done()
                .configureNew(socketGroupsType.getTypeId())
                    .configureNew(socketGroupType.getTypeId(), "standard-sockets")
                        .dependsOn("default-interface", interfaceType.getTypeId(), "public")
                        .done()
                    .done()
                .configureNew(serverGroupsType.getTypeId())
                    .configureNew(serverGroupType.getTypeId(), "main-server-group")
                        .dependsOn("profile", profileType.getTypeId(), "default")
                        .dependsOn("socket-binding-group", socketGroupType.getTypeId(), "standard-sockets")
                        .done()
                    .configureNew(serverGroupType.getTypeId(), "other-server-group")
                        .dependsOn("profile", profileType.getTypeId(), "default")
                        .dependsOn("socket-binding-group", socketGroupType.getTypeId(), "standard-sockets")
                        .done()
                    .done()
                .build();
/*
        final FeatureConfig sampleConfig = domainType.configBuilder("sample")
                .addFeatureConfig(requiredOnce.configBuilder("a")
                        .addDependency("requiredDep", reqUnbA)
                        .build())
                .addFeatureConfig(reqUnbA)
                .build();
*/
        for(FeatureConfig config : LineupUtility.lineup(domain, domain)) {
            System.out.println(config.getRef() == null ? config.getTypeId() : config.getRef());
        }
    }
}
