package com.ltpoxcos;

public class SystemVersionTest {
    public static void main(String[] args) {
        yes("OxygenOS 16.0.0.201".equals(SystemVersion.label("16.0.0.201", "17.0.0.102", "", "16")));
        yes("ColorOS 17.0.0.102".equals(SystemVersion.label("", "17.0.0.102(EX01)", "", "16")));
        yes("OxygenOS 16.0.0.201".equals(SystemVersion.label("", "17.0.0.102", "OxygenOS_16.0.0.201", "16")));
        yes("Android 16".equals(SystemVersion.label("", "", "CPH2745_16.0.0.201(EX01)", "16")));
        yes("System version unavailable".equals(SystemVersion.label("", "", "", "")));
        System.out.println("PASS 5 system-version assertions");
    }

    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("system-version assertion failed");
    }
}
