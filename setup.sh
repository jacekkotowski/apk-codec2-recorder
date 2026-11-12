#!/bin/bash
# One-time setup on a fresh Linux machine or cloud session: installs the Android SDK parts build.sh needs (about 3 GB).
# Needs a JDK (17 or newer), curl and unzip. Afterwards:  source ./env.sh && bash build.sh
set -e
cd "$(dirname "$0")"
S=${ANDROID_SDK_ROOT:-$HOME/android-sdk}
if [ ! -x "$S/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$S/cmdline-tools"
  curl -sS -o "$S/clt.zip" https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q "$S/clt.zip" -d "$S/cmdline-tools" && mv "$S/cmdline-tools/cmdline-tools" "$S/cmdline-tools/latest" && rm "$S/clt.zip"
fi
yes | "$S/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$S" "build-tools;36.0.0" "platforms;android-36" "ndk;28.2.13676358" > /dev/null
printf 'export ANDROID_SDK_ROOT=%s\nexport ANDROID_NDK_ROOT=%s/ndk/28.2.13676358\n' "$S" "$S" > env.sh
echo "SDK ready in $S. Next: copy c2.keystore and signing.env into this folder, then:  source ./env.sh && bash build.sh"
