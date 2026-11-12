# Compile flags for the modem library, shared by build.sh and tests/radio/build.sh: only the FreeDV modes the app uses.
FDVFLAGS='-DGIT_HASH="310777b" -DFREEDV_MODE_EN_DEFAULT=0 -DFREEDV_MODE_700D_EN=1 -DFREEDV_MODE_700E_EN=1 -DFREEDV_MODE_DATAC1_EN=1 -DFREEDV_MODE_DATAC3_EN=1 -DFREEDV_MODE_DATAC4_EN=1'
