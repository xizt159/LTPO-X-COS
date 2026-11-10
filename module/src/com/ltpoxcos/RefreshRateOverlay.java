package com.ltpoxcos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

final class RefreshRateOverlay {
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    // Matches Android Settings: 0/1 set the overlay state, and 2 queries it.
    private static final int TRANSACTION_SHOW_REFRESH_RATE = 1034;
    private static final int VALUE_QUERY = 2;
    private static final Pattern PARCEL = Pattern.compile("Parcel\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARCEL_WORD = Pattern.compile("\\b[0-9a-f]{8}\\b", Pattern.CASE_INSENSITIVE);

    static boolean isEnabled() throws IOException, InterruptedException {
        return parseEnabledState(runServiceCall(VALUE_QUERY));
    }

    static void setEnabled(boolean enabled) throws IOException, InterruptedException {
        runServiceCall(enabled ? 1 : 0);
        boolean actual = isEnabled();
        if (actual != enabled) {
            throw new IOException("SurfaceFlinger did not apply the requested refresh-rate overlay state");
        }
    }

    private static String runServiceCall(int value) throws IOException, InterruptedException {
        String command = "service call " + SURFACE_FLINGER + " "
                + TRANSACTION_SHOW_REFRESH_RATE + " i32 " + value;
        Process process = new ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Root command timed out");
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) output.append('\n');
                output.append(line);
            }
        }
        if (process.exitValue() != 0) {
            throw new IOException(output.length() == 0 ? "Root command failed" : output.toString());
        }
        return output.toString();
    }

    static boolean parseEnabledState(String output) throws IOException {
        Matcher parcel = PARCEL.matcher(output);
        if (!parcel.find()) {
            throw new IOException("Could not read the SurfaceFlinger refresh-rate overlay state: " + output);
        }
        Matcher word = PARCEL_WORD.matcher(parcel.group(1));
        String lastWord = null;
        while (word.find()) lastWord = word.group();
        if ("00000001".equalsIgnoreCase(lastWord)) return true;
        if ("00000000".equalsIgnoreCase(lastWord)) return false;
        throw new IOException("Unexpected SurfaceFlinger refresh-rate overlay response: " + output);
    }

    private RefreshRateOverlay() {}
}
