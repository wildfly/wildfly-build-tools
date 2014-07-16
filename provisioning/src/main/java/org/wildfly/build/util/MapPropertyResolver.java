package org.wildfly.build.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Stuart Douglas
 */
public class MapPropertyResolver implements PropertyResolver {

    private final Map<String, String> props;

    public MapPropertyResolver(Map<String, String> props) {
        this.props = props;
    }

    public MapPropertyResolver(Properties properties) {
        this.props = new HashMap<>();
        for(Map.Entry<Object, Object> p : properties.entrySet()) {
            props.put(p.getKey().toString(), p.getValue() == null ? null: p.getValue().toString());
        }
    }

    @Override
    public String resolveProperty(String property) {
        return props.get(property);
    }
}
