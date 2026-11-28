package com.ltpoxcos;

final class BatteryDisplayPolicy {
  private static volatile boolean batterySaver;

  private BatteryDisplayPolicy() {}

  static int limit(int requestedRate) {
    return Config.refresh("battery_saver_cap") && batterySaver
        ? Math.min(requestedRate, 60)
        : requestedRate;
  }

  static boolean updateBatterySaver(boolean enabled) {
    if (batterySaver == enabled) return false;
    batterySaver = enabled;
    return true;
  }

  static boolean capEnabled() {
    return Config.refresh("battery_saver_cap") && batterySaver;
  }

  static String status() {
    return "batterySaverEnabled:"
        + Config.refresh("battery_saver_cap")
        + ",batterySaver:"
        + batterySaver
        + ",batterySaverCeiling:60";
  }
}
