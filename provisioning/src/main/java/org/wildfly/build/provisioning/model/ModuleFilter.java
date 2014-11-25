package org.wildfly.build.provisioning.model;

import org.wildfly.build.util.xml.ParsingUtils;

import java.util.regex.Pattern;

/**
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ModuleFilter {

    private final String patternString;
    private final Pattern pattern;
    private final boolean include;
    private final boolean transitive;

    public ModuleFilter(String pattern, boolean include, boolean transitive) {
        if (pattern == null) {
            throw new IllegalArgumentException("null pattern");
        }
        this.patternString = pattern;
        this.pattern = Pattern.compile(ParsingUtils.wildcardToJavaRegexp(pattern));
        this.include = include;
        this.transitive = transitive;
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

    public boolean isTransitive() {
        return transitive;
    }

    public String getPattern() {
        return patternString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModuleFilter that = (ModuleFilter) o;

        if (!patternString.equals(that.patternString)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return patternString.hashCode();
    }

    @Override
    public String toString() {
        return patternString;
    }
}
