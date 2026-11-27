package com.ltpoxcos;

public class BatteryDisplayPolicyTest {
    public static void main(String[] args) {
        Config.flags.put("battery_saver_cap", true);
        yes(BatteryDisplayPolicy.limit(165) == 165);
        yes(BatteryDisplayPolicy.updateBatterySaver(true));
        yes(BatteryDisplayPolicy.capEnabled());
        yes(BatteryDisplayPolicy.limit(165) == 60);
        yes(BatteryDisplayPolicy.limit(120) == 60);
        yes(BatteryDisplayPolicy.limit(60) == 60);
        Config.flags.put("battery_saver_cap", false);
        yes(BatteryDisplayPolicy.limit(165) == 165);
        yes(!BatteryDisplayPolicy.capEnabled());
        yes(BatteryDisplayPolicy.updateBatterySaver(false));
        System.out.println("PASS 9 Battery Saver ceiling assertions");
    }

    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("refresh-cap assertion failed");
    }
}
