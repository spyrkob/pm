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
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.PackageXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class PkgBuilder {

    public static PkgBuilder newInstance() {
        return newInstance(null);
    }

    public static PkgBuilder newInstance(FpBuilder fp) {
        return new PkgBuilder(fp);
    }

    private final FpBuilder fp;
    private boolean isDefault;
    private PackageDescription.Builder pkg = PackageDescription.builder();

    private PkgBuilder(FpBuilder fp) {
        this.fp = fp;
    }

    public FpBuilder getFeaturePack() {
        return fp;
    }

    public PkgBuilder setDefault() {
        isDefault = true;
        return this;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public PkgBuilder setName(String name) {
        pkg.setName(name);
        return this;
    }

    public PkgBuilder addDependency(String dep) {
        this.pkg.addDependency(dep);
        return this;
    }

    PackageDescription write(Path fpDir) {
        final PackageDescription pkgDescr = pkg.build();
        final Path pkgDir;
        try {
            pkgDir = LayoutUtils.getPackageDir(fpDir, pkgDescr.getName(), false);
        } catch (ProvisioningDescriptionException e) {
            throw new IllegalStateException(e);
        }
        TestFiles.mkdirs(pkgDir);
        try {
            PackageXmlWriter.INSTANCE.write(pkgDescr, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        return pkgDescr;
    }
}
