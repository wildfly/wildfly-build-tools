package org.wildfly.build.util;

/**
 * Property resolver abstract
 *
 * @author Stuart Douglas
 */
public interface PropertyResolver {

    String resolveProperty(final String property);

    PropertyResolver NO_OP = new PropertyResolver() {
        @Override
        public String resolveProperty(String property) {
            return null;
        }
    };
}
