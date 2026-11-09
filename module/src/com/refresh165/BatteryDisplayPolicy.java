package com.refresh165;

final class BatteryDisplayPolicy {
    private static volatile int percent = -1;
    private static volatile boolean plugged;

    private BatteryDisplayPolicy() {}

    static boolean update(int level, int scale, boolean isPlugged) {
        int nextPercent = scale > 0 && level >= 0 && level <= scale
                ? (int) (((long) level * 100 + scale / 2) / scale) : -1;
        if (percent == nextPercent && plugged == isPlugged) return false;
        percent = nextPercent;
        plugged = isPlugged;
        return true;
    }

    static int limit(int requestedRate) {
        return limit(requestedRate, Config.refresh("battery_profile"), percent, plugged,
                Config.number("battery_threshold", 20, 5, 50),
                Config.validRate(Config.number("battery_rate", 120, 60, 165)));
    }

    static int limit(int requestedRate, boolean enabled, int batteryPercent, boolean isPlugged,
            int threshold, int ceiling) {
        if (!enabled || isPlugged || batteryPercent < 0 || batteryPercent > threshold) return requestedRate;
        return Math.min(requestedRate, ceiling);
    }

    static int percent() { return percent; }
    static boolean plugged() { return plugged; }
    static String status() {
        return "enabled:"+Config.refresh("battery_profile")+",percent:"+percent+",plugged:"+plugged
                +",threshold:"+Config.number("battery_threshold",20,5,50)
                +",ceiling:"+Config.number("battery_rate",120,60,165);
    }
}
