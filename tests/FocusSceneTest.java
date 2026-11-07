package com.refresh165;
import android.os.*;import java.lang.reflect.*;import com.refresh165.xp.*;import com.android.server.wm.DisplayContent;
public class FocusSceneTest {
 static int checks;static void yes(boolean b){checks++;if(!b)throw new AssertionError("focus "+checks);}static Object get(String n)throws Exception{Field f=AdaptivePolicy.class.getDeclaredField(n);f.setAccessible(true);return f.get(null);}static void set(String n,Object v)throws Exception{Field f=AdaptivePolicy.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}static void settle()throws Exception{((Handler)get("worker")).until(SystemClock.now);}
 public static void main(String[]args)throws Exception{
  Config.flags.put("app_policies",true);Config.flags.put("video",false);Config.flags.put("ime",false);Config.flags.put("disable_ltpo",false);Config.hz=165;Config.profiles=AppProfiles.parse("com.android.chrome=v120;com.test.mail=v90");
  AdaptivePolicy.install(FocusSceneTest.class.getClassLoader());set("context",new android.content.Context());set("ready",true);DisplayContent display=new DisplayContent();DisplayContent.Window window=new DisplayContent.Window();display.mCurrentFocus=window;Method focus=DisplayContent.class.getDeclaredMethod("updateFocusedWindowLocked",int.class,boolean.class,int.class);yes((Boolean)get("focusHook"));
  XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==120);yes((Integer)get("cap")==120);yes(get("pkg").equals("com.android.chrome"));yes((Boolean)get("sceneFull"));yes((Long)get("sceneReads")==1);
  window.pkg="com.test.mail";XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==90&&(Integer)get("cap")==90);yes(get("pkg").equals("com.test.mail"));
  window.pkg="com.android.chrome";XposedBridge.call(focus,display,0,true,0);window.pkg="com.test.mail";XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==90);yes((Long)get("sceneReads")==4);
  DisplayContent.Task.multi=true;XposedBridge.call(focus,display,0,true,0);settle();yes(!(Boolean)get("sceneFull"));yes(Config.effectiveRate()==165);DisplayContent.Task.multi=false;
  window.pkg="com.android.chrome";XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==120);window.mAttrs.type=2011;XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==120&&(Boolean)get("sceneFull"));window.mAttrs.type=0;
  window.pkg="com.test.unconfigured";XposedBridge.call(focus,display,0,true,0);settle();yes(Config.effectiveRate()==165&&(Integer)get("cap")==165);
  PowerManager.interactive=false;XposedBridge.call(focus,display,0,true,0);settle();yes((Integer)get("cap")==0);
  System.out.println("PASS "+checks+" native focus-event / no-surface-traversal / coalescing / split-screen / keyboard owner / screen-off assertions");
 }
}
