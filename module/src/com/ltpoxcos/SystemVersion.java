package com.ltpoxcos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SystemVersion {
    private static final Pattern VERSION = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d+){0,3})(?!\\d)");
    private static final Pattern NAMED_ROM = Pattern.compile("(OxygenOS|ColorOS)[ _-]*(\\d+(?:\\.\\d+){0,3})", Pattern.CASE_INSENSITIVE);

    private SystemVersion() {}

    static String label(String oxygenOs, String oplusRom, String display, String androidRelease) {
        String version = version(oxygenOs);
        if (version != null) return "OxygenOS " + version;

        Matcher named = NAMED_ROM.matcher(display == null ? "" : display);
        if (named.find()) return capitalize(named.group(1)) + " " + named.group(2);

        version = version(oplusRom);
        if (version != null) return "ColorOS " + version;

        return androidRelease == null || androidRelease.isEmpty()
                ? "System version unavailable" : "Android " + androidRelease;
    }

    private static String version(String value) {
        if (value == null || value.isEmpty()) return null;
        Matcher matcher = VERSION.matcher(value);
        String dotted = null, integer = null;
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate.indexOf('.') >= 0) dotted = candidate;
            else integer = candidate;
        }
        return dotted != null ? dotted : integer;
    }

    private static String capitalize(String value) {
        return value.equalsIgnoreCase("oxygenos") ? "OxygenOS" : "ColorOS";
    }
}
