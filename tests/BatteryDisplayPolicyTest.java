package com.ltpoxcos;

public class BatteryDisplayPolicyTest {
    public static void main(String[] args) {
        yes(BatteryDisplayPolicy.limit(165, false, 120, false, false, 90) == 165);
        yes(BatteryDisplayPolicy.limit(165, true, 120, false, false, 90) == 120);
        yes(BatteryDisplayPolicy.limit(165, false, 120, true, false, 90) == 165);
        yes(BatteryDisplayPolicy.limit(165, false, 120, true, true, 90) == 90);
        yes(BatteryDisplayPolicy.limit(165, true, 120, true, true, 90) == 90);
        yes(BatteryDisplayPolicy.limit(165, true, 120, true, true, 144) == 120);
        yes(BatteryDisplayPolicy.limit(90, true, 120, true, true, 120) == 90);
        yes(BatteryDisplayPolicy.limit(165, true, 165, false, false, 90) == 165);
        yes(BatteryDisplayPolicy.limit(60, true, 120, true, true, 90) == 60);
        System.out.println("PASS 9 independent refresh-cap assertions");
    }

    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("refresh-cap assertion failed");
    }
}
