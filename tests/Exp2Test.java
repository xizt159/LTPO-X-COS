package com.ltpoxcos;
import android.os.*;
import com.ltpoxcos.xp.*;
import java.lang.reflect.*;
public class Exp2Test {
 static int checks;static void yes(boolean value){checks++;if(!value)throw new AssertionError("exp2 "+checks);}
 static Object get(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(null);}
 static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{return XposedBridge.call(o.getClass().getDeclaredMethod(n,types),o,args);}
 public static void main(String[] args)throws Exception{
  VideoFrameEvidence.Sample a=new VideoFrameEvidence.Sample(10,20,100,100),b=new VideoFrameEvidence.Sample(10,20,136,125);
  yes(!VideoFrameEvidence.stablePlayback(a,b,1000));yes(VideoFrameEvidence.stablePlayback(a,b,1000,true));yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(10,20,136,173),1000,true));yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(10,20,100,125),1000,true));
  String root="  Layer [293] VRI-test.app/Main#0\nownerPid=42 frame=100 isVisible=true\n";
  RootFrame first=RootFrame.parse(root,"test.app",42);yes(first!=null);yes(RootFrame.same(first,RootFrame.parse(root,"test.app",42)));yes(!RootFrame.same(first,RootFrame.parse(root.replace("frame=100","frame=101"),"test.app",42)));yes(RootFrame.parse(root,"other.app",42)==null);yes(RootFrame.same(first,RootFrame.parse(root+root,"test.app",42)));
  Config.hz=144;Config.flags.put("gate144",true);SystemClock.now=10000;Handler main=new Handler();Idle144Policy.start(main,()->{});Idle144Policy.interaction();SurfaceSnapshot.dump=root+root;Idle144Policy.scene("test.app",42,true,true);
  Handler io=(Handler)get(Idle144Policy.class,"io");io.until(11000);yes(!Idle144Policy.active());io.until(12500);yes(Idle144Policy.active());yes(Idle144Policy.owner("test.app"));yes(!Idle144Policy.owner("other.app"));
  Method install=Hook.class.getDeclaredMethod("hookDefaultWindowRate",ClassLoader.class);install.setAccessible(true);install.invoke(null,Exp2Test.class.getClassLoader());com.android.server.wm.OplusRefreshRatePolicyImpl nativePolicy=new com.android.server.wm.OplusRefreshRatePolicyImpl();nativePolicy.mPickRefreshRateData.name="test.app";yes((Integer)call(nativePolicy,"getWindowPreferredModeId",new Class<?>[]{int.class},4)==3);nativePolicy.mPickRefreshRateData.name="other.app";yes((Integer)call(nativePolicy,"getWindowPreferredModeId",new Class<?>[]{int.class},4)==4);nativePolicy.mPickRefreshRateData.name="test.app";
  Method allowed=AnimationPolicy.class.getDeclaredMethod("allowed",int.class);allowed.setAccessible(true);allowed.invoke(null,85);yes(Idle144Policy.active());
  Config.flags.put("disable_ltpo",true);yes(!Idle144Policy.active());Config.flags.remove("disable_ltpo");yes(Idle144Policy.active());
  Idle144Policy.interaction();yes(!Idle144Policy.active());yes((Integer)call(nativePolicy,"getWindowPreferredModeId",new Class<?>[]{int.class},4)==4);io.until(15000);yes(Idle144Policy.active());SurfaceSnapshot.dump=root.replace("frame=100","frame=101");io.until(15500);yes(!Idle144Policy.active());io.until(16000);yes(Idle144Policy.active());Idle144Policy.scene("test.app",42,true,false);yes(!Idle144Policy.active());Config.hz=165;Config.flags.clear();
  yes(!ScopeRestart.COMMAND.contains("system_server"));yes(ScopeRestart.COMMAND.contains("com.android.systemui"));
  System.out.println("PASS "+checks+" exp2 ambient/144-transition/wallpaper/double-tap assertions");
 }
}
