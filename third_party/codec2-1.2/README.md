# Codec 2 version 1.2.0 (modem part of the app)

Unchanged library sources from https://github.com/drowe67/codec2, commit 310777b1c6f1af0bc7c72f5b32f80f6fd9136962 (2026-03-17), LGPL 2.1 (`COPYING`).

- `src/`: the files of `CODEC2_SRCS` in upstream `src/CMakeLists.txt`, and the headers.
- `generated/`: codebooks, `config.h` and `codec2/version.h` as produced by upstream's CMake build on Linux.

Built into `libfdv.so` for the FreeDV modems (radio link). The recorder keeps using `third_party/codec2` (version 1.0) in `libc2.so`, because version 1.2 no longer has the 450 bit/s mode.
