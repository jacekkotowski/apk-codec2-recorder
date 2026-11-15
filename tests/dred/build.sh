#!/bin/bash
# Builds, from third_party/opus-1.6.1 with the app's flags (opusflags.sh), no autotools:
#   tests/dred/dredtest       coder and player on the command line
#   tests/dred/out/libvoice.so the app's JNI library (app/jni/dredjni.c and radejni.c: DRED and RADE) for this computer, with the Java classes beside it
# and tests/dred/fdvpipe from the modem library.
set -e
cd "$(dirname "$0")/../.."
. ./opusflags.sh
. ./radeflags.sh
mkdir -p tests/dred/obj
# Each Opus file is compiled once into tests/dred/obj, then linked.
objs=""
for f in $OPUSSRCS; do o=tests/dred/obj/$(echo ${f#$OPUS/} | tr / _).o; objs="$objs $o"; [ "$o" -nt "$f" ] || gcc $OPUSFLAGS -fPIC ${CFLAGS_EXTRA} -c -o "$o" "$f" & done; wait
gcc $OPUSFLAGS ${CFLAGS_EXTRA} -o tests/dred/dredtest tests/dred/dredtest.c $objs -lm
JH=$(dirname $(dirname $(readlink -f $(which javac)))); O=tests/dred/out; mkdir -p $O
# The app's library: DRED and RADE V1 with the patched copy of nnet.c (radeflags.sh), as build.sh makes it.
voice_nnet tests/dred/obj
vobjs=$(for o in $objs; do [ "$(basename $o)" = nnet.c.o ] || [ "$(basename $o)" = dnn_nnet.c.o ] || printf '%s ' $o; done)
gcc $OPUSFLAGS -fPIC ${CFLAGS_EXTRA} -c -o tests/dred/obj/voice_nnet.o tests/dred/obj/nnet.c
for f in $RADESRCS; do o=tests/dred/obj/rade_$(basename $f .c).o; vobjs="$vobjs $o"; [ "$o" -nt "$f" ] || gcc $OPUSFLAGS $RADEFLAGS -fPIC ${CFLAGS_EXTRA} -c -o "$o" "$f" 2>/dev/null & done; wait
gcc $OPUSFLAGS $RADEFLAGS -fPIC -shared ${CFLAGS_EXTRA} -I$JH/include -I$JH/include/linux -Wl,--no-undefined -o $O/libvoice.so app/jni/dredjni.c app/jni/radejni.c $vobjs tests/dred/obj/voice_nnet.o -lm
javac -nowarn -d $O app/src/pl/dyktafon/Dred.java app/src/pl/dyktafon/DredLib.java app/src/pl/dyktafon/Rade.java $( [ -f app/src/pl/dyktafon/Recorder.java ] && echo app/src/pl/dyktafon/{Resampler,VoiceChain,Denoiser,Vad,Limiter,Compressor,Recorder}.java) tests/dred/*.java 2>&1 | grep -v "^Picked up" || true
. ./fdvflags.sh
C12=third_party/codec2-1.2/src; H12=third_party/codec2-1.2/generated
gcc -O2 -DNDEBUG $FDVFLAGS -I$C12 -I$H12 -o tests/dred/fdvpipe tests/dred/fdvpipe.c $C12/*.c $H12/*.c -lm
echo built tests/dred/dredtest, tests/dred/out and tests/dred/fdvpipe
