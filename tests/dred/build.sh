#!/bin/bash
# Builds tests/dred/dredtest against an Opus source tree built with DRED on.
# Opus 1.6.1 was used: opus_1.6.1.orig.tar.xz from the Ubuntu source pool,
# sha256 001ac615b9f8f8994b229ad2b1a5969ed12ab48b0d154d0af1016a5899cd6ba1.
#   mkdir build && cd build && ../opus-1.6.1/configure --enable-dred --disable-shared --disable-doc --disable-extra-programs && make
# Then: OPUS_SRC=/path/opus-1.6.1 OPUS_BUILD=/path/build bash tests/dred/build.sh
set -e
cd "$(dirname "$0")"
S=${OPUS_SRC:?Set OPUS_SRC to the Opus source folder}; B=${OPUS_BUILD:?Set OPUS_BUILD to the folder where Opus was built}
gcc -O2 -DHAVE_CONFIG_H -I"$B" -I"$S/include" -I"$S/celt" -I"$S/silk" -I"$S/dnn" -I"$S/src" -o dredtest dredtest.c "$B/.libs/libopus.a" -lm
C=../../third_party/codec2/src; H=../../third_party/codec2/generated
gcc -O2 -DNDEBUG -I$C -I$H -o c2rt c2rt.c $C/*.c $H/*.c -lm
echo built tests/dred/dredtest and tests/dred/c2rt
