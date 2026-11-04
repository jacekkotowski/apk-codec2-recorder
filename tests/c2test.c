#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "codec2.h"
int main(int c,char**v){int M[]={0,1,2,3,4,5,8,10};const char*N[]={"3200","2400","1600","1400","1300","1200","700C","450"};
 FILE*f=fopen(v[1],"rb");fseek(f,0,2);long n=ftell(f)/2;rewind(f);short*x=malloc(n*2),*y=calloc(n+640,2);fread(x,2,n,f);
 for(int k=0;k<8;k++){struct CODEC2*h=codec2_create(M[k]);int s=codec2_samples_per_frame(h),b=codec2_bytes_per_frame(h);unsigned char*bits=malloc(b);long fr=n/s;
  struct CODEC2*d=codec2_create(M[k]);for(long i=0;i<fr;i++){codec2_encode(h,bits,x+i*s);codec2_decode(d,y+i*s,bits);}
  /* speech envelope correlation over 20 ms, allowing delay */
  int w=160;long W=(fr*s)/w;double best=0;for(int lag=0;lag<40;lag++){double sa=0,sb=0,sab=0,saa=0,sbb=0;long m=0;for(long i=0;i+lag<W;i++){double a=0,bb=0;for(int j=0;j<w;j++){a+=abs(x[i*w+j]);bb+=abs(y[(i+lag)*w+j]);}a=log(a+1);bb=log(bb+1);sa+=a;sb+=bb;sab+=a*bb;saa+=a*a;sbb+=bb*bb;m++;}double r=(sab-sa*sb/m)/sqrt((saa-sa*sa/m)*(sbb-sb*sb/m)+1e-9);if(r>best)best=r;}
  printf("%s: frame %d smp %d B, %.0f b/s, envelope corr %.3f\n",N[k],s,b,b*8*8000.0/s,best);codec2_destroy(h);codec2_destroy(d);}
 return 0;}
