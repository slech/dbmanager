package de.slech.dbmanager.util;

import java.util.Collection;

/**
 * Utility class to concat strings.
 */
public class StringUtils {
    private StringUtils() {
        throw new UnsupportedOperationException("Keine Instanzierung möglich");
    }

    public static String concatStrings(Collection<String> list, final String delimiter) {
        StringBuilder sb = new StringBuilder();
        list.stream().filter(s ->!isBlank(s)).forEach(s -> sb.append(s).append(delimiter));
        if (sb.length() > 0) {
            sb.setLength(sb.length() - delimiter.length());
        }
        return sb.toString();
    }

    public static boolean isBlank(String str) {
        return str == null || str.chars().allMatch(i -> Character.isWhitespace((char) i));
    }

}
