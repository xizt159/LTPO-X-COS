package com.refresh165;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

final class RefreshRateOverlay {
    static final String SETTING = "show_refresh_rate";

    static void setEnabled(boolean enabled) throws IOException, InterruptedException {
        String value = enabled ? "1" : "0";
        String command = "settings put global " + SETTING + " " + value
                + " || exit 1; actual=$(settings get global " + SETTING + "); "
                + "if [ \"$actual\" != \"" + value + "\" ]; then "
                + "echo \"Expected " + value + ", got: $actual\"; exit 1; fi";
        Process process = new ProcessBuilder(
                "su", "-c", command)
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
    }

    private RefreshRateOverlay() {}
}
