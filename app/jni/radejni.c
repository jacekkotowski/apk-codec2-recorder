/* RADE V1 for the app: speech to modem sound and back. The same steps as rade_tx_wav.c and rade_rx_wav.c in third_party/rade_c,
   on sound in memory instead of files. Speech is 16 kHz, modem sound 8 kHz, both 16 bit mono.
   Sending codes a whole recorded message at once. Receiving takes the radio's sound as it comes and gives speech back. */
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "rade_api.h"
#include "rade_dsp.h"
#include "lpcnet.h"
#include "fargan.h"
#include "cpu_support.h"

#define F(n) Java_pl_dyktafon_Rade_##n
#define NF RADE_NB_TOTAL_FEATURES

typedef struct {
  struct rade *r; int arch;
  /* receive */
  RADE_COMP *in; int have, nin_max; float *feat, *eoo; int n_feat_max;
  FARGANState fargan; int fargan_ready, cont_frames; float cont[5 * NF];
  int eoo_seen;
} Rade;

static int initialized;

/* Rounded as rade_tx_wav.c does, so the app's modem sound matches the reference tool. */
static short modem16(float re) { float v = re * RADE_INT16_SCALE; if (v > 32767.0f) v = 32767.0f; if (v < -32767.0f) v = -32767.0f; return (short)floor(0.5 + (double)v); }
static short to16(float v) { v *= 32768.0f; if (v > 32767.0f) v = 32767.0f; if (v < -32767.0f) v = -32767.0f; return (short)floor(0.5 + (double)v); }

JNIEXPORT jlong JNICALL F(open)(JNIEnv *e, jclass c) {
  if (!initialized) { rade_initialize(); initialized = 1; }
  Rade *h = calloc(1, sizeof(Rade));
  if (!h) return 0;
  h->r = rade_open("", RADE_VERBOSE_0); /* V1, built-in weights */
  if (!h->r) { free(h); return 0; }
  h->arch = opus_select_arch();
  h->nin_max = rade_nin_max(h->r);
  h->n_feat_max = rade_n_features_in_out(h->r);
  h->in = calloc(h->nin_max, sizeof(RADE_COMP));
  h->feat = calloc(h->n_feat_max, sizeof(float));
  h->eoo = calloc(rade_n_eoo_bits(h->r) + 1, sizeof(float));
  if (!h->in || !h->feat || !h->eoo) { rade_close(h->r); free(h->in); free(h->feat); free(h->eoo); free(h); return 0; }
  fargan_init(&h->fargan);
  return (jlong)(intptr_t)h;
}

JNIEXPORT void JNICALL F(close)(JNIEnv *e, jclass c, jlong hh) {
  Rade *h = (Rade *)(intptr_t)hh; if (!h) return;
  rade_close(h->r); free(h->in); free(h->feat); free(h->eoo); free(h);
}

/* Modem sound for a whole message: leadMs of coded silence first (opens a transmitter's VOX without breaking the signal),
   then the speech, then RADE's end-of-over frame. Returns the modem samples, or null. */
JNIEXPORT jshortArray JNICALL F(send)(JNIEnv *e, jclass c, jlong hh, jshortArray speech, jint n, jint leadMs) {
  Rade *h = (Rade *)(intptr_t)hh;
  int nfi = rade_n_features_in_out(h->r), ntx = rade_n_tx_out(h->r), neoo = rade_n_tx_eoo_out(h->r), per = nfi / NF;
  long lead = (long)leadMs * 16, total = lead + n;
  long frames = (total / LPCNET_FRAME_SIZE + per - 1) / per; /* modem frames, the last one padded */
  long outN = frames * ntx + neoo;
  short *out = malloc(outN * sizeof(short)); RADE_COMP *tx = malloc((ntx > neoo ? ntx : neoo) * sizeof(RADE_COMP));
  float *fin = calloc(nfi, sizeof(float)); jshort *s = (*e)->GetShortArrayElements(e, speech, 0);
  LPCNetEncState *enc = lpcnet_encoder_create();
  jshortArray res = 0;
  if (!out || !tx || !fin || !s || !enc) goto done;
  long pos = 0, o = 0; int fi = 0; opus_int16 pcm[LPCNET_FRAME_SIZE];
  while (pos + LPCNET_FRAME_SIZE <= total) {
    for (int i = 0; i < LPCNET_FRAME_SIZE; i++) { long k = pos + i - lead; pcm[i] = k >= 0 ? (s[k] < -32767 ? -32767 : s[k]) : 0; } /* clamped as rade_tx_wav.c does */
    pos += LPCNET_FRAME_SIZE;
    lpcnet_compute_single_frame_features(enc, pcm, &fin[fi * NF], h->arch);
    if (++fi == per) { int m = rade_tx(h->r, tx, fin); for (int i = 0; i < m && o < outN; i++) out[o++] = modem16(tx[i].real); fi = 0; }
  }
  if (fi > 0) { /* the last part of the speech, padded with zero features as rade_tx_wav does */
    memset(&fin[fi * NF], 0, (size_t)(per - fi) * NF * sizeof(float));
    int m = rade_tx(h->r, tx, fin); for (int i = 0; i < m && o < outN; i++) out[o++] = modem16(tx[i].real);
  }
  int m = rade_tx_eoo(h->r, tx); for (int i = 0; i < m && o < outN; i++) out[o++] = modem16(tx[i].real);
  res = (*e)->NewShortArray(e, (jsize)o);
  if (res) (*e)->SetShortArrayRegion(e, res, 0, (jsize)o, out);
done:
  if (enc) lpcnet_encoder_destroy(enc);
  if (s) (*e)->ReleaseShortArrayElements(e, speech, s, JNI_ABORT);
  free(out); free(tx); free(fin);
  return res;
}

/* Radio sound in (8 kHz, any count), speech out (16 kHz). out must hold at least 4 * 1920 samples per 960 samples in.
   Returns the speech samples written. */
JNIEXPORT jint JNICALL F(receive)(JNIEnv *e, jclass c, jlong hh, jshortArray in, jint n, jshortArray out, jint cap) {
  Rade *h = (Rade *)(intptr_t)hh;
  jshort *x = (*e)->GetShortArrayElements(e, in, 0); jshort *y = (*e)->GetShortArrayElements(e, out, 0);
  int w = 0;
  for (int i = 0; i < n; i++) {
    /* a real signal from the radio's speaker: the imaginary part is unknown, so 0, and twice the scale (rade_api.h) */
    h->in[h->have].real = x[i] * (2.0f / RADE_INT16_SCALE); h->in[h->have].imag = 0; h->have++;
    if (h->have < rade_nin(h->r)) continue;
    int has_eoo = 0;
    int got = rade_rx(h->r, h->feat, &has_eoo, h->eoo, h->in);
    h->have = 0;
    if (has_eoo) h->eoo_seen++;
    for (int f = 0; f + NF <= got; f += NF) {
      float *feat = &h->feat[f];
      if (!h->fargan_ready) { /* FARGAN starts from 5 frames of features, as in rade_rx_wav.c */
        memcpy(&h->cont[h->cont_frames * NF], feat, NF * sizeof(float));
        if (++h->cont_frames >= 5) {
          float packed[5 * NB_FEATURES], zeros[FARGAN_CONT_SAMPLES] = {0};
          for (int k = 0; k < 5; k++) memcpy(&packed[k * NB_FEATURES], &h->cont[k * NF], NB_FEATURES * sizeof(float));
          fargan_cont(&h->fargan, zeros, packed); h->fargan_ready = 1;
        }
        continue;
      }
      float pcm[LPCNET_FRAME_SIZE];
      fargan_synthesize(&h->fargan, pcm, feat);
      for (int s = 0; s < LPCNET_FRAME_SIZE && w < cap; s++) y[w++] = to16(pcm[s]);
    }
  }
  (*e)->ReleaseShortArrayElements(e, in, x, JNI_ABORT); (*e)->ReleaseShortArrayElements(e, out, y, 0);
  return w;
}

JNIEXPORT jboolean JNICALL F(sync)(JNIEnv *e, jclass c, jlong hh) { return rade_sync(((Rade *)(intptr_t)hh)->r) != 0; }
JNIEXPORT jfloat JNICALL F(snr)(JNIEnv *e, jclass c, jlong hh) { return rade_snrdB_3k_est(((Rade *)(intptr_t)hh)->r); }
/* End-of-over frames seen since the last call. */
JNIEXPORT jint JNICALL F(endOfOver)(JNIEnv *e, jclass c, jlong hh) { Rade *h = (Rade *)(intptr_t)hh; int k = h->eoo_seen; h->eoo_seen = 0; return k; }
