package com.ltpoxcos;

public class SystemVersionTest {
    public static void main(String[] args) {
        yes("OxygenOS 16.0.0.201".equals(SystemVersion.label("16.0.0.201", "17.0.0.102", "", "16", "OnePlus")));
        yes("ColorOS 17.0.0.102".equals(SystemVersion.label("", "17.0.0.102(EX01)", "", "16", "OPPO")));
        yes("OxygenOS 16.0.0.201".equals(SystemVersion.label("", "16.0.0.201(EX01)", "", "16", "OnePlus")));
        yes("ColorOS 17.0.0.102".equals(SystemVersion.label("", "17.0.0.102(EX01)", "", "16", "OnePlus")));
        yes("OxygenOS 16.0.0.201".equals(SystemVersion.label("", "17.0.0.102", "OxygenOS_16.0.0.201", "16", "OPPO")));
        yes("ColorOS 17.0.0.102".equals(SystemVersion.label("", "17.0.0.102", "ColorOS_17.0.0.102", "16", "OnePlus")));
        yes("Android 16".equals(SystemVersion.label("", "", "CPH2745_16.0.0.201(EX01)", "16", "OnePlus")));
        yes("System version unavailable".equals(SystemVersion.label("", "", "", "", "")));
        System.out.println("PASS 8 system-version assertions");
    }

    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("system-version assertion failed");
    }
}
