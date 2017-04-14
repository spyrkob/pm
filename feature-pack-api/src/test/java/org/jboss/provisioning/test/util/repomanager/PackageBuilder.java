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

package org.jboss.provisioning.test.util.repomanager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.test.util.fs.FsTaskContext;
import org.jboss.provisioning.test.util.fs.FsTaskList;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.PackageXmlWriter;
import org.jboss.provisioning.xml.ParameterSetXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageBuilder {

    public static PackageBuilder newInstance(FeaturePackBuilder fp, String name) {
        return new PackageBuilder(fp, name);
    }

    private final FeaturePackBuilder fp;
    private boolean isDefault;
    private final PackageSpec.Builder pkg;
    private final FsTaskList tasks = FsTaskList.newList();
    private Map<String, PackageConfigBuilder> configs = Collections.emptyMap();

    private PackageBuilder(FeaturePackBuilder fp, String name) {
        this.fp = fp;
        pkg = PackageSpec.builder(name);
    }

    public FeaturePackBuilder getFeaturePack() {
        return fp;
    }

    public PackageBuilder setDefault() {
        isDefault = true;
        return this;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public PackageBuilder addDependency(String pkgName) {
        return addDependency(pkgName, false);
    }

    public PackageBuilder addDependency(String pkgName, boolean optional) {
        this.pkg.addDependency(pkgName, optional);
        return this;
    }

    public PackageBuilder addDependency(PackageDependencySpec dep) {
        this.pkg.addDependency(dep);
        return this;
    }

    public PackageBuilder addDependency(String fpDepName, PackageDependencySpec dep) {
        this.pkg.addDependency(fpDepName, dep);
        return this;
    }

    public PackageBuilder addDependency(String fpDepName, String pkgName) {
        this.pkg.addDependency(fpDepName, pkgName);
        return this;
    }

    public PackageBuilder addDependency(String fpDepName, String pkgName, boolean optional) {
        this.pkg.addDependency(fpDepName, pkgName, optional);
        return this;
    }

    public PackageBuilder addPath(String relativeTarget, Path src) {
        tasks.copy(src, relativeTarget);
        return this;
    }

    public PackageBuilder addDir(String relativeTarget, Path src, boolean contentOnly) {
        tasks.copyDir(src, relativeTarget, contentOnly);
        return this;
    }

    public PackageBuilder writeContent(String relativeTarget, String content) {
        tasks.write(content, relativeTarget);
        return this;
    }

    public PackageBuilder addParameter(String name, String defaultValue) {
        this.pkg.addParameter(name, defaultValue);
        return this;
    }

    public PackageConfigBuilder addConfig(String config) {
        if(configs.isEmpty()) {
            configs = new HashMap<>();
            final PackageConfigBuilder configBuilder = new PackageConfigBuilder(this, config);
            configs.put(config, configBuilder);
            return configBuilder;
        }
        PackageConfigBuilder configBuilder = configs.get(config);
        if(configBuilder == null) {
            configBuilder = new PackageConfigBuilder(this, config);
            configs.put(config, configBuilder);
        }
        return configBuilder;
    }

    public PackageSpec build(Path fpDir) {
        final PackageSpec pkgSpec = pkg.build();
        final Path pkgDir;
        try {
            pkgDir = LayoutUtils.getPackageDir(fpDir, pkgSpec.getName(), false);
        } catch (ProvisioningDescriptionException e) {
            throw new IllegalStateException(e);
        }
        TestUtils.mkdirs(pkgDir);
        if(!configs.isEmpty()) {
            final Path configsDir = LayoutUtils.getPackageConfigsDir(pkgDir);
            TestUtils.mkdirs(configsDir);
            for(PackageConfigBuilder configBuilder : configs.values()) {
                final ParameterSet config = configBuilder.configBuilder.build();
                for(PackageParameter param : config.getParameters()) {
                    if(!pkgSpec.hasParameter(param.getName())) {
                        throw new IllegalStateException("Config " + config.getName() + " of package " + pkgSpec.getName() + " contains undefined parameter " + param.getName());
                    }
                }
                try {
                    ParameterSetXmlWriter.getInstance().write(config, configsDir.resolve(config.getName() + ".xml"));
                } catch (XMLStreamException | IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        try {
            if(!tasks.isEmpty()) {
                tasks.execute(FsTaskContext.builder().setTargetRoot(pkgDir.resolve(Constants.CONTENT)).build());
            }
            PackageXmlWriter.getInstance().write(pkgSpec, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        return pkgSpec;
    }
}
