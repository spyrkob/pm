/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.cli;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactRepositoryManager;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author jdenise@redhat.com
 */
public class Universe {

    public static final String NS = "urn:jboss:universe:1.0";
    private static final String UNIVERSE = "universe";

    public static class StreamLocation {

        private final String name;
        private final ArtifactCoords coordinates;

        private StreamLocation(String name, ArtifactCoords coordinates) {
            this.name = name;
            this.coordinates = coordinates;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the coordinates
         */
        public ArtifactCoords getCoordinates() {
            return coordinates;
        }
    }

    static class UniverseReader implements XMLElementReader<Universe> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, Universe universe) throws XMLStreamException {
            String localName = reader.getLocalName();
            if (!UNIVERSE.equals(localName)) {
                throw new XMLStreamException("Unexpected element: " + localName);
            }
            readUniverseElement_1_0(reader, universe);
        }

        public void readUniverseElement_1_0(XMLExtendedStreamReader reader, Universe universe) throws XMLStreamException {
            boolean universeEnded = false;
            while (reader.hasNext() && universeEnded == false) {
                int tag = reader.nextTag();
                if (tag == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals("stream")) {
                        // For now, stream reference the feature pack directly.
                        String groupId = reader.getAttributeValue(null, "group-id");
                        String artefactId = reader.getAttributeValue(null, "artefact-id");
                        // TODO, NO NEED FOR VERSION MUST REMOVE
                        String version = reader.getAttributeValue(null, "version");
                        String name = reader.getAttributeValue(null, "name");
                        universe.addStreamLocation(new StreamLocation(name, ArtifactCoords.newGav(groupId, artefactId, version).toArtifactCoords()));
                    } else {
                        throw new XMLStreamException("Unexpected element: " + localName);
                    }
                } else if (tag == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals("universe")) {
                        universeEnded = true;
                    }
                }
            }
        }
    }

    private final Map<String, StreamLocation> streamLocations = new HashMap<>();
    private final UniverseLocation location;

    private Universe(UniverseLocation location) {
        this.location = location;
    }

    private void addStreamLocation(StreamLocation location) {
        streamLocations.put(location.getName(), location);
    }

    public UniverseLocation getLocation() {
        return location;
    }

    public Collection<StreamLocation> getStreamLocations() {
        return Collections.unmodifiableCollection(streamLocations.values());
    }

    public ArtifactCoords resolveStream(String name) {
        StreamLocation loc = streamLocations.get(name);
        if (loc == null) {
            throw new RuntimeException("Unknown stream " + name);
        }
        return loc.getCoordinates();
    }

    static Universe buildUniverse(ArtifactRepositoryManager manager,
            UniverseLocation location) throws Exception {
        Universe universe = new Universe(location);
        Path p = manager.resolve(location.getCoordinates());
        try (JarFile jarFile = new JarFile(p.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().equals("universe.xml")) {
                    InputStream input = jarFile.getInputStream(entry);
                    return parse(input, universe);
                }
            }
        }
        throw new Exception("Universe content not found");
    }

    private static Universe parse(InputStream input, Universe universe) throws Exception {
        final XMLMapper mapper = XMLMapper.Factory.create();

        final XMLElementReader<Universe> reader = new UniverseReader();
        mapper.registerRootElement(new QName(NS, UNIVERSE), reader);
        XMLStreamReader universeReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
        mapper.parseDocument(universe, universeReader);
        universeReader.close();
        return universe;
    }
}
