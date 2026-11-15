#!/bin/bash
# Computer test of the self-test (app/src/pl/dyktafon/SelfTest.java) against the real native libraries.
# Needs tests/dred/build.sh and tests/radio/build.sh to have run (they build the libraries for this computer).
set -e
cd "$(dirname "$0")/../.."
[ -f tests/dred/out/libvoice.so ] || bash tests/dred/build.sh
[ -f tests/radio/out/libfdv.so ] || bash tests/radio/build.sh
O=tests/selftest/out; rm -rf $O; mkdir -p $O
D=app/src/pl/dyktafon
javac -nowarn -d $O $D/{SelfTest,Dred,DredLib,Rade,Recorder,Resampler,VoiceChain,Denoiser,Vad,Limiter,Compressor,Fountain,Radio,RadioLink}.java tests/selftest/ST.java 2>&1 | grep -v "^Picked up" || true
java -Djava.library.path=tests/dred/out:tests/radio/out -cp $O pl.dyktafon.ST 2>&1 | grep -v "^Picked up"
