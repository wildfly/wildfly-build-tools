package org.wildfly.build.pack.model;

import java.util.regex.Pattern;

/**
 * @author Stuart Douglas
 */
public class FileFilter {

    private final Pattern pattern;
    private final boolean include;

    public FileFilter(String pattern, boolean include) {
        this.pattern = Pattern.compile(pattern);
        this.include = include;
    }

    public Pattern getPattern() {
        return pattern;
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
