/* Codec 2 round trip for comparison with DRED: 8 kHz, 16 bit, mono raw in and out. Prints the stored size as the app stores it (whole bytes per frame). */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "codec2.h"
int main(int c, char **v) {
  static const char *N[] = {"3200", "2400", "1600", "1400", "1300", "1200", "700C", "450"};
  static const int M[] = {0, 1, 2, 3, 4, 5, 8, 10};
  if (c != 4) { fprintf(stderr, "usage: c2rt MODE in8k.raw out8k.raw\n"); return 2; }
  int mode = -1; for (int i = 0; i < 8; i++) if (!strcmp(v[1], N[i])) mode = M[i];
  if (mode < 0) return 2;
  FILE *fi = fopen(v[2], "rb"), *fo = fopen(v[3], "wb"); if (!fi || !fo) return 1;
  struct CODEC2 *e = codec2_create(mode), *d = codec2_create(mode);
  if (mode == 8) codec2_700c_eq(e, 1); /* as the app does */
  int n = codec2_samples_per_frame(e), nb = codec2_bytes_per_frame(e);
  short *s = malloc(n * sizeof(short)); unsigned char *b = malloc(nb); long frames = 0;
  while ((int)fread(s, sizeof(short), n, fi) == n) { codec2_encode(e, b, s); codec2_decode(d, s, b); fwrite(s, sizeof(short), n, fo); frames++; }
  double sec = frames * n / 8000.0;
  printf("c2 %s seconds %.1f bytes %ld bit_per_s %.0f kB_per_min %.2f\n", v[1], sec, frames * nb, frames * nb * 8 / sec, frames * nb * 60 / sec / 1000);
  return 0;
}
