package pl.dyktafon;
import java.nio.*;import java.nio.file.*;import java.util.*;
public class L{static double band(double[] x,int st,int en,double fl,double fh){int N=512;double tot=0;int c=0;double[] re=new double[N],im=new double[N];for(int s=st;s+N<=en;s+=N/2){for(int k=fl<1?0:0;k<N/2;k++){}double e=0;for(int k=(int)(fl/15.625);k<(int)(fh/15.625);k++){double cs=0,sn=0;for(int i=0;i<N;i+=1){double w=.5-.5*Math.cos(2*Math.PI*i/N);cs+=x[s+i]*w*Math.cos(2*Math.PI*k*i/N);sn+=x[s+i]*w*Math.sin(2*Math.PI*k*i/N);}e+=cs*cs+sn*sn;}tot+=e;c++;}return tot/Math.max(c,1);}
static double[] run(short[] in,int rum,double lift,int treble,boolean dn){short[] p=new short[320];double[] o=new double[in.length];VoiceChain c=new VoiceChain(320,dn,.18,treble,null,rum,lift,12);for(int i=0;i+320<=in.length;i+=320){System.arraycopy(in,i,p,0,320);c.process(p,-100,5,1);for(int j=0;j<320;j++)o[i+j]=p[j];}return o;}
public static void main(String[] a)throws Exception{byte[] b=Files.readAllBytes(Paths.get(a[0]));int n=b.length/2;ByteBuffer B=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);double[] s=new double[n];for(int i=0;i<n;i++)s[i]=B.getShort();
 // muffled mic: one-pole low-pass at 1 kHz, speech at modest level; rumble at 50 Hz + thumps; quiet hiss
 double al=Math.exp(-2*Math.PI*1000/8000),lp=0;Random R=new Random(2);int pad=16000;short[] in=new short[n+2*pad];for(int i=0;i<in.length;i++){double v=0;if(i>=pad&&i<pad+n){lp=al*lp+(1-al)*s[i-pad];v=lp*.5;}double t=i/8000.0;double thump=1500*Math.exp(-((t%0.85))*30)*Math.sin(2*Math.PI*45*t);v+=thump+600*Math.sin(2*Math.PI*50*t)+60*R.nextGaussian();in[i]=(short)Math.max(-32000,Math.min(32000,v));}
 double[] raw=new double[in.length];for(int i=0;i<in.length;i++)raw[i]=in[i];
 int F=256;java.util.function.BiFunction<double[],int[],Double> dummy=null;
 // consonant frames chosen from the clean speech: highs stronger than mids
 List<Integer> cons=new ArrayList<>(),vow=new ArrayList<>();for(int st=0;st+F<=n;st+=F){double h=0,l=0,lp2=0,e=0;for(int i=st;i<st+F;i++){lp2=al*lp2+(1-al)*s[i];double hi=s[i]-lp2;h+=hi*hi;l+=lp2*lp2;}if(h>1.0*l&&h>1e4*F/1000*1)cons.add(st);else if(l>4*h&&l>1e5*F/100)vow.add(st);}
 System.out.println("consonant frames "+cons.size()+", vowel frames "+vow.size());
 double[][] res=null;Object[][] cfg={{"raw",0,0.0,0,false},{"treble only, no denoise",180,0.0,1,false},{"treble + lift +6, no denoise",180,6.0,1,false},{"treble + lift +10, no denoise",180,10.0,1,false},{"denoise + treble",180,0.0,1,true},{"denoise + treble + lift +6",180,6.0,1,true},{"denoise + treble + lift +10",180,10.0,1,true}};
 double base=0;for(Object[] c:cfg){double[] o=c[0].equals("raw")?raw:run(in,(int)c[1],(double)c[2],(int)c[3],(boolean)c[4]);int lag=c[0].equals("raw")?0:((boolean)c[4]?256:0);
  double hc=0,mv=0;for(int st:cons)hc+=band(o,pad+st+lag,pad+st+lag+512,2500,3800);hc/=Math.max(cons.size(),1);for(int st:vow)mv+=band(o,pad+st+lag,pad+st+lag+512,300,1500);mv/=Math.max(vow.size(),1);double pause=band(o,4000,pad-500,2500,3800);
  double d=10*Math.log10(hc/mv);if(c[0].equals("raw"))base=d;System.out.printf("%-32s consonant highs vs vowels %+6.1f dB (change %+5.1f), consonant vs pause highs %5.1f dB%n",c[0],d,d-base,10*Math.log10(hc/pause));}
}}
