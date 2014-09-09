/*
 * Copyright 2014 Red Hat, Inc.
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
