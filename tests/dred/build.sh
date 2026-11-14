#!/bin/bash
# Builds tests/dred/dredtest from third_party/opus-1.6.1 with the app's flags (opusflags.sh), no autotools,
# and tests/dred/fdvpipe from the modem library.
set -e
cd "$(dirname "$0")/../.."
. ./opusflags.sh
mkdir -p tests/dred/obj
# Each Opus file is compiled once into tests/dred/obj, then linked.
objs=""
for f in $OPUSSRCS; do o=tests/dred/obj/$(echo ${f#$OPUS/} | tr / _).o; objs="$objs $o"; [ "$o" -nt "$f" ] || gcc $OPUSFLAGS ${CFLAGS_EXTRA} -c -o "$o" "$f" & done; wait
gcc $OPUSFLAGS ${CFLAGS_EXTRA} -o tests/dred/dredtest tests/dred/dredtest.c $objs -lm
. ./fdvflags.sh
C12=third_party/codec2-1.2/src; H12=third_party/codec2-1.2/generated
gcc -O2 -DNDEBUG $FDVFLAGS -I$C12 -I$H12 -o tests/dred/fdvpipe tests/dred/fdvpipe.c $C12/*.c $H12/*.c -lm
echo built tests/dred/dredtest and tests/dred/fdvpipe
