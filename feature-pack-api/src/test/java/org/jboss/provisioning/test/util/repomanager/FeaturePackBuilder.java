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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.FeatureSpec;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.ConfigXmlWriter;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;
import org.jboss.provisioning.xml.FeatureSpecXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackBuilder {

    static Path getArtifactPath(Path repoHome, final ArtifactCoords coords) {
        Path p = repoHome;
        final String[] groupParts = coords.getGroupId().split("\\.");
        for (String part : groupParts) {
            p = p.resolve(part);
        }
        p = p.resolve(coords.getArtifactId());
        p = p.resolve(coords.getVersion());
        final StringBuilder fileName = new StringBuilder();
        fileName.append(coords.getArtifactId()).append('-').append(coords.getVersion()).append('.').append(coords.getExtension());
        return p.resolve(fileName.toString());
    }

    public static FeaturePackBuilder newInstance() {
        return newInstance(null);
    }

    public static FeaturePackBuilder newInstance(FeaturePackRepoManager.Installer installer) {
        return new FeaturePackBuilder(installer);
    }

    private final FeaturePackRepoManager.Installer installer;
    private final FeaturePackSpec.Builder fpBuilder = FeaturePackSpec.builder();
    private List<PackageBuilder> pkgs = Collections.emptyList();
    private Set<Class<?>> classes = Collections.emptySet();
    private Map<String, Set<String>> services = Collections.emptyMap();
    private String pluginFileName = "plugins.jar";
    private Map<String, Config> configs = Collections.emptyMap();
    private Map<String, FeatureSpec> specs = Collections.emptyMap();

    protected FeaturePackBuilder(FeaturePackRepoManager.Installer repo) {
        this.installer = repo;
    }

    public FeaturePackRepoManager.Installer getInstaller() {
        return installer;
    }

    public FeaturePackBuilder setGav(ArtifactCoords.Gav gav) {
        fpBuilder.setGav(gav);
        return this;
    }

    public FeaturePackBuilder addDependency(String name, FeaturePackConfig dep) throws ProvisioningDescriptionException {
        fpBuilder.addDependency(name, dep);
        return this;
    }

    public FeaturePackBuilder addDependency(FeaturePackConfig dep) throws ProvisioningDescriptionException {
        return addDependency(null, dep);
    }

    public FeaturePackBuilder addDependency(ArtifactCoords.Gav gav) throws ProvisioningDescriptionException {
        return addDependency(FeaturePackConfig.forGav(gav));
    }

    public FeaturePackBuilder addDependency(String name, ArtifactCoords.Gav gav) throws ProvisioningDescriptionException {
        return addDependency(name, FeaturePackConfig.forGav(gav));
    }

    public FeaturePackBuilder addPackage(PackageBuilder pkg) {
        switch (pkgs.size()) {
            case 0:
                pkgs = Collections.singletonList(pkg);
                break;
            case 1:
                pkgs = new ArrayList<>(pkgs);
            default:
                pkgs.add(pkg);
        }
        return this;
    }

    public PackageBuilder newPackage(String name) {
        return newPackage(name, false);
    }

    public PackageBuilder newPackage(String name, boolean isDefault) {
        final PackageBuilder pkg = PackageBuilder.newInstance(this, name);
        if(isDefault) {
            pkg.setDefault();
        }
        addPackage(pkg);
        return pkg;
    }

    public FeaturePackBuilder addSpec(FeatureSpec spec) throws ProvisioningDescriptionException {
        if(specs.isEmpty()) {
            specs = Collections.singletonMap(spec.getName(), spec);
        } else {
            if(specs.containsKey(spec.getName())) {
                throw new ProvisioningDescriptionException("Duplicate spec name " + spec.getName() + " for " + fpBuilder.getGav());
            }
            if(specs.size() == 1) {
                specs = new HashMap<>(specs);
            }
            specs.put(spec.getName(), spec);
        }
        return this;
    }

    public FeaturePackBuilder addConfig(Config config) throws ProvisioningDescriptionException {
        return addConfig(config, false);
    }

    public FeaturePackBuilder addConfig(Config config, boolean defaultConfig) throws ProvisioningDescriptionException {
        if(config.getName() == null) {
            if(defaultConfig) {
                throw new ProvisioningDescriptionException("default config must have a name");
            }
            fpBuilder.setConfig(config);
            return this;
        }
        if(configs.isEmpty()) {
            configs = Collections.singletonMap(config.getName(), config);
        } else {
            if(configs.containsKey(config.getName())) {
                throw new ProvisioningDescriptionException("Duplicate config name " + config.getName() + " for " + fpBuilder.getGav());
            }
            if(configs.size() == 1) {
                configs = new HashMap<>(configs);
            }
            configs.put(config.getName(), config);
        }
        if(defaultConfig) {
            fpBuilder.addDefaultConfig(config.getName());
        }
        return this;
    }

    public FeaturePackBuilder setPluginFileName(String pluginFileName) {
        this.pluginFileName = pluginFileName;
        return this;
    }

    public FeaturePackBuilder addClassToPlugin(Class<?> cls) {
        if(classes.contains(cls)) {
            return this;
        }
        switch(classes.size()) {
            case 0:
                classes = Collections.singleton(cls);
                break;
            case 1:
                classes = new HashSet<>(classes);
            default:
                classes.add(cls);
        }
        return this;
    }

    public FeaturePackBuilder addPlugin(Class<?> pluginCls) {
        return addService(ProvisioningPlugin.class, pluginCls);
    }

    public FeaturePackBuilder addService(Class<?> serviceInterface, Class<?> serviceImpl) {
        final String serviceName = serviceInterface.getName();
        Set<String> implSet = services.get(serviceName);
        if(implSet == null) {
            switch(services.size()) {
                case 0:
                    services = Collections.singletonMap(serviceName, Collections.singleton(serviceImpl.getName()));
                    break;
                case 1:
                    services = new HashMap<>(services);
                default:
                    services.put(serviceName, Collections.singleton(serviceImpl.getName()));
            }
        } else {
            if(implSet.contains(serviceImpl.getName())) {
                return this;
            }
            if(implSet.size() == 1) {
                implSet = new HashSet<>(implSet);
                implSet.add(serviceImpl.getName());
                if(services.size() == 1) {
                    services = Collections.singletonMap(serviceName, implSet);
                } else {
                    services.put(serviceName, implSet);
                }
            } else {
                implSet.add(serviceImpl.getName());
            }
        }
        addClassToPlugin(serviceImpl);
        return this;
    }

    public FeaturePackSpec build(Path repoHome) throws ProvisioningDescriptionException {
        final Path fpWorkDir = TestUtils.mkRandomTmpDir();
        final FeaturePackSpec fpSpec;
        try {
            for (PackageBuilder pkg : pkgs) {
                final PackageSpec pkgDescr = pkg.build(fpWorkDir);
                if(pkg.isDefault()) {
                    fpBuilder.addDefaultPackage(pkgDescr.getName());
                }
            }

            if(!specs.isEmpty()) {
                final Path featuresDir = fpWorkDir.resolve(Constants.FEATURES);
                final FeatureSpecXmlWriter specWriter = FeatureSpecXmlWriter.getInstance();
                for(FeatureSpec spec : specs.values()) {
                    final Path featureDir = featuresDir.resolve(spec.getName());
                    ensureDir(featureDir);
                    specWriter.write(spec, featureDir.resolve(Constants.SPEC_XML));
                }
            }

            if(!configs.isEmpty()) {
                final Path configsDir = fpWorkDir.resolve(Constants.CONFIGS);
                ensureDir(configsDir);
                final ConfigXmlWriter configWriter = ConfigXmlWriter.getInstance();
                for(Config config : configs.values()) {
                    configWriter.write(config, configsDir.resolve(config.getName() + Constants.DOT_XML));
                }
            }

            if(!classes.isEmpty()) {
                addPlugins(fpWorkDir);
            }
            fpSpec = fpBuilder.build();
            final FeaturePackXmlWriter writer = FeaturePackXmlWriter.getInstance();
            writer.write(fpSpec, fpWorkDir.resolve(Constants.FEATURE_PACK_XML));

            final Path fpZip;
            fpZip = getArtifactPath(repoHome, fpSpec.getGav().toArtifactCoords());
            TestUtils.mkdirs(fpZip.getParent());
            ZipUtils.zip(fpWorkDir, fpZip);
            return fpSpec;
        } catch(ProvisioningDescriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            IoUtils.recursiveDelete(fpWorkDir);
        }
    }

    private void addPlugins(Path fpDir) throws IOException {
        final Path tmpDir = IoUtils.createRandomTmpDir();
        try {
            byte[] bytes = new byte[65536];
            for(Class<?> cls : classes) {
                Path p = tmpDir;
                final String[] parts = cls.getName().split("\\.");
                int i = 0;
                while(i < parts.length - 1) {
                    p = p.resolve(parts[i++]);
                }
                p = p.resolve(parts[i] + ".class");
                Files.createDirectories(p.getParent());

                final InputStream is = cls.getClassLoader().getResourceAsStream(tmpDir.relativize(p).toString());
                if(is == null) {
                    throw new IOException("Failed to locate " + tmpDir.relativize(p));
                }
                try (OutputStream os = Files.newOutputStream(p)) {
                    int rc;
                    while ((rc = is.read(bytes)) != -1) {
                        os.write(bytes, 0, rc);
                    }
                    os.flush();
                } finally {
                    try {
                        is.close();
                    } catch(IOException e) {
                    }
                }
            }

            if(!services.isEmpty()) {
                final Path servicesDir = tmpDir.resolve("META-INF").resolve("services");
                Files.createDirectories(servicesDir);
                for(Map.Entry<String, Set<String>> entry : services.entrySet()) {
                    final Path service = servicesDir.resolve(entry.getKey());
                    try(BufferedWriter writer = Files.newBufferedWriter(service)) {
                        for(String impl : entry.getValue()) {
                            writer.write(impl);
                            writer.newLine();
                        }
                    }
                }
            }

            final Path pluginsDir = fpDir.resolve(Constants.PLUGINS);
            ensureDir(pluginsDir);
            ZipUtils.zip(tmpDir, pluginsDir.resolve(pluginFileName));
        } finally {
            IoUtils.recursiveDelete(tmpDir);
        }
    }

    private void ensureDir(Path dir) throws IOException {
        if(!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if(!Files.isDirectory(dir)) {
            throw new IllegalStateException(dir + " is not a directory.");
        }
    }
}
