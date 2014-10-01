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
package org.wildfly.build.common.model;

import org.wildfly.build.util.xml.ParsingUtils;

import java.util.regex.Pattern;

/**
 * @author Stuart Douglas
 */
public class FileFilter {

    private final String patternString;
    private final Pattern pattern;
    private final boolean include;

    public FileFilter(String patternString, boolean include) {
        this.patternString = patternString;
        this.pattern = Pattern.compile(ParsingUtils.wildcardToJavaRegexp(patternString));
        this.include = include;
    }

    public String getPattern() {
        return patternString;
    }

    /**
     * Returns true if the file matches the regular expression
     */
    public boolean matches(final String filePath) {
        return pattern.matcher(filePath).matches();
    }

    public boolean isInclude() {
        return include;
    }


}
