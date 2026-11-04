package pl.dyktafon;
import java.nio.*;import java.nio.file.*;
public class C{public static void main(String[] a)throws Exception{byte[] b=Files.readAllBytes(Paths.get(a[0]));int n=b.length/2;ByteBuffer B=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);double[] s=new double[n];double pk=1;for(int i=0;i<n;i++){s[i]=B.getShort();pk=Math.max(pk,Math.abs(s[i]));}
 String[] names={"quiet","normal","loud","sudden loud burst","overloaded input"};double[] sc={.03,.3,1,0,1};
 for(int c=0;c<5;c++)for(int man:new int[]{-100,24}){int T=n*2;short[] in=new short[T];for(int i=0;i<T;i++){double v=s[i%n]/pk*32000;double g=sc[c];if(c==3)g=(i%n)<n/2?.02:1;if(c==4){v=Math.max(-32000,Math.min(32000,v*3));}in[i]=(short)Math.max(-32767,Math.min(32767,v*g));}
  VoiceChain ch=new VoiceChain(320,true,.18,1,null,180,6,12);short[] p=new short[320];java.io.ByteArrayOutputStream o=new java.io.ByteArrayOutputStream();double mx=0;long over=0;double lastRms=0,sum=0;long cnt=0;for(int i=0;i+320<=T;i+=320){System.arraycopy(in,i,p,0,320);ch.process(p,man,5,1);for(short q:p){o.write(q&255);o.write((q>>8)&255);mx=Math.max(mx,Math.abs(q));if(Math.abs(q)>VoiceChain.CEILING)over++;if(i>T/2){sum+=q*q;cnt++;}}}
  Files.write(Paths.get(a[1]+"_"+c+"_"+(man==-100?"auto":"m24")+".raw"),o.toByteArray());
  System.out.printf("%-20s %-5s peak %6.0f (ceiling %.0f), over ceiling %d, rms %6.0f, clipped-input flag %b%n",names[c],man==-100?"auto":"+24dB",mx,VoiceChain.CEILING,over,Math.sqrt(sum/Math.max(cnt,1)),ch.inputClipped);}}}
