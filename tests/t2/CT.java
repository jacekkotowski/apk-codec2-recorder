package pl.dyktafon;
import java.nio.*;import java.nio.file.*;
public class CT{public static void main(String[] a)throws Exception{byte[] b=Files.readAllBytes(Paths.get(a[0]));int n=b.length/2;ByteBuffer B=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);short[] x=new short[n];for(int i=0;i<n;i++)x[i]=B.getShort();
 // 8 kHz speech up to 44.1 kHz by linear interpolation, quiet, then back through Resampler and VoiceChain
 for(int rate:new int[]{44100,48000,8000}){int m=(int)(n*(long)rate/8000);double[] up=new double[m];for(int i=0;i<m;i++){double p=i*8000.0/rate;int k=(int)p;double f=p-k;up[i]=(k+1<n?x[k]*(1-f)+x[k+1]*f:0)*0.1;}
  double[] count={0,0};VoiceChain c=new VoiceChain(320,true,0.18,1,null);short[] pcm=new short[320];int[] fill={0};Resampler r=new Resampler(rate,v->{pcm[fill[0]++]=(short)Math.max(-32767,Math.min(32767,v));if(fill[0]==320){fill[0]=0;c.process(pcm,-100,5,1);for(short q:pcm){count[0]=Math.max(count[0],Math.abs(q));}count[1]++;}});
  for(double v:up)r.put(v);System.out.printf("rate %d: %.0f frames (expected %d), output peak %.0f, gain %.1f dB%n",rate,count[1],n/320,count[0],20*Math.log10(c.currentGain));}}}
