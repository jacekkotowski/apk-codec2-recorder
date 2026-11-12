#!/bin/bash
# Builds the modem library for this computer and the Java classes that use it, into tests/radio/out.
set -e
cd "$(dirname "$0")/../.."
O=tests/radio/out; mkdir -p $O
C=third_party/codec2-1.2/src; G=third_party/codec2-1.2/generated
. ./fdvflags.sh
JH=$(dirname $(dirname $(readlink -f $(which javac))))
gcc -O2 -fPIC -shared -DNDEBUG $FDVFLAGS -I$C -I$G -I$JH/include -I$JH/include/linux -o $O/libfdv.so app/jni/fdvjni.c $C/*.c $G/*.c -lm
javac -nowarn -d $O app/src/pl/dyktafon/Fountain.java app/src/pl/dyktafon/Radio.java app/src/pl/dyktafon/RadioLink.java tests/radio/*.java 2>&1 | grep -v "^Picked up" || true
