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

package org.jboss.provisioning.test.util;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.test.util.fs.FsTaskContext;
import org.jboss.provisioning.test.util.fs.FsTaskList;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.PackageXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageBuilder {

    public static PackageBuilder newInstance() {
        return newInstance(null);
    }

    public static PackageBuilder newInstance(FeaturePackBuilder fp) {
        return new PackageBuilder(fp);
    }

    private final FeaturePackBuilder fp;
    private boolean isDefault;
    private final PackageDescription.Builder pkg = PackageDescription.builder();
    private final FsTaskList tasks = FsTaskList.newList();

    private PackageBuilder(FeaturePackBuilder fp) {
        this.fp = fp;
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

    public PackageBuilder setName(String name) {
        pkg.setName(name);
        return this;
    }

    public PackageBuilder addDependency(String dep) {
        this.pkg.addDependency(dep);
        return this;
    }

    public PackageBuilder addPath(Path src, String relativeTarget) {
        tasks.copy(src, relativeTarget);
        return this;
    }

    public PackageBuilder addDir(Path src, String relativeTarget, boolean contentOnly) {
        tasks.copyDir(src, relativeTarget, contentOnly);
        return this;
    }

    public PackageBuilder writeContent(String content, String relativeTarget) {
        tasks.write(content, relativeTarget);
        return this;
    }

    public PackageDescription build(Path fpDir) {
        final PackageDescription pkgDescr = pkg.build();
        final Path pkgDir;
        try {
            pkgDir = LayoutUtils.getPackageDir(fpDir, pkgDescr.getName(), false);
        } catch (ProvisioningDescriptionException e) {
            throw new IllegalStateException(e);
        }
        TestUtils.mkdirs(pkgDir);
        try {
            if(!tasks.isEmpty()) {
                tasks.execute(FsTaskContext.builder().setTargetRoot(pkgDir.resolve(Constants.CONTENT)).build());
            }
            PackageXmlWriter.INSTANCE.write(pkgDescr, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        return pkgDescr;
    }
}
