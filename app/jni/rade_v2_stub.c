/* RADE V2 left out of the app: its waveform and weights are still changing and the FreeDV team asks that it not be used on air
   (rade_c README, 2026-10). rade_api.c names these functions but only calls them when a context is opened with RADE_MODE_V2,
   which the app never does. Each one fails, so a V2 open would be refused rather than run. Saves about 3 MB per processor type. */
#include "rade_tx_v2.h"
#include "rade_rx_v2.h"

int rade_tx_v2_init(rade_tx_v2_state *tx, int bpf_en) { (void)tx; (void)bpf_en; return -1; }
int rade_tx_v2_n_features_in(void) { return 0; }
int rade_tx_v2_n_samples_out(void) { return 0; }
int rade_tx_v2_n_eoo_out(void) { return 0; }
int rade_tx_v2_process(rade_tx_v2_state *tx, RADE_COMP *tx_out, const float *features_in) { (void)tx; (void)tx_out; (void)features_in; return 0; }
int rade_tx_v2_eoo(rade_tx_v2_state *tx, RADE_COMP *tx_out) { (void)tx; (void)tx_out; return 0; }
void rade_tx_v2_set_data_symbol(rade_tx_v2_state *tx, float symbol) { (void)tx; (void)symbol; }
int rade_rx_v2_init(rade_rx_v2_state *rx, int bpf_en) { (void)rx; (void)bpf_en; return -1; }
int rade_rx_v2_nin(const rade_rx_v2_state *rx) { (void)rx; return 0; }
int rade_rx_v2_nin_max(void) { return 0; }
int rade_rx_v2_n_features_out(void) { return 0; }
int rade_rx_v2_process(rade_rx_v2_state *rx, float *features_out, const RADE_COMP *rx_in) { (void)rx; (void)features_out; (void)rx_in; return 0; }
float rade_rx_v2_get_data_symbol(const rade_rx_v2_state *rx) { (void)rx; return 0; }
