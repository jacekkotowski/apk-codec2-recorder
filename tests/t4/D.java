package pl.dyktafon;
import java.nio.*;import java.nio.file.*;import java.util.*;
public class D{public static void main(String[] a)throws Exception{byte[] b=Files.readAllBytes(Paths.get(a[0]));int n=b.length/2;ByteBuffer B=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);double[] s=new double[n];double pk=1;for(int i=0;i<n;i++){s[i]=B.getShort();pk=Math.max(pk,Math.abs(s[i]));}
 Random R=new Random(4);int pad=24000,T=4*n+2*pad;double[] in=new double[T];
 // speech at four levels: 0 dB, -20 dB, -6 dB, overloaded; hiss throughout
 double[] sc={.9,.09,.45,3};for(int i=0;i<T;i++){double v=40*R.nextGaussian();if(i>=pad&&i<T-pad){int k=(i-pad);int seg=Math.min(3,k/n);v+=Math.max(-32000,Math.min(32000,s[k%n]/pk*32000*sc[seg]));}in[i]=Math.max(-32767,Math.min(32767,v));}
 for(double[] cf:new double[][]{{1,0},{1,12},{2,12},{3,12},{5,12}}){VoiceChain c=new VoiceChain(320,true,.18,1,null,180,6,cf[1],cf[0]);short[] p=new short[320],q;double[] seg=new double[4];int[] sn=new int[4];long over=0;double mx=0,pz=0;int pn=0;java.io.ByteArrayOutputStream o=new java.io.ByteArrayOutputStream();
  for(int i=0;i+320<=T;i+=320){for(int j=0;j<320;j++)p[j]=(short)in[i+j];c.process(p,-100,5,1);for(int j=0;j<320;j++){short v=p[j];o.write(v&255);o.write((v>>8)&255);mx=Math.max(mx,Math.abs(v));if(Math.abs(v)>VoiceChain.CEILING)over++;int k=i+j-pad;if(k>=n/4&&k<4*n){int g=k/n;seg[g]+=v*v;sn[g]++;}if(i+j>8000&&i+j<pad-2000){pz+=v*v;pn++;}}}
  Files.write(Paths.get("d_"+(int)cf[0]+"_"+(int)cf[1]+".raw"),o.toByteArray());double[] db=new double[4];double hi=-99,lo=99;for(int g=0;g<4;g++){db[g]=10*Math.log10(seg[g]/sn[g]+1);hi=Math.max(hi,db[g]);lo=Math.min(lo,db[g]);}
  System.out.printf("ratio %.0f:1 gate %2.0f  peak %6.0f over %d  level spread %4.1f dB (input 30.5)  speech vs pause %4.1f dB%n",cf[0],cf[1],mx,over,hi-lo,10*Math.log10((seg[0]/sn[0])/(pz/pn+1)));}}}
