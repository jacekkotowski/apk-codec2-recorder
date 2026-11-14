/* Can the FreeDV 700D and 700E voice modems carry our own bytes in place of Codec 2 voice?
   Sends random payloads through freedv_rawdatatx / freedv_rawdatarx, with optional white noise, and counts what arrives.
   Build from the project folder:
     . ./fdvflags.sh; gcc -O2 -DNDEBUG $FDVFLAGS -Ithird_party/codec2-1.2/src -Ithird_party/codec2-1.2/generated \
       -o tests/dred/fdvpipe tests/dred/fdvpipe.c third_party/codec2-1.2/src/*.c third_party/codec2-1.2/generated/*.c -lm
   Run: tests/dred/fdvpipe 700D|700E [SNR_dB in 3 kHz, default none] */
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "freedv_api.h"

static double gauss(void) {
  double u = (rand() + 1.0) / (RAND_MAX + 2.0), v = (rand() + 1.0) / (RAND_MAX + 2.0);
  return sqrt(-2 * log(u)) * cos(2 * M_PI * v);
}

int main(int c, char **v) {
  if (c < 2) return 2;
  int mode = !strcmp(v[1], "700E") ? FREEDV_MODE_700E : FREEDV_MODE_700D;
  struct freedv *tx = freedv_open(mode), *rx = freedv_open(mode);
  freedv_set_clip(tx, 1); freedv_set_tx_bpf(tx, 1);
  int bits = freedv_get_bits_per_modem_frame(tx), bytes = (bits + 7) / 8, n = freedv_get_n_tx_modem_samples(tx);
  int frames = 400;
  short *sig = malloc(sizeof(short) * n * (frames + 20)); unsigned char *sent = malloc(bytes * frames), got[64];
  srand(7);
  for (int i = 0; i < frames; i++) {
    for (int k = 0; k < bytes; k++) sent[i * bytes + k] = rand() & 255;
    if (bits % 8) sent[i * bytes + bytes - 1] &= 0xff << (8 - bits % 8);
    freedv_rawdatatx(tx, sig + i * n, sent + i * bytes);
  }
  long total = (long)n * frames;
  memset(sig + total, 0, sizeof(short) * n * 20); total += n * 20;
  if (c > 2) {
    double p = 0; for (long i = 0; i < (long)n * frames; i++) p += (double)sig[i] * sig[i];
    p /= (double)n * frames;
    /* noise power in 3 kHz of the 4 kHz band the samples cover */
    double sd = sqrt(p / pow(10, atof(v[2]) / 10) * 4000.0 / 3000.0);
    for (long i = 0; i < total; i++) { double x = sig[i] + sd * gauss(); sig[i] = x > 32767 ? 32767 : x < -32768 ? -32768 : (short)x; }
  }
  long pos = 0; int ok = 0, wrong = 0, flagged = 0;
  while (pos + freedv_nin(rx) <= total) {
    int nin = freedv_nin(rx);
    int nb = freedv_rawdatarx(rx, got, sig + pos); pos += nin;
    if (nb <= 0) continue;
    int hit = 0;
    for (int i = 0; i < frames && !hit; i++) hit = !memcmp(got, sent + i * bytes, bytes);
    if (hit) ok++; else { wrong++; if (freedv_get_rx_status(rx) & FREEDV_RX_BIT_ERRORS) flagged++; }
  }
  printf("%s: %d bits (%d bytes) per frame of %.0f ms = %.0f bit/s. Sent %d frames, arrived intact %d, arrived damaged %d (of those, flagged by the modem %d)\n",
         v[1], bits, bytes, n / 8.0, bits * 8000.0 / n, frames, ok, wrong, flagged);
  return 0;
}
