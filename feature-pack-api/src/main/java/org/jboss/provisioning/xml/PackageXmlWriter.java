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
package org.jboss.provisioning.xml;

import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.xml.PackageXmlParser10.Attribute;
import org.jboss.provisioning.xml.PackageXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageXmlWriter extends BaseXmlWriter<PackageSpec> {

    private static final String TRUE = "true";

    private static final PackageXmlWriter INSTANCE = new PackageXmlWriter();

    public static PackageXmlWriter getInstance() {
        return INSTANCE;
    }

    private PackageXmlWriter() {
    }

    protected ElementNode toElement(PackageSpec pkgSpec) {

        final ElementNode pkg = addElement(null, Element.PACKAGE_SPEC);
        addAttribute(pkg, Attribute.NAME, pkgSpec.getName());

        ElementNode deps = null;
        if(pkgSpec.dependsOnLocalPackages()) {
            deps = addElement(pkg, Element.DEPENDENCIES);
            for(PackageDependencySpec depSpec : pkgSpec.getLocalPackageDependencies().getDescriptions()) {
                writePackageDependency(deps, depSpec);
            }
        }
        if(pkgSpec.dependsOnExternalPackages()) {
            if(deps == null) {
                deps = addElement(pkg, Element.DEPENDENCIES);
            }
            for(String name : pkgSpec.getPackageDependencySources()) {
                writeFeaturePackDependency(deps, pkgSpec.getExternalPackageDependencies(name));
            }
        }
        if(pkgSpec.hasParameters()) {
            PackageParametersXml.write(pkg, pkgSpec.getParameters());
        }

        return pkg;
    }

    private static void writeFeaturePackDependency(ElementNode deps, PackageDependencyGroupSpec depGroupSpec) {
        final ElementNode fpElement = addElement(deps, Element.FEATURE_PACK);
        addAttribute(fpElement, Attribute.DEPENDENCY, depGroupSpec.getGroupName());
        for(PackageDependencySpec depSpec : depGroupSpec.getDescriptions()) {
            writePackageDependency(fpElement, depSpec);
        }
    }

    private static void writePackageDependency(ElementNode deps, PackageDependencySpec depSpec) {
        final ElementNode depElement = addElement(deps, Element.PACKAGE);
        addAttribute(depElement, Attribute.NAME, depSpec.getName());
        if(depSpec.isOptional()) {
            addAttribute(depElement, Attribute.OPTIONAL, TRUE);
        }
        if(depSpec.hasParams()) {
            PackageParametersXml.write(depElement, depSpec.getParameters());
        }
    }
}
