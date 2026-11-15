# RADE V1 (third_party/rade_c, the FreeDV project's C port, BSD 2-clause) built into the same library as DRED: libvoice.so.
# Source after opusflags.sh. Gives RADESRCS, RADEFLAGS, VOICESRCS (Opus without dnn/nnet.c), and voice_nnet DIR, which writes
# DIR/nnet.c: Opus's dnn/nnet.c with its convolution buffer raised from 1024 to 2048 values, because RADE's encoder layers are
# wider than Opus's own (rade_c patches the same line in its own build, src/opus-nnet.c.diff). The upstream file stays unchanged.
RADE=${RADE:-third_party/rade_c/src}
RADESRCS="$(for f in rade_api rade_enc rade_dec rade_enc_data rade_dec_data rade_dsp rade_ofdm rade_acq rade_tx rade_rx rade_bpf kiss_fft kiss_fftr; do printf '%s ' $RADE/$f.c; done)app/jni/rade_v2_stub.c"
RADEFLAGS="-I$RADE"
VOICESRCS=$(for f in $OPUSSRCS; do [ "$f" = "$OPUS/dnn/nnet.c" ] || printf '%s ' $f; done)
voice_nnet() {
  sed 's/#define MAX_CONV_INPUTS_ALL IMAX(DRED_MAX_CONV_INPUTS, 1024)/#define MAX_CONV_INPUTS_ALL IMAX(DRED_MAX_CONV_INPUTS, 2048) \/* raised for RADE, radeflags.sh *\//' "$OPUS/dnn/nnet.c" > "$1/nnet.c"
  grep -q "raised for RADE" "$1/nnet.c" || { echo "radeflags.sh: the nnet.c patch did not apply; check MAX_CONV_INPUTS_ALL in $OPUS/dnn/nnet.c"; return 1; }
}
