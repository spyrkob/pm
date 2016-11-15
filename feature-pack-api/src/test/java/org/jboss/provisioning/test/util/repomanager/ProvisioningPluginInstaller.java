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

package org.jboss.provisioning.test.util.repomanager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningPluginInstaller {

    public static ProvisioningPluginInstaller forCoords(String coords) {
        return forCoords(ArtifactCoords.fromString(coords));
    }

    public static ProvisioningPluginInstaller forCoords(ArtifactCoords coords) {
        return new ProvisioningPluginInstaller(coords);
    }

    private final ArtifactCoords coords;
    private Set<Class<?>> classes = Collections.emptySet();
    private Map<String, Set<String>> services = Collections.emptyMap();

    private ProvisioningPluginInstaller(ArtifactCoords coords) {
        this.coords = coords;
    }

    public ProvisioningPluginInstaller addClass(Class<?> cls) {
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

    public ProvisioningPluginInstaller addPlugin(Class<?> pluginCls) {
        return addService(ProvisioningPlugin.class, pluginCls);
    }

    public ProvisioningPluginInstaller addService(Class<?> serviceInterface, Class<?> serviceImpl) {
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
        addClass(serviceImpl);
        return this;
    }

    public Path install(Path repoHome) throws IOException {
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

            ensureDir(repoHome);
            final Path artifactPath = FeaturePackBuilder.getArtifactPath(repoHome, coords);
            ensureDir(artifactPath.getParent());
            ZipUtils.zip(tmpDir, artifactPath);
            return artifactPath;
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
