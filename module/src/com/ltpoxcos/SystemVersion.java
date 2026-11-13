package com.ltpoxcos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SystemVersion {
    private static final Pattern VERSION = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d+){0,3})(?!\\d)");
    private static final Pattern NAMED_ROM = Pattern.compile("(OxygenOS|ColorOS)[ _-]*(\\d+(?:\\.\\d+){0,3})", Pattern.CASE_INSENSITIVE);

    private SystemVersion() {}

    static String label(String oxygenOs, String oplusRom, String display, String androidRelease,
            String manufacturer) {
        String version = version(oxygenOs);
        if (version != null) return "OxygenOS " + version;

        Matcher named = NAMED_ROM.matcher(display == null ? "" : display);
        if (named.find()) return capitalize(named.group(1)) + " " + named.group(2);

        version = version(oplusRom);
        if (version != null) {
            boolean labelOxygenOs = isOnePlus(manufacturer) && majorVersion(version) < 17;
            return (labelOxygenOs ? "OxygenOS " : "ColorOS ") + version;
        }

        return androidRelease == null || androidRelease.isEmpty()
                ? "System version unavailable" : "Android " + androidRelease;
    }

    private static int majorVersion(String version) {
        int separator = version.indexOf('.');
        String major = separator < 0 ? version : version.substring(0, separator);
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isOnePlus(String manufacturer) {
        return manufacturer != null && manufacturer.toLowerCase(java.util.Locale.ROOT).contains("oneplus");
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
