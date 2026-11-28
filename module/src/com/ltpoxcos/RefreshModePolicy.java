package com.ltpoxcos;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

final class RefreshModePolicy {
  private static final String TAG = "LTPO-X-COS";
  private static final String USER_REFRESH_RATE = "user_refresh_rate";
  private static final int HIGH_RATE = 165;

  private RefreshModePolicy() {}

  static void sync(Context context, boolean enabled) {
    try {
      ContentResolver resolver = context.getContentResolver();
      Bundle saved = resolver.call(Config.URI, "refresh_mode_state", "read", null);
      if (saved == null) {
        throw new IllegalStateException("Refresh-mode state is unavailable");
      }
      if (enabled) {
        if (!saved.getBoolean("active")) {
          Bundle previous = new Bundle();
          String peak =
              Settings.System.getStringForUser(
                  resolver, Settings.System.PEAK_REFRESH_RATE, UserHandle.USER_CURRENT);
          String user =
              Settings.System.getStringForUser(
                  resolver, USER_REFRESH_RATE, UserHandle.USER_CURRENT);
          previous.putBoolean("peak_present", peak != null);
          previous.putString("peak", peak);
          previous.putBoolean("user_present", user != null);
          previous.putString("user", user);
          saved = resolver.call(Config.URI, "refresh_mode_state", "save", previous);
          if (saved == null || !saved.getBoolean("active")) {
            throw new IllegalStateException("Could not save previous refresh-mode settings");
          }
        }
        put(resolver, Settings.System.PEAK_REFRESH_RATE, String.valueOf((float) HIGH_RATE));
        put(resolver, USER_REFRESH_RATE, String.valueOf(HIGH_RATE));
      } else if (saved.getBoolean("active")) {
        String previousPeak =
            saved.getBoolean("peak_present") ? saved.getString("peak", null) : null;
        String previousUser =
            saved.getBoolean("user_present") ? saved.getString("user", null) : null;
        put(resolver, Settings.System.PEAK_REFRESH_RATE, previousPeak);
        put(resolver, USER_REFRESH_RATE, previousUser);
        resolver.call(Config.URI, "refresh_mode_state", "clear", null);
      }
    } catch (RuntimeException error) {
      Log.e(TAG, "Could not synchronize the system refresh-rate setting", error);
    }
  }

  private static void put(ContentResolver resolver, String key, String value) {
    if (!Settings.System.putStringForUser(resolver, key, value, UserHandle.USER_CURRENT)) {
      throw new IllegalStateException("Could not update system setting: " + key);
    }
  }
}
