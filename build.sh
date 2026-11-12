#!/bin/bash
# Builds Codec2Recorder.apk. Needs ANDROID_SDK_ROOT and signing.env (see README.md).
set -e
cd "$(dirname "$0")"
S=${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK}
[ -f signing.env ] || { echo "signing.env is missing, see README.md"; exit 1; }
. ./signing.env
BT=$S/build-tools/36.0.0; J=$S/platforms/android-36/android.jar; NDK=${ANDROID_NDK_ROOT:-$S/android-ndk-r28c}; O=out; rm -rf $O; mkdir -p $O/classes $O/res
TC=$(echo $NDK/toolchains/llvm/prebuilt/*/bin)
C=third_party/codec2/src; H=third_party/codec2/generated
SRCS="$C/*.c $H/*.c"
C12=third_party/codec2-1.2/src; H12=third_party/codec2-1.2/generated
. ./fdvflags.sh
for pair in "arm64-v8a aarch64-linux-android29" "armeabi-v7a armv7a-linux-androideabi29" "x86_64 x86_64-linux-android29"; do set -- $pair; mkdir -p $O/lib/$1
 $TC/clang --target=$2 -O2 -fPIC -shared -DNDEBUG -I$C -I$H -Wl,-z,max-page-size=16384 -Wl,--no-undefined -o $O/lib/$1/libc2.so app/jni/c2jni.c $SRCS -lm -llog 2>&1 | grep -E "error|undefined" | head -8 || true
 $TC/llvm-strip --strip-unneeded $O/lib/$1/libc2.so
 # Modem library: Codec 2 1.2 with only the modes the app uses. Hidden symbols keep it apart from libc2.so.
 $TC/clang --target=$2 -O2 -fPIC -shared -DNDEBUG $FDVFLAGS -fvisibility=hidden -I$C12 -I$H12 -Wl,-z,max-page-size=16384 -Wl,--no-undefined -o $O/lib/$1/libfdv.so app/jni/fdvjni.c $C12/*.c $H12/*.c -lm 2>&1 | grep -E "error|undefined" | head -8 || true
 $TC/llvm-strip --strip-unneeded $O/lib/$1/libfdv.so
done
$BT/aapt2 compile --dir app/res -o $O/res/res.zip
$BT/aapt2 link -o $O/base.apk -I $J --manifest app/AndroidManifest.xml --min-sdk-version 29 --target-sdk-version 36 -A app/assets $O/res/res.zip
javac -nowarn --release 11 -classpath $J -d $O/classes app/src/pl/dyktafon/*.java 2>&1 | grep -v "^Picked up\|^Note" || true
$BT/d8 --release --lib $J --min-api 29 --output $O $(find $O/classes -name '*.class') 2>&1 | grep -v "^Picked up" || true
cp $O/base.apk $O/u.apk; (cd $O && zip -q u.apk classes.dex && zip -q -r u.apk lib)
$BT/zipalign -f -p 4 $O/u.apk $O/a.apk
$BT/apksigner sign --ks "$KS_FILE" --ks-pass "pass:$KS_PASS" --out Codec2Recorder.apk $O/a.apk 2>&1 | grep -v "^Picked up" || true
$BT/apksigner verify Codec2Recorder.apk 2>&1 | grep -v "^Picked up\|WARNING" || true
ls -la Codec2Recorder.apk; unzip -l Codec2Recorder.apk | grep "lib/"
