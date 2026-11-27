# LTPO-X (COS)

Adaptive refresh-rate policies and Always-On Display controls for compatible OnePlus and OPPO devices running OxygenOS or ColorOS.

**Current release:** `1.0.1` · **Package:** `com.ltpoxcos`

[Download APK](https://github.com/xizt159/LTPO-X-COS/releases/latest) · [Releases](https://github.com/xizt159/LTPO-X-COS/releases) · [Changelog](CHANGELOG.md)

## Compatibility

- Compatible OnePlus and OPPO devices depend on their model, firmware, and vendor display APIs.
- Reference device: OnePlus 15 / CPH2745 on ColorOS 17. OxygenOS 16 has also been reported working.
- Requires a rooted device with an Xposed-compatible runtime that supports modern libxposed API 102. Magisk or KernelSU alone is not a substitute for that runtime.
- The module changes Android/OEM Java behavior; it does not install panel timings or modify the kernel.

## Display and refresh rate

| Feature                | Behavior                                                                                                                                                               |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Maximum refresh rate   | Choose 60, 90, 120, 144, or 165 Hz; 185 Hz is offered when supported by the device. LTPO can still lower the rate while idle.                                          |
| System display setting | Select **Standard (60 Hz)** to enable LTPO-X's maximum-rate policy. Custom system refresh modes are left unchanged.                                                    |
| Battery Saver cap      | Optional 60 Hz maximum while the device's own Battery Saver is active; adaptive LTPO idle remains available.                                                           |
| Lock screen/camera cap | Optional 120 Hz maximum while the keyguard is locked or a detected camera app is foreground; the camera viewfinder remains camera-controlled.                          |
| Adaptive idle          | 60/90/120/144 Hz modes can reach 1 Hz when firmware permits. At 165 Hz, the idle floor varies by firmware (about 33–55 Hz). The 144-to-1 Hz path remains experimental. |
| Other controls         | Low-brightness idle, recognized idle tables, shorter idle timeout, selected-rate animations, keyboard and passive-video ceilings, and fixed interaction rate.          |

Thermal and other higher-priority system restrictions can still limit the actual refresh rate.

## Per-app refresh policies

Open **Settings → Display & brightness → Screen refresh rate → Supported apps**.

| Policy                                | Meaning                                                         |
| ------------------------------------- | --------------------------------------------------------------- |
| Fixed 60 / 90 / 120 / 144 / 165 Hz    | Holds the app's selected rate during ordinary use.              |
| Variable 60 / 90 / 120 / 144 / 165 Hz | Sets an interaction ceiling while keeping native adaptive idle. |
| Follow current                        | Follows LTPO-X's selected maximum rate.                         |
| System default                        | Removes the app-specific policy.                                |

Keyboard, video, thermal, and system limits still take precedence.

The three-dot menu can apply a policy to all listed apps, clear LTPO-X overrides, or follow the current maximum rate. Disabling the per-app policy switch retains saved selections without applying them.

## Always-On Display

The AOD section has an independent master switch.

| Feature                     | Behavior                                                                 |
| --------------------------- | ------------------------------------------------------------------------ |
| Full-screen and all-day AOD | Exposes supported panoramic AOD options.                                 |
| Status bar and wallpaper    | Controls panoramic status-bar visibility and the AOD wallpaper backdrop. |
| Tap and lift                | Configures supported tap gestures and lift-to-show behavior.             |
| Brightness                  | Adjusts initial, running, or fixed AOD brightness.                       |
| Battery-only status         | Shows the battery percentage instead of the full AOD status bar.         |

Availability depends on the device's native AOD implementation.

## Installation

1. Remove an older `com.refresh165` installation before installing this package; its settings do not migrate.
2. Install the release APK and enable **LTPO-X (COS)** in a compatible module manager.
3. Enable the recommended scopes: Android/System (`android` or `system`), Settings (`com.android.settings`), SystemUI (`com.android.systemui`), and OPlus AOD (`com.oplus.aod`).
4. Reboot after installing or updating the APK.
5. In system display settings, select **Standard (60 Hz)** if you want LTPO-X to manage the maximum refresh rate. Custom system modes are intentionally preserved.

The in-app **Restart scopes** action requires root and does not reload `system_server`; it does not replace a full reboot after an APK update. Root is also required to change Android's refresh-rate overlay indicator.

## Known limitations

| Area                | Limitation                                                                              |
| ------------------- | --------------------------------------------------------------------------------------- |
| Idle refresh        | 1 Hz is not guaranteed in every scene; 144 Hz to 1 Hz remains experimental.             |
| 165 Hz mode         | Idle floor varies between firmware versions.                                            |
| System restrictions | Thermal policy and unsupported modes may reduce the requested rate.                     |
| Refresh measurement | The app's FPS indicator and rendered frame rate may differ from physical panel refresh. |

## Reporting issues

Include the device model, complete firmware version, module/runtime versions, enabled features, selected system and LTPO-X rates, brightness, overlapping modules, and steps to reproduce. Describe whether video was fullscreen or inline and whether input was active. Do not publish private device dumps, account data, or signing keys.

## Credits and license

- [MaZaGnGy](https://github.com/MaZaGnGy/Screen-Tuner) — previous maintainer; credited for the project's earlier work.
- [Refresh165 / wanwei1028](https://github.com/wanwei1028/Refresh165) — original project foundation. The original MIT notice is retained in [LICENSE](LICENSE).
- [Force165hz / tryptz](https://github.com/tryptz/Force165hz-Oneplus-15-NoRoot) — refresh-rate implementation reference.
- **FreshShift** and **ColorOS AOD** — references for display, Settings, and AOD integration.
- [murongchaopin / murongruyan](https://github.com/murongruyan/murongchaopin) — display-policy research reference.
- [libxposed](https://github.com/libxposed/api) — module API dependency.

**Maintainer:** [xizt159](https://github.com/xizt159)
