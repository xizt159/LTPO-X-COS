# LTPO-X (COS)

**Adaptive refresh-rate policies and Always-On Display controls for compatible OnePlus and OPPO devices running OxygenOS or ColorOS.**

LTPO-X (COS) brings display tuning, per-app refresh policies and AOD customization into one APK-based Xposed module. It aims to keep interaction smooth at your selected rate, allow low-refresh idle where supported, and apply temporary limits during typing and passive video playback.

**Current release:** `1.0.0-beta.3` (Beta 3) · **versionCode:** `20008` · **package:** `com.ltpoxcos`

[Download APK](https://github.com/xizt159/LTPO-X-COS/releases/latest) · [Releases](https://github.com/xizt159/LTPO-X-COS/releases) · [Changelog](CHANGELOG.md) · [Arabic Beta 1 guide](README-BETA1-AR.md)

## Compatibility and requirements

- Target devices: **OnePlus and OPPO** models running compatible OxygenOS or ColorOS firmware; support can vary with vendor APIs and release.
- Reviewed reference firmware: **OnePlus 15 / CPH2745**, running **CPH2745_17.0.0.102(EX01), ColorOS 17**. **OxygenOS 16 is also reported working.**
- A rooted device with a working hooking framework supporting **modern libxposed API 102**. Legacy-only Xposed support is insufficient.
- This remains an experimental module for compatible OnePlus and OPPO firmware; behavior depends on the system's vendor APIs and can vary by model and release.

The APK does not flash DTBO, replace DRM kernel modules or create new physical
panel timings. It is an Xposed/libxposed module and still requires a hook
runtime that implements libxposed API 102 to hook Android and OEM Java
processes. Magisk or KernelSU alone cannot replace that runtime. A Magisk/KernelSU
package could distribute or install the APK, but would not remove the Xposed
runtime requirement or turn these Java hooks into kernel-level hooks.

## Display and brightness

| Feature | Behavior |
| --- | --- |
| Display master switch | Enables display modifications independently of AOD. |
| Interaction refresh rate | Select **60 / 90 / 120 / 144 / 165Hz** as the ordinary interaction ceiling. |
| General refresh-rate cap | Optional and off by default. When enabled, caps the global and per-app refresh ceiling (default **120Hz**) at all times, regardless of Battery Saver. LTPO's native adaptive behavior remains enabled. |
| Battery Saver refresh-rate cap | A separate optional cap (default **120Hz**) that applies only while Android Battery Saver is enabled. Both caps can be enabled together; the lower ceiling takes effect. |
| Native Settings selector | Expands the existing OPlus refresh-rate selector, retaining the native popup and adding an up/down indicator. |
| Low-brightness idle | Adjusts recognized eligibility conditions to permit deeper idle where supported, while retaining DC eligibility. |
| 1Hz idle tables | Changes recognized 60/90/120/165Hz idle tables; unknown schemas are preserved. A table change is not a guarantee of 1Hz in every scene. |
| Faster adaptive idle | Shortens recognized default delays to 600ms/200ms; application-specific timings are preserved. No fixed sequence of refresh rates is forced. |
| Selected-rate animations | Adjusts supported launcher and transition requests to the selected rate, retaining thermal and relevant fingerprint restrictions. |
| Keyboard cap | Applies a temporary **60Hz ceiling after 300ms** of keyboard visibility, allowing the entrance animation to finish first. The previous policy resumes after dismissal. |
| Passive video policy | Uses a **60Hz ceiling with native 30Hz behavior where applicable**, only after supported playback is detected and input/content settle. Google Photos can be detected from advancing video-surface evidence even without a media session. Interaction restores the selected ceiling. Supported fullscreen and inline playback are handled without permanently capping the whole app. **120Hz is not selected for video.** |
| Disable LTPO | Requests the selected rate during ordinary use. Temporary keyboard/video limits and thermal constraints still apply. |
| Experimental 144Hz fallback | Optionally requests the real 120Hz timing during verified idle, then returns to 144Hz on interaction. **144Hz-to-1Hz remains unresolved.** |
| Show refresh rate | Toggles Android's green refresh-rate indicator in the top-left corner. Changing the developer option requires granting root access when prompted. |

Thermal protection remains effective. A requested fixed rate may yield to higher-priority system restrictions or unsupported mode combinations.

## Per-app refresh policies

Open **Settings → Display & brightness → Screen refresh rate → Supported apps**.

The native on/off switch is replaced by a policy selector. The same controls work in the normal list and search results.

| Policy | Meaning |
| --- | --- |
| Fixed 165 / 144 / 120 / 90 / 60Hz | Requests the chosen rate during ordinary app use and disables touch-idle through a private outgoing configuration copy while the fixed policy is held. |
| Variable 165 / 144 / 120 / 90 / 60Hz | Selects an interaction ceiling while allowing native adaptive idle. It takes precedence over the global LTPO-disable switch for that app. |
| Follow current | Dynamically follows the global selected refresh rate, with adaptive idle. Later changes to the global rate are followed automatically. |
| System default | Removes the app-specific enforcement. General module settings still apply. |

**The three-dot menu adds:**

- **Select all…** — choose one policy for the full supported-app list, including rows currently offscreen.
- **Clear all** — return the list to System default.
- **Follow current refresh rate** — apply dynamic following to the full list.
- **Restore to defaults** — retains the native confirmation/reset and also clears LTPO-X (COS) app overrides after confirmation.

Choosing a policy enables the per-app policy master switch. Disabling that master retains saved choices but stops applying them. The focused full-task app owns the per-app policy; multiwindow does not impose one app's profile on the other windows. Keyboard, passive-video, thermal and relevant system limits still take precedence over fixed profiles.

## Always-On Display

AOD has a separate master switch and individual feature switches.

| Feature | Behavior |
| --- | --- |
| Full-screen/Panoramic AOD | Exposes support through compatible native paths. |
| All-day option | Exposes supported panoramic all-day settings. |
| Panoramic status bar | Hides the native panoramic keyguard status bar. |
| Hide wallpaper | Keeps the Full-screen design with a black backdrop similar to Partial AOD, across the available display modes. The regular lock-screen wallpaper returns on wake. |
| Two-stage tap gesture | **Two taps reveal AOD; one separate subsequent tap wakes the lock screen.** It does not dismiss authentication. |
| Dark-environment visibility | Prevents the supported native dark-light rule from hiding AOD. |
| Initial brightness in darkness | Separate switch and value for the initial dark-environment brightness. |
| Initial brightness in light | Separate switch and value for initial brightness in brighter environments. |
| Running brightness | Applies a configurable **25–300% multiplier** to supported normalized AOD brightness requests. |
| Block lift-to-show | Prevents tilt/motion from revealing AOD while retaining tap gestures. Turning it off restores native behavior; it does not force a system-disabled lift feature on. |
| Fixed AOD brightness | Choose a relative level from **2–255**. Takes precedence over initial brightness and the multiplier. Turning it off restores previous settings. Values are not nits; screen-off/invalid requests retain native meaning. |
| Battery percentage only | Displays the percentage instead of the full status bar during AOD. The normal lock-screen/status-bar presentation returns on wake. |

Block lift-to-show, fixed AOD brightness and battery-only AOD are off by default. Availability depends on the native AOD implementation and selected mode.

## App interface

- Separate **Display & Brightness** and **AOD** sections.
- English and Arabic, with a language selector and module version; the header reports the detected OxygenOS/ColorOS version when available.
- A circular-arrows **Restart scopes** button beside Settings, restarting **SystemUI and OPlus AOD**.
- Root warning before scope restart; no root prompt simply from opening the app.
- No Internet permission requested by the APK.

## Installation and updates

1. Uninstall the previous LTPO-X (COS) package (`com.refresh165`) and disable other overlapping refresh-rate/AOD modules or corresponding features, including FreshShift, ColorOS AOD and LuckyTool display modifications.
2. Install the release APK. Because its package ID changed, it cannot update the previous package in place. Settings from the previous installation do not carry over and must be configured again.
3. Enable LTPO-X (COS) (`com.ltpoxcos`) in your module manager and accept the recommended scopes:
   - Android System / System Framework (`android` / `system`, depending on manager naming)
   - Settings (`com.android.settings`)
   - SystemUI (`com.android.systemui`)
   - OPlus AOD (`com.oplus.aod`)
4. **Reboot the phone after installing or updating the APK.**
5. Configure the desired features. Per-app policies are edited from the native Supported apps page.

The Restart scopes button does **not** reload `system_server`; it does not replace the full reboot after an APK update. Root access is used by the Restart scopes button and the Show refresh rate toggle. If denied, module settings still work; restart the scopes manually or reboot instead.

## Latest changes: Beta 3

- Separate general and Battery Saver refresh-rate caps.
- Renamed the app and module label to LTPO-X (COS) and migrated the package ID to `com.ltpoxcos`.
- Beta 3 requires uninstalling the previous `com.refresh165` package; its saved settings must be configured again.

See [CHANGELOG.md](CHANGELOG.md) for earlier builds.

## Validation and known limitations

| Area | Status |
| --- | --- |
| 165 / 120 / 90Hz low-brightness idle | 1Hz reported working on the development device in the established unified builds. |
| 144Hz → 1Hz | **Unresolved.** Experimental 120Hz idle fallback is not a confirmed fix. |
| 60Hz profile | Selectable; low-brightness 1Hz at this profile has not been confirmed. |
| Keyboard and established video/AOD features | Earlier unified behavior was user-tested; newer additions have separate validation limits. |
| exp4 additions | Lift toggle, fixed AOD brightness and battery-only display were host-tested; physical-device confirmation remains pending in the recorded results. |
| Beta 1 per-app policies | **757 host-side assertions passed.** The current native Settings APK and its 99-app page/overflow menu were reviewed on the development device. The development-device user confirmed the per-app policy works across all available fixed/variable choices. System telemetry independently recorded Variable120Hz on the Gallery app while the global rate was165Hz. |

1Hz requires suitable static content and native operating conditions; it is not guaranteed during scrolling, animations, playback or every application. Video detection requires supported playback/content evidence; Google Photos without a media session additionally requires a detectable advancing video surface. External PiP and multiwindow are excluded from the passive-video override. A rendering FPS overlay may differ from the physical panel's refresh behavior.

## Building and tests

Dependencies: JDK17+, Python3, Android SDK platform34 and build-tools34+. The repository includes the libxposed API102 compile-time JAR.

**Windows:**

```powershell
python build_windows.py --android-jar C:/Android/Sdk/platforms/android-34/android.jar --build-tools C:/Android/Sdk/build-tools/34.0.0 --out out-local
```

Use a fresh output directory. The local builder creates a local test signing key when absent. It is not included in this repository and will not match the published release certificate.

**Linux/macOS:**

```bash
ANDROID_HOME=/path/to/Android/Sdk ./build.sh
```

The shell builder creates a separate local key if none is provided. Self-built APKs do not automatically share the release signature. The verification workflow uses an ephemeral test key; release APKs are built by the tag-triggered workflow below.

### GitHub release builds

Pushing a tag matching the `versionName` in `version.properties` (for example, `v1.0.0-beta.3`) runs the signed release workflow. A new version tag creates a new GitHub Release and retains older releases. To rebuild an existing version, run **Build and publish release APK** from the Actions page and enter its existing tag; that replaces the release's APK asset.

Configure these repository Actions secrets before pushing a release tag:

- `RELEASE_KEYSTORE_BASE64` — Base64-encoded release keystore.
- `RELEASE_KEYSTORE_PASSWORD` — keystore password.
- `RELEASE_KEY_ALIAS` — signing key alias.
- `RELEASE_KEY_PASSWORD` — signing key password.

The keystore must be the same one used for the APKs being updated. For a new release, increment both `versionName` and `versionCode` in `version.properties`, commit the change, then create and push the matching tag:

```bash
git tag v1.0.0-beta.3
git push origin v1.0.0-beta.3
```

Use the actual `versionName` from `version.properties`. Do not move a published tag. A newly generated key cannot update installs signed by a different key.

**Host regression suites:**

```bash
python run_tests.py /tmp/ltpo-x-cos-pure
python tests/run_lifecycle.py /tmp/ltpo-x-cos-lifecycle
python tests/provider_check.py /tmp/ltpo-x-cos-provider
python tests/settings_check.py /tmp/ltpo-x-cos-settings
ANDROID_JAR=/path/to/android.jar python tests/widgets_check.py /tmp/ltpo-x-cos-widgets
ANDROID_JAR=/path/to/android.jar python tests/supported_apps_check.py /tmp/ltpo-x-cos-supported
```

These suites use Android/OEM stand-ins and recorded fixtures. They validate adapter behavior and policy isolation; they cannot establish physical panel performance or visual correctness on a phone.

## Reporting an issue

Include the device model, complete firmware version, module/framework versions, enabled features, selected global/per-app rate, brightness, overlapping modules and clear reproduction steps. For video/keyboard issues, describe whether playback was fullscreen/inline and whether input was active. Specify the counter or measurement source used for refresh-rate results.

Do not publish private device dumps, account data or signing keys in issues.

## Credits and license

- [Refresh165 / wanwei1028](https://github.com/wanwei1028/Refresh165) — original project foundation. Original MIT notice is retained in [LICENSE](LICENSE).
- **FreshShift** and **ColorOS AOD** — reference modules for Settings and AOD integration.
- **LuckyTool** — reference for the installed panoramic status-bar handling.
- [murongchaopin / murongruyan](https://github.com/murongruyan/murongchaopin) — display-policy research reference.
- [libxposed](https://github.com/libxposed/api) — module API dependency.

Maintained and published by [MaZaGnGy](https://github.com/MaZaGnGy).
