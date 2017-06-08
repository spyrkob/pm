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

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.test.util.fs.FsTaskContext;
import org.jboss.provisioning.test.util.fs.FsTaskList;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.PackageXmlWriter;

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
    private FsTaskList tasks;

    private PackageBuilder(FeaturePackBuilder fp, String name) {
        this.fp = fp;
        pkg = PackageSpec.builder(name);
    }

    private FsTaskList getTasks() {
        return tasks == null ? tasks = FsTaskList.newList() : tasks;
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
        getTasks().copy(src, relativeTarget);
        return this;
    }

    public PackageBuilder addDir(String relativeTarget, Path src, boolean contentOnly) {
        getTasks().copyDir(src, relativeTarget, contentOnly);
        return this;
    }

    public PackageBuilder writeContent(String relativeTarget, String content) {
        getTasks().write(content, relativeTarget);
        return this;
    }

    public PackageBuilder addParameter(String name, String defaultValue) {
        this.pkg.addParameter(name, defaultValue);
        return this;
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
        try {
            if(tasks != null && !tasks.isEmpty()) {
                tasks.execute(FsTaskContext.builder().setTargetRoot(pkgDir.resolve(Constants.CONTENT)).build());
            }
            PackageXmlWriter.getInstance().write(pkgSpec, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        return pkgSpec;
    }
}
