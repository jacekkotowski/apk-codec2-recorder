#!/bin/bash
# Builds, from third_party/opus-1.6.1 with the app's flags (opusflags.sh), no autotools:
#   tests/dred/dredtest       coder and player on the command line
#   tests/dred/out/libdred.so the app's JNI library (app/jni/dredjni.c) for this computer, with the Java classes beside it
# and tests/dred/fdvpipe from the modem library.
set -e
cd "$(dirname "$0")/../.."
. ./opusflags.sh
mkdir -p tests/dred/obj
# Each Opus file is compiled once into tests/dred/obj, then linked.
objs=""
for f in $OPUSSRCS; do o=tests/dred/obj/$(echo ${f#$OPUS/} | tr / _).o; objs="$objs $o"; [ "$o" -nt "$f" ] || gcc $OPUSFLAGS -fPIC ${CFLAGS_EXTRA} -c -o "$o" "$f" & done; wait
gcc $OPUSFLAGS ${CFLAGS_EXTRA} -o tests/dred/dredtest tests/dred/dredtest.c $objs -lm
JH=$(dirname $(dirname $(readlink -f $(which javac)))); O=tests/dred/out; mkdir -p $O
gcc $OPUSFLAGS -fPIC -shared ${CFLAGS_EXTRA} -I$JH/include -I$JH/include/linux -Wl,--no-undefined -o $O/libdred.so app/jni/dredjni.c $objs -lm
javac -nowarn -d $O app/src/pl/dyktafon/Dred.java app/src/pl/dyktafon/DredLib.java $( [ -f app/src/pl/dyktafon/Recorder.java ] && echo app/src/pl/dyktafon/{Resampler,VoiceChain,Denoiser,Vad,Limiter,Compressor,Recorder}.java) tests/dred/*.java 2>&1 | grep -v "^Picked up" || true
. ./fdvflags.sh
C12=third_party/codec2-1.2/src; H12=third_party/codec2-1.2/generated
gcc -O2 -DNDEBUG $FDVFLAGS -I$C12 -I$H12 -o tests/dred/fdvpipe tests/dred/fdvpipe.c $C12/*.c $H12/*.c -lm
echo built tests/dred/dredtest, tests/dred/out and tests/dred/fdvpipe
