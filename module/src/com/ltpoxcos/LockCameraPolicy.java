package com.ltpoxcos;

import java.util.Set;

final class LockCameraPolicy {
  private LockCameraPolicy() {}

  static boolean active(
      boolean enabled,
      boolean interactive,
      boolean keyguardLocked,
      String foregroundPackage,
      Set<String> cameraPackages) {
    return enabled
        && interactive
        && (keyguardLocked
            || (foregroundPackage != null && cameraPackages.contains(foregroundPackage)));
  }

  static int ceiling(int requestedRate, boolean active) {
    return active ? Math.min(requestedRate, 120) : requestedRate;
  }
}
