# Changelog

## v1.0.1 — 2026-11-27

### Fixed

- Restore compatibility with the vendor's mode-0 **Standard (60 Hz)** setting, as used by the beta4 refresh policy. Without this sentinel, maximum-rate overrides and the Battery Saver ceiling could both be bypassed.
- Remove the requirement to select **Standard (60 Hz)**; the selected maximum and Battery Saver cap now apply regardless of the system refresh mode.

### Changed

- Hide dependent display and AOD controls while their master switch is off, move activation guidance into a **Help** alert, place **Show refresh rate** in **Developer options**, and distinguish selection controls from navigation links.

### Release

- Set version to `1.0.1` (`versionCode` `20012`) so this fix installs as an update to v1.0.0.
- The release must be signed with the same key as existing installations.

## v1.0.0 — 2026-11-27

### Added

- Added the optional **Lock screen & camera at 120 Hz** ceiling for a locked keyguard or detected foreground camera app.
- Clarified the 144 Hz idle fallback and renamed the Battery Saver control.

### Changed

- Respect the selected maximum refresh rate when mapping supported device refresh modes, including devices whose highest available mode is 144 Hz.
- Apply the optional 60 Hz Battery Saver ceiling while retaining adaptive LTPO behavior.
- Removed duplicate section headings and clarified when the 144 → 120 Hz idle option is needed for 1 Hz idle.

### Release

- Set version to `1.0.0` (`versionCode` `20011`).
- The release must be signed with the same key as existing installations to install as an update.
- Lock-screen and camera refresh behavior depends on the device's keyguard, camera-app resolution, and OEM display policy; it has not been physically validated on every supported device.

## v1.0.0-beta.5 — Beta 5 — 2026-11-22

### Changed

- Apply LTPO-X refresh-rate overrides only when the system display mode is explicitly **Standard (60 Hz)**; leave Auto and custom modes untouched.
- Simplified the maximum refresh-rate control and kept the optional 60 Hz Battery Saver cap separate.
- Enforce the selected ceiling in both display ranges and refresh-rate votes, including while Battery Saver is active.
- Offer 185 Hz when supported by the device and streamline the interface to English.

### Release

- Increased `versionCode` to `20010`.
- The release must be signed with the same key as existing installations to install as an update.

## v1.0.0-beta.4 — Beta 4 — 2026-11-10

### Changed

- Prepared the release with separate general refresh-rate and Battery Saver refresh-rate caps.
- The general cap applies whenever enabled; the Battery Saver cap applies only while Android Battery Saver is active.
- Enforced refresh-rate caps on resolved display ranges to prevent native 165Hz overrides.
- Made the app interface English-only and removed the language selector.

### Release

- Increased `versionCode` to `20009`.
- Renamed the app and Xposed module label to LTPO-X (COS) and changed its package ID to `com.ltpoxcos`. This package-ID change requires uninstalling the previous package before installing Beta 3.

## v1.0.0-beta.3 — Beta 3 — 2026-11-10

### Changed

- Added Battery Saver refresh-rate caps. The general cap applies whenever enabled; the cap applies only while Android Battery Saver is active.

### Release

- Increased `versionCode` to `20008`.
- The release must be signed with the same key as existing installations to install as an update.

## v1.0.0-beta.2 — Beta 2 — 2026-11-07

### Added

- Added an Activation toggle for Android's Show refresh rate developer option. The toggle requests root access, verifies the global setting, and reports failures.
- Added a tag-triggered GitHub release workflow that signs APKs with the configured release keystore and replaces the APK asset when rerunning the same tag.
- Added manual release workflow dispatch for rebuilding an existing version tag.

### Release

- Increased `versionCode` to `20007`.
- The release must be signed with the same key as existing installations to install as an update.

## v1.0.0-beta.1 — Beta 1 — 2026-11-06

### Added

- Native focus-change event tracking and targeted deoptimization of verified focus callers.
- Configuration-load retry for early-boot provider availability, plus loaded-build/profile diagnostics.
- Native Supported apps selectors for Fixed and Variable 165/144/120/90/60Hz, Follow current and System default.
- The same selectors in the native search results, with correct row recycling.
- Three-dot Select all…, Clear all and Follow current refresh rate actions.
- Native Restore to defaults integration clearing module overrides after confirmation.
- Independent per-app policy master; saving a profile does not enable an inactive legacy app list.
- Validated, persistent, atomic profile writes restricted to the module app and platform-signed Settings.

### Policy behavior

- Focused full-task app selects its rate; switching apps releases the previous profile.
- Fixed profiles request matching physical/render floors during ordinary use and suppress native touch-idle on a private outgoing configuration copy.
- Variable profiles allow native idle and override the global LTPO-disable choice for the selected app.
- Follow current responds to subsequent changes of the global selected rate.
- Per-app user peak votes are changed in a returned copy; native vote storage, thermal limits and fingerprint votes are preserved.
- Keyboard60 after300ms and passive video60/native30 remain higher-priority temporary policies.
- Native idle configuration is refreshed when entering/leaving a fixed hold.

### Validation and unchanged behavior

- 757 host-side assertions passed.
- Reviewed the matching Settings APK and the live99-app list/Restore to defaults menu on CPH2745_17.0.0.102(EX01).
- The development-device user confirmed that per-app policy works across all available fixed/variable choices. Variable120Hz on Gallery was independently captured with a120Hz resolved ceiling/render rate while the global selection was165Hz.
- The earlier exp5 APK was initially tested before system_server loaded its update; an additional focus-policy issue remained after reboot and prompted the Beta 1 fixes above.
- AOD code, recognized idle tables and IME delay retained from exp4.
- 144Hz-to-1Hz remains unresolved.

## v2.0.3-unified-exp4

- Added Block lift-to-show AOD for native tilt/motion reasons, preserving tap gestures.
- Added fixed AOD brightness2–255 with priority over initial/running brightness controls.
- Added AOD battery-percentage-only display.
- Added native refresh-selector up/down indicator.
- Moved Restart scopes beside Settings with a circular-arrows icon.
- Added Arabic/English strings for the new controls.
  -633 host-side assertions passed; new physical-device behavior remained pending confirmation.

## v2.0.2-unified-exp3

- Accepted identical duplicate SurfaceFlinger root records while still rejecting conflicting roots.
- Added opaque native AOD backdrop handling across supported display modes.
- Used the installed LuckyTool native keyguard status-bar condition for panoramic AOD.
- Added two taps to reveal AOD, followed by a separate tap to wake the lock screen.
- Kept native proximity/upside-down checks for wake behavior.
  -513 host-side assertions passed. The user subsequently reported all required behavior working except144Hz-to-1Hz.

## v2.0.1-unified-exp2

- Refined passive inline-video handling for supported ambient UI redraws.
- Integrated the existing ColorOS refresh-rate menu rather than adding a separate row.
- Added LTPO-X (COS) branding, English/Arabic settings and root-assisted scope restart.
- Added an experimental real120Hz idle fallback for144Hz; this was not a confirmed1Hz fix.

## v2.0.0-unified-exp1

- Combined refresh-rate and AOD controls in one APK.
- Added guarded idle-table changes for60/90/120/165Hz while retaining DC eligibility.
- Increased IME show delay to300ms.
- Integrated initial/running AOD brightness and supported panoramic/all-day controls.

## Earlier165Hz baseline

- Developed interaction165Hz with low-brightness idle, IME60 and passive video60/native30.
- The r8 keyboard entrance delay was200ms before the unified branch moved it to300ms.
