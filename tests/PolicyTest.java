package com.refresh165;
import java.util.Arrays;
public class PolicyTest {
 static int checks;
 static void yes(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 static void eq(int a,int b){checks++;if(a!=b)throw new AssertionError(a+" != "+b);}
 static String layer(String pkg,int pid,int id,long frame){return "layer: format:YUV dataspace:BT709 name:SurfaceView["+pkg+"/Player]\n"
  +"  Layer ["+id+"] SurfaceView["+pkg+"/Player](BLAST)#7\nownerPid="+pid+" frame="+frame+" bounds={0,0,1080,800}\n"
  +"isVisible=true isOpaque=true blend=NONE (1) alpha=1.000000\nrequestedFrameRate: {30 Hz}\n";}
 public static void main(String[]args){
  eq(7,RateProfile.highRateId(id->id==7?165:0));
  eq(9,RateProfile.highRateId(id->id==9?165:id==4?144:0));
  eq(-1,RateProfile.highRateId(id->id==8||id==9?165:0));
  eq(-1,RateProfile.highRateId(id->0));
  eq(-1,RateProfile.highRateId(id->{throw new IllegalArgumentException();}));
  for(int mode:new int[]{165})for(int peak:new int[]{165}){
   int[] nativeTable={10,33,33,peak,33,peak,33,peak,33,peak,33,0,0};int[] saved=nativeTable.clone();
   int[] idle=RateProfile.reserveIdleTable(mode,nativeTable);yes(idle!=null&&idle!=nativeTable);yes(Arrays.equals(nativeTable,saved));
   eq(10,idle[0]);eq(0,idle[11]);eq(0,idle[12]);for(int i=1;i<=10;i++)eq(1,idle[i]);
  }
  yes(RateProfile.reserveIdleTable(120,new int[13])==null);
  yes(RateProfile.reserveIdleTable(144,new int[10])==null);
  yes(RateProfile.reserveIdleTable(144,null)==null);
  yes(RateProfile.reserveIdleTable(144,new int[]{10,33,33,144,33,144,33,144,33,144,33,0,1})==null);
  eq(165,AdaptiveState.effective(true,false));eq(60,AdaptiveState.effective(true,true));
  eq(0,AdaptiveState.effective(false,true));eq(0,AdaptiveState.effective(false,false));
  ImeLatch ime=new ImeLatch();yes(!ime.observe(true,0));eq(300,(int)ime.deadline());yes(!ime.observe(true,100));yes(!ime.value(299));yes(ime.value(300));
  yes(ime.observe(false,310));yes(ime.value(659));yes(!ime.value(660));
  yes(!ime.observe(true,1000));yes(!ime.observe(false,1100));eq(-1,(int)ime.deadline());yes(!ime.value(1200));
  yes(!ime.observe(true,1300));yes(!ime.value(1599));yes(ime.value(1600));
  for(int key=0;key<20;key++){long at=1700+100*key;yes(ime.observe(true,at));yes(ime.observe(false,at+5));yes(ime.observe(true,at+20));}
  ime.clear();yes(!ime.value(10000));eq(-1,(int)ime.deadline());
  yes(!ime.observe(true,11000));ime.clear();yes(!ime.value(12000));
  System.out.println("PASS "+checks+" profile/IME assertions");
 }
}
