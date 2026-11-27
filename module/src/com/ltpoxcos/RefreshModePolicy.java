package com.ltpoxcos;

final class RefreshModePolicy {
    private RefreshModePolicy() {}

    static boolean shouldOverride(boolean enabled, int selectedMode, int standardMode) {
        return enabled && standardMode > 0 && (selectedMode == 0 || selectedMode == standardMode);
    }
}
