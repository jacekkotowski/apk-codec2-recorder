#!/bin/bash
# Builds Codec2Recorder.apk. Needs ANDROID_SDK_ROOT and signing.env (see README.md).
#   bash build.sh        the real app, package pl.dyktafon
#   bash build.sh test   a test build, package pl.dyktafon.test, named "Recorder test", which installs beside the real app
set -e
KIND=$1
cd "$(dirname "$0")"
S=${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK}
[ -f signing.env ] || { echo "signing.env is missing, see README.md"; exit 1; }
. ./signing.env
BT=$S/build-tools/36.0.0; J=$S/platforms/android-36/android.jar; NDK=${ANDROID_NDK_ROOT:-$S/android-ndk-r28c}; O=out; rm -rf $O; mkdir -p $O/classes $O/res
TC=$(echo $NDK/toolchains/llvm/prebuilt/*/bin)
. ./opusflags.sh
C12=third_party/codec2-1.2/src; H12=third_party/codec2-1.2/generated
. ./fdvflags.sh
# Each processor type builds in parallel; the logs are printed after.
for pair in "arm64-v8a aarch64-linux-android29" "armeabi-v7a armv7a-linux-androideabi29" "x86_64 x86_64-linux-android29"; do set -- $pair; mkdir -p $O/lib/$1
 (
 # DRED coder and player: Opus 1.6.1 with DRED, flags in opusflags.sh.
 $TC/clang --target=$2 $OPUSFLAGS -DNDEBUG -fPIC -shared -fvisibility=hidden -Wl,-z,max-page-size=16384 -Wl,--no-undefined -o $O/lib/$1/libdred.so app/jni/dredjni.c $OPUSSRCS -lm -llog 2>&1 | grep -E "error|undefined" | head -8
 $TC/llvm-strip --strip-unneeded $O/lib/$1/libdred.so
 # Modem library: Codec 2 1.2 with only the modes the app uses.
 $TC/clang --target=$2 -O2 -fPIC -shared -DNDEBUG $FDVFLAGS -fvisibility=hidden -I$C12 -I$H12 -Wl,-z,max-page-size=16384 -Wl,--no-undefined -o $O/lib/$1/libfdv.so app/jni/fdvjni.c $C12/*.c $H12/*.c -lm 2>&1 | grep -E "error|undefined" | head -8
 $TC/llvm-strip --strip-unneeded $O/lib/$1/libfdv.so
 ) > $O/lib-$1.log 2>&1 &
done
wait; cat $O/lib-*.log
for a in arm64-v8a armeabi-v7a x86_64; do for l in dred fdv; do [ -f $O/lib/$a/lib$l.so ] || { echo "lib$l.so for $a did not build"; exit 1; }; done; done
M=app/AndroidManifest.xml; APK=Codec2Recorder.apk; LINK=""
if [ "$KIND" = test ]; then
 # Another package name, provider authority and label, so it installs beside the real app and keeps its own recordings.
 sed -e 's/android:authorities="pl.dyktafon.files"/android:authorities="pl.dyktafon.test.files"/' -e 's/android:label="Codec2 Recorder"/android:label="Recorder test"/' $M > $O/AndroidManifest.xml
 M=$O/AndroidManifest.xml; APK=Codec2Recorder-test.apk; LINK="--rename-manifest-package pl.dyktafon.test --version-name 2.7-test1 --replace-version"
fi
$BT/aapt2 compile --dir app/res -o $O/res/res.zip
$BT/aapt2 link -o $O/base.apk -I $J --manifest $M $LINK --min-sdk-version 29 --target-sdk-version 36 -A app/assets $O/res/res.zip
javac -nowarn --release 11 -classpath $J -d $O/classes app/src/pl/dyktafon/*.java 2>&1 | grep -v "^Picked up\|^Note" || true
$BT/d8 --release --lib $J --min-api 29 --output $O $(find $O/classes -name '*.class') 2>&1 | grep -v "^Picked up" || true
cp $O/base.apk $O/u.apk; (cd $O && zip -q u.apk classes.dex && zip -q -r u.apk lib)
$BT/zipalign -f -p 4 $O/u.apk $O/a.apk
$BT/apksigner sign --ks "$KS_FILE" --ks-pass "pass:$KS_PASS" --out $APK $O/a.apk 2>&1 | grep -v "^Picked up" || true
$BT/apksigner verify $APK 2>&1 | grep -v "^Picked up\|WARNING" || true
ls -la $APK; unzip -l $APK | grep "lib/"
