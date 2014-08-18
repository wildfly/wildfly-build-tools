package org.wildfly.build.provisioning.model;

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
public class ServerProvisioningDescriptionModelParser {

    private static final QName ROOT_1_0 = new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, ServerProvisioningDescriptionModelParser10.Element.SERVER_PROVISIONING.getLocalName());

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public ServerProvisioningDescriptionModelParser(PropertyResolver properties) {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new ServerProvisioningDescriptionModelParser10(properties));
    }

    public ServerProvisioningDescription parse(final InputStream input) throws XMLStreamException {

        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescription();
        mapper.parseDocument(serverProvisioningDescription, streamReader);
        return serverProvisioningDescription;
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
