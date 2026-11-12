/* Radio link: thin layer over the FreeDV API of Codec 2 1.2, and its JNI for pl.dyktafon.Radio.
   Data modes send fixed-size frames in bursts; voice modes turn speech into modem audio and back.
   All audio is 8 kHz, 16 bit, mono. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "freedv_api.h"

#define FIFO 16384
typedef struct {
  struct freedv *f;
  int data;            /* 1 for a data mode */
  int payload;         /* data: bytes per frame for the caller (without the CRC16) */
  short fifo[FIFO]; int have;
  short *speech; uint8_t *bytes;
} radio;

/* 0, 1, 2: files, fast to very robust. 10, 11: live voice. */
static int fdv_mode(int m) {
  switch (m) {
    case 0: return FREEDV_MODE_DATAC1;
    case 1: return FREEDV_MODE_DATAC3;
    case 2: return FREEDV_MODE_DATAC4;
    case 10: return FREEDV_MODE_700D;
    case 11: return FREEDV_MODE_700E;
  }
  return -1;
}

static radio *radio_open(int mode, int frames_per_burst) {
  int fm = fdv_mode(mode); if (fm < 0) return NULL;
  radio *r = calloc(1, sizeof(radio)); if (!r) return NULL;
  r->f = freedv_open(fm); if (!r->f) { free(r); return NULL; }
  r->data = mode < 10;
  if (r->data) {
    freedv_set_frames_per_burst(r->f, frames_per_burst);
    r->payload = freedv_get_bits_per_modem_frame(r->f) / 8 - 2;
    r->bytes = malloc(r->payload + 2);
  } else {
    freedv_set_clip(r->f, 1); freedv_set_tx_bpf(r->f, 1);   /* more average power for the same peak */
    freedv_set_squelch_en(r->f, 1);
    freedv_passthrough_gain(r->f, 0.0f);                    /* silence, not channel noise, while there is no signal */
    r->speech = malloc(sizeof(short) * freedv_get_n_max_speech_samples(r->f));
  }
  return r;
}

static void radio_close(radio *r) { if (!r) return; freedv_close(r->f); free(r->speech); free(r->bytes); free(r); }

static int radio_tx_max(radio *r) {
  int a = freedv_get_n_tx_modem_samples(r->f), b = freedv_get_n_tx_preamble_modem_samples(r->f), c = freedv_get_n_tx_postamble_modem_samples(r->f);
  int m = a > b ? a : b; m = m > c ? m : c;
  int g = 2 * freedv_get_n_nom_modem_samples(r->f);
  return m > g ? m : g;
}

static int radio_tx_frame(radio *r, const uint8_t *payload, short *out) {
  memcpy(r->bytes, payload, r->payload);
  unsigned short crc = freedv_gen_crc16(r->bytes, r->payload);
  r->bytes[r->payload] = crc >> 8; r->bytes[r->payload + 1] = crc & 0xff;
  freedv_rawdatatx(r->f, out, r->bytes);
  return freedv_get_n_tx_modem_samples(r->f);
}

/* Takes any number of samples. Writes whole good frames to out (payload bytes each), returns how many. */
static int radio_rx_data(radio *r, const short *in, int n, uint8_t *out, int max_frames) {
  int frames = 0;
  while (n > 0) {
    int take = FIFO - r->have; if (take > n) take = n;
    memcpy(r->fifo + r->have, in, take * sizeof(short)); r->have += take; in += take; n -= take;
    int pos = 0, nin;
    while ((nin = freedv_nin(r->f)) <= r->have - pos) {
      int nb = freedv_rawdatarx(r->f, r->bytes, r->fifo + pos); pos += nin;
      if (nb == r->payload + 2 && frames < max_frames) {
        unsigned short crc = freedv_gen_crc16(r->bytes, r->payload);
        if (r->bytes[r->payload] == (crc >> 8) && r->bytes[r->payload + 1] == (crc & 0xff)) { memcpy(out + frames * r->payload, r->bytes, r->payload); frames++; }
      }
    }
    memmove(r->fifo, r->fifo + pos, (r->have - pos) * sizeof(short)); r->have -= pos;
  }
  return frames;
}

/* Takes any number of samples, writes decoded speech to out, returns the sample count (at most max). */
static int radio_rx_voice(radio *r, const short *in, int n, short *out, int max) {
  int made = 0;
  while (n > 0) {
    int take = FIFO - r->have; if (take > n) take = n;
    memcpy(r->fifo + r->have, in, take * sizeof(short)); r->have += take; in += take; n -= take;
    int pos = 0, nin;
    while ((nin = freedv_nin(r->f)) <= r->have - pos) {
      int ns = freedv_rx(r->f, r->speech, r->fifo + pos); pos += nin;
      if (ns > max - made) ns = max - made;
      memcpy(out + made, r->speech, ns * sizeof(short)); made += ns;
    }
    memmove(r->fifo, r->fifo + pos, (r->have - pos) * sizeof(short)); r->have -= pos;
  }
  return made;
}

#define F(n) Java_pl_dyktafon_Radio_##n
#define R ((radio *)(intptr_t)h)
JNIEXPORT jlong JNICALL F(open)(JNIEnv *e, jclass c, jint mode, jint fpb) { return (jlong)(intptr_t)radio_open(mode, fpb); }
JNIEXPORT void JNICALL F(close)(JNIEnv *e, jclass c, jlong h) { radio_close(R); }
JNIEXPORT jint JNICALL F(payload)(JNIEnv *e, jclass c, jlong h) { return R->payload; }
JNIEXPORT jint JNICALL F(txMax)(JNIEnv *e, jclass c, jlong h) { return radio_tx_max(R); }
JNIEXPORT jint JNICALL F(preamble)(JNIEnv *e, jclass c, jlong h, jshortArray out) {
  jshort *o = (*e)->GetShortArrayElements(e, out, 0); int n = freedv_rawdatapreambletx(R->f, o);
  (*e)->ReleaseShortArrayElements(e, out, o, 0); return n;
}
JNIEXPORT jint JNICALL F(postamble)(JNIEnv *e, jclass c, jlong h, jshortArray out) {
  jshort *o = (*e)->GetShortArrayElements(e, out, 0); int n = freedv_rawdatapostambletx(R->f, o);
  (*e)->ReleaseShortArrayElements(e, out, o, 0); return n;
}
JNIEXPORT jint JNICALL F(gap)(JNIEnv *e, jclass c, jlong h) { return 2 * freedv_get_n_nom_modem_samples(R->f); }
JNIEXPORT jint JNICALL F(frame)(JNIEnv *e, jclass c, jlong h, jbyteArray in, jshortArray out) {
  jbyte *b = (*e)->GetByteArrayElements(e, in, 0); jshort *o = (*e)->GetShortArrayElements(e, out, 0);
  int n = radio_tx_frame(R, (const uint8_t *)b, o);
  (*e)->ReleaseByteArrayElements(e, in, b, JNI_ABORT); (*e)->ReleaseShortArrayElements(e, out, o, 0); return n;
}
JNIEXPORT jint JNICALL F(rxData)(JNIEnv *e, jclass c, jlong h, jshortArray in, jint n, jbyteArray out) {
  jshort *s = (*e)->GetShortArrayElements(e, in, 0); jbyte *b = (*e)->GetByteArrayElements(e, out, 0);
  int frames = radio_rx_data(R, s, n, (uint8_t *)b, (*e)->GetArrayLength(e, out) / R->payload);
  (*e)->ReleaseShortArrayElements(e, in, s, JNI_ABORT); (*e)->ReleaseByteArrayElements(e, out, b, 0); return frames;
}
JNIEXPORT jint JNICALL F(speechN)(JNIEnv *e, jclass c, jlong h) { return freedv_get_n_speech_samples(R->f); }
JNIEXPORT jint JNICALL F(modemN)(JNIEnv *e, jclass c, jlong h) { return freedv_get_n_nom_modem_samples(R->f); }
JNIEXPORT void JNICALL F(txVoice)(JNIEnv *e, jclass c, jlong h, jshortArray speech, jshortArray mod) {
  jshort *s = (*e)->GetShortArrayElements(e, speech, 0), *o = (*e)->GetShortArrayElements(e, mod, 0);
  freedv_tx(R->f, o, s);
  (*e)->ReleaseShortArrayElements(e, speech, s, JNI_ABORT); (*e)->ReleaseShortArrayElements(e, mod, o, 0);
}
JNIEXPORT jint JNICALL F(rxVoice)(JNIEnv *e, jclass c, jlong h, jshortArray in, jint n, jshortArray out) {
  jshort *s = (*e)->GetShortArrayElements(e, in, 0), *o = (*e)->GetShortArrayElements(e, out, 0);
  int made = radio_rx_voice(R, s, n, o, (*e)->GetArrayLength(e, out));
  (*e)->ReleaseShortArrayElements(e, in, s, JNI_ABORT); (*e)->ReleaseShortArrayElements(e, out, o, 0); return made;
}
JNIEXPORT jint JNICALL F(sync)(JNIEnv *e, jclass c, jlong h) { int s; float snr; freedv_get_modem_stats(R->f, &s, &snr); return s; }
JNIEXPORT jfloat JNICALL F(snr)(JNIEnv *e, jclass c, jlong h) { int s; float snr; freedv_get_modem_stats(R->f, &s, &snr); return snr; }
JNIEXPORT jint JNICALL F(frameN)(JNIEnv *e, jclass c, jlong h) { return freedv_get_n_tx_modem_samples(R->f); }
