package com.refresh165;

final class BatteryDisplayPolicy {
    private static volatile boolean batterySaver;

    private BatteryDisplayPolicy() {}

    static int limit(int requestedRate) {
        return limit(requestedRate, Config.refresh("refresh_cap"),
                Config.validRate(Config.number("refresh_cap_rate", 120, 60, 165)),
                Config.refresh("battery_saver_cap"), batterySaver,
                Config.validRate(Config.number("battery_rate", 120, 60, 165)));
    }

    static int limit(int requestedRate, boolean generalEnabled, int generalCeiling,
            boolean batterySaverEnabled, boolean isBatterySaver, int batterySaverCeiling) {
        int limitedRate = generalEnabled ? Math.min(requestedRate, generalCeiling) : requestedRate;
        return batterySaverEnabled && isBatterySaver
                ? Math.min(limitedRate, batterySaverCeiling) : limitedRate;
    }

    static boolean updateBatterySaver(boolean enabled) {
        if (batterySaver == enabled) return false;
        batterySaver = enabled;
        return true;
    }

    static String status() {
        return "generalEnabled:"+Config.refresh("refresh_cap")
                +",generalCeiling:"+Config.number("refresh_cap_rate",120,60,165)
                +",batterySaverEnabled:"+Config.refresh("battery_saver_cap")
                +",batterySaver:"+batterySaver
                +",batterySaverCeiling:"+Config.number("battery_rate",120,60,165);
    }
}
