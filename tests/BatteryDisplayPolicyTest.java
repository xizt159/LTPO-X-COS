package com.refresh165;

public class BatteryDisplayPolicyTest {
    public static void main(String[] args) {
        yes(BatteryDisplayPolicy.limit(165, false, 10, false, 20, 120) == 165);
        yes(BatteryDisplayPolicy.limit(165, true, 21, false, 20, 120) == 165);
        yes(BatteryDisplayPolicy.limit(165, true, 20, false, 20, 120) == 120);
        yes(BatteryDisplayPolicy.limit(165, true, 5, false, 20, 90) == 90);
        yes(BatteryDisplayPolicy.limit(90, true, 5, false, 20, 120) == 90);
        yes(BatteryDisplayPolicy.limit(165, true, 5, true, 20, 120) == 165);
        yes(BatteryDisplayPolicy.limit(165, true, -1, false, 20, 120) == 165);
        yes(BatteryDisplayPolicy.update(15, 100, false));
        yes(BatteryDisplayPolicy.percent() == 15 && !BatteryDisplayPolicy.plugged());
        yes(!BatteryDisplayPolicy.update(15, 100, false));
        yes(BatteryDisplayPolicy.update(15, 100, true));
        yes(BatteryDisplayPolicy.plugged());
        yes(BatteryDisplayPolicy.update(101, 100, false));
        yes(BatteryDisplayPolicy.percent() == -1);
        System.out.println("PASS 12 low-battery display-profile assertions");
    }

    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("battery-profile assertion failed");
    }
}
