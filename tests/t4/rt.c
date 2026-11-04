#include <stdio.h>
#include <stdlib.h>
#include "codec2.h"
int main(int c,char**v){int M[]={0,8,10};const char*N[]={"3200","700C","450"};FILE*f=fopen(v[1],"rb");fseek(f,0,2);long n=ftell(f)/2;rewind(f);short*x=malloc(n*2);if(fread(x,2,n,f)!=n)return 1;
 for(int k=0;k<3;k++){struct CODEC2*h=codec2_create(M[k]),*d=codec2_create(M[k]);if(M[k]==8)codec2_700c_eq(h,1);int s=codec2_samples_per_frame(h),b=codec2_bytes_per_frame(h);unsigned char*bt=malloc(b);short*y=malloc(s*2);int pk=0;long sat=0;for(long i=0;i+s<=n;i+=s){codec2_encode(h,bt,x+i);codec2_decode(d,y,bt);for(int j=0;j<s;j++){int a=abs(y[j]);if(a>pk)pk=a;if(a>=32000)sat++;}}printf(" %s peak %d sat %ld;",N[k],pk,sat);}printf("\n");return 0;}
