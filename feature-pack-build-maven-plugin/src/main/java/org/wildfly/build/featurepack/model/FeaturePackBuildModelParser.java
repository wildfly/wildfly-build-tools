package org.wildfly.build.featurepack.model;

import java.io.InputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.wildfly.build.util.PropertyResolver;

/**
 * @author Stuart Douglas
 */
public class FeaturePackBuildModelParser {

    private static final QName ROOT_1_0 = new QName(FeaturePackBuildModelParser10.NAMESPACE_1_0, FeaturePackBuildModelParser10.Element.BUILD.getLocalName());

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public FeaturePackBuildModelParser(PropertyResolver properties) {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new FeaturePackBuildModelParser10(properties));
    }

    public FeaturePackBuild parse(final InputStream input) throws XMLStreamException {

        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        FeaturePackBuild build = new FeaturePackBuild();
        mapper.parseDocument(build, streamReader);
        return build;
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
