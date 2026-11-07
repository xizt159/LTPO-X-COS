#!/usr/bin/env bash
# Build an installable APK from source.
# Requirements: JDK 17+ and Android SDK (build-tools 34+, platform android-34+).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
MOD="$ROOT/module"
VERSION_FILE="$ROOT/version.properties"

VER_NAME="$(grep '^versionName=' "$VERSION_FILE" | cut -d= -f2)"
VER_CODE="$(grep '^versionCode=' "$VERSION_FILE" | cut -d= -f2)"
echo "Refresh165 v$VER_NAME ($VER_CODE)"

SDK="${ANDROID_SDK:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
BT="${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)}"
PLATFORM="${ANDROID_JAR:-$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -1)/android.jar}"
LIBXPOSED="$ROOT/libs/libxposed-api-102.jar"

JAVAC="${JAVAC:-javac}"
D8="$BT/d8"
AAPT2="$BT/aapt2"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"

KEYSTORE="${KEYSTORE:-$ROOT/keystore.jks}"
KS_PASS="${KS_PASS:-modpass}"
KS_ALIAS="${KS_ALIAS:-modkey}"
OUT="${OUT:-$ROOT/out}"
APK_NAME="${APK_NAME:-Refresh165-v$VER_NAME.apk}"

for f in "$D8" "$AAPT2" "$ZIPALIGN" "$APKSIGNER" "$PLATFORM" "$LIBXPOSED"; do
  [ -e "$f" ] || { echo "Missing dependency: $f" >&2; exit 1; }
done

echo "[1/6] Cleaning output"
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/apk"

echo "[2/6] Compiling Java"
find "$MOD/src" -name '*.java' > "$OUT/sources.txt"
"$JAVAC" --release 11 -encoding UTF-8 \
  -cp "$PLATFORM:$LIBXPOSED" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "[3/6] Generating DEX with d8"
find "$OUT/classes/com" -name '*.class' > "$OUT/classes.txt"
"$D8" --release --min-api 29 --lib "$PLATFORM" --output "$OUT/dex" @"$OUT/classes.txt"

echo "[4/6] Compiling and linking resources"
"$AAPT2" compile --dir "$MOD/res" -o "$OUT/res.zip"
sed -E "s/android:versionCode=\"[0-9]+\"/android:versionCode=\"$VER_CODE\"/; s/android:versionName=\"[^\"]*\"/android:versionName=\"$VER_NAME\"/" \
  "$MOD/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$AAPT2" link -o "$OUT/apk/base.apk" -I "$PLATFORM" \
  --manifest "$OUT/AndroidManifest.xml" "$OUT/res.zip"

echo "[5/6] Packaging (including assets and root META-INF/xposed)"
python3 - "$MOD" "$OUT" <<'PY'
from pathlib import Path
import sys, zipfile
mod, out = map(Path, sys.argv[1:])
with zipfile.ZipFile(out/"apk"/"base.apk") as src, \
     zipfile.ZipFile(out/"apk"/"unsigned.apk", "w", zipfile.ZIP_STORED) as dst:
    for name in src.namelist():
        dst.writestr(name, src.read(name))
    dst.write(out/"dex"/"classes.dex", "classes.dex")
    assets = mod/"assets"
    for f in assets.rglob("*"):
        if not f.is_file():
            continue
        rel = str(f.relative_to(assets))
        dst.write(f, "assets/" + rel)
        # Modern Xposed API entry points, scopes, and module.prop must be at the APK root under META-INF/xposed/.
        if rel.startswith("META-INF/"):
            dst.write(f, rel)
PY

echo "[6/6] Aligning and signing"
"$ZIPALIGN" -f -p 4 "$OUT/apk/unsigned.apk" "$OUT/apk/aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
  echo "Keystore not found; generating a self-signed debug key: $KEYSTORE"
  keytool -genkeypair -keystore "$KEYSTORE" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Refresh165, OU=Dev, O=Refresh165, L=NA, S=NA, C=CN"
fi
"$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out "$OUT/$APK_NAME" "$OUT/apk/aligned.apk"
"$APKSIGNER" verify --verbose "$OUT/$APK_NAME" | head -5

echo "Build complete: $OUT/$APK_NAME"
shasum -a 256 "$OUT/$APK_NAME"
