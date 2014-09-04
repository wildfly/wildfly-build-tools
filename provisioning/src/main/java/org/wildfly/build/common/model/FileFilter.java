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
