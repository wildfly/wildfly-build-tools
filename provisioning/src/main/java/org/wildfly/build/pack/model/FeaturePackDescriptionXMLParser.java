/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.pack.model;

import org.jboss.staxmapper.XMLMapper;
import org.wildfly.build.util.PropertyResolver;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * @author Eduardo Martins
 */
public class FeaturePackDescriptionXMLParser {

    private static final QName ROOT_1_0 = new QName(FeaturePackDescriptionXMLParser10.NAMESPACE_1_0, FeaturePackDescriptionXMLParser10.Element.FEATURE_PACK.getLocalName());

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public FeaturePackDescriptionXMLParser(PropertyResolver properties) {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new FeaturePackDescriptionXMLParser10(properties));
    }

    public FeaturePackDescription parse(final InputStream input) throws XMLStreamException {
        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        FeaturePackDescription featurePackDescription = new FeaturePackDescription();
        mapper.parseDocument(featurePackDescription, streamReader);
        return featurePackDescription;
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
