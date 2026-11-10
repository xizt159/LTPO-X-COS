package com.ltpoxcos;
import java.lang.reflect.*;
import java.util.*;
import android.os.*;
import com.ltpoxcos.xp.*;

public final class UnifiedTest {
    static int checks;
    static void yes(boolean b){checks++;if(!b)throw new AssertionError("unified check "+checks);}
    static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{return XposedBridge.call(o.getClass().getDeclaredMethod(name,types),o,args);}
    static Object instance(String name)throws Exception{return Class.forName(name).getConstructor().newInstance();}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void setStatic(Class<?> c,String name,Object value)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static Object getStatic(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    public static void main(String[] a)throws Exception{
        for(int rate:new int[]{60,90,120}){
            int[] source={10,1,1,30,1,rate,1,rate,10,rate,10,0,0},saved=source.clone();int[] out=NativeTables.lower(rate,source);
            yes(out!=null&&out!=source);yes(Arrays.equals(source,saved));for(int i=1;i<=10;i++)yes(out[i]==1);yes(out[0]==10&&out[11]==0&&out[12]==0);
            source[9]++;yes(NativeTables.lower(rate,source)==null);
        }
        yes(NativeTables.lower(144,new int[13])==null);yes(NativeTables.lower(60,null)==null);
        NativeReservePolicy.install(UnifiedTest.class.getClassLoader());
        com.android.server.display.feature.panel.hwdpfeatures.OplusHwDisplayRusAdfr rus=new com.android.server.display.feature.panel.hwdpfeatures.OplusHwDisplayRusAdfr();
        int[] normal=rus.mAdfrXmlConfig.mMinfps60level,reserve=rus.mAdfrXmlConfig.mReserveMinfpslevel1;
        rus.duringSend=()->{yes(rus.mAdfrXmlConfig.mMinfps60level!=normal);yes(rus.mAdfrXmlConfig.mMinfps60level[3]==1);yes(rus.mAdfrXmlConfig.mMinfps90level[5]==1);yes(rus.mAdfrXmlConfig.mMinfps120level[9]==1);};
        call(rus,"updateAdfrXmlConfig",new Class<?>[]{String.class},"native");yes(rus.mAdfrXmlConfig.mMinfps60level==normal);yes(rus.mAdfrXmlConfig.mReserveMinfpslevel1==reserve);
        rus.fail=true;try{call(rus,"updateAdfrXmlConfig",new Class<?>[]{String.class},"native");}catch(Exception expected){}yes(rus.mAdfrXmlConfig.mMinfps60level==normal);yes(rus.mAdfrXmlConfig.mReserveMinfpslevel1==reserve);rus.fail=false;
        Config.flags.put("tables",false);rus.duringSend=()->{yes(rus.mAdfrXmlConfig.mMinfps60level==normal);yes(rus.mAdfrXmlConfig.mReserveMinfpslevel1==reserve);};call(rus,"updateAdfrXmlConfig",new Class<?>[]{String.class},"native");Config.flags.clear();
        AdfrVersion v=new AdfrVersion();int[] p=new int[225];p[0]=1;p[1]=20250925;
        yes(v.outgoing(p)[1]==20250925);p[74]=10;yes(v.outgoing(p)[1]==20250926);yes(v.outgoing(p)[1]==20250926);
        p[74]=0;yes(v.outgoing(p)[1]==20250927);yes(p[1]==20250925);p[13]=1;yes(v.outgoing(p)==p);
        yes(v.outgoing(new int[13]).length==13);
        yes(AodValues.scale(-1f,160)==-1f);yes(Float.isNaN(AodValues.scale(Float.NaN,160)));yes(AodValues.scale(0,160)==0);
        yes(Math.abs(AodValues.scale(.25f,160)-.4f)<.0001f);yes(AodValues.scale(.9f,300)==1f);yes(AodValues.scale(2f,200)==2f);
        yes(AodValues.initial(20,80,160)==80);yes(AodValues.initial(40,80,160)==160);yes(AodValues.initial(-1,80,160)==-1);
        com.oplus.vrr.OPlusExternalRefreshRateManager m=new com.oplus.vrr.OPlusExternalRefreshRateManager();m.mCurrentModeId=144;
        yes(NativeIdlePolicy.eligible(m,true).equals("unsupported-mode"));Config.flags.put("gate144",true);yes(NativeIdlePolicy.eligible(m,true).equals("unsupported-mode"));
        m.mKillerPattern.mEnable=true;yes(NativeIdlePolicy.eligible(m,true).equals("active-or-unknown-killer"));m.mKillerPattern.mEnable=false;
        m.mLockRefreshRate=true;yes(NativeIdlePolicy.eligible(m,true).equals("native-hw-guard"));m.mLockRefreshRate=false;
        Config.flags.put("low_light",false);yes(NativeIdlePolicy.eligible(m,true).equals("disabled-by-user"));Config.flags.clear();
        Method discover=Hook.class.getDeclaredMethod("hasExpectedRateMap",ClassLoader.class);discover.setAccessible(true);yes((Boolean)discover.invoke(null,UnifiedTest.class.getClassLoader()));
        for(int[] pair:new int[][]{{60,2},{90,1},{120,3},{144,4},{165,7}}){Config.hz=pair[0];yes(Hook.highRateId()==pair[1]);}Config.hz=165;
        AodPolicy.install(UnifiedTest.class.getClassLoader(),"com.android.systemui");AodPolicy.install(UnifiedTest.class.getClassLoader(),"com.oplus.aod");
        Object smooth=instance("com.oplus.systemui.aod.display.SmoothTransitionController");call(smooth,"initSmoothTransitionState",new Class<?>[0]);yes(!(Boolean)field(smooth,"isSupportPanoramic"));
        Config.flags.put("aod",true);call(smooth,"initSmoothTransitionState",new Class<?>[0]);yes((Boolean)field(smooth,"isSupportPanoramic"));yes((Boolean)field(smooth,"isSupportPanoramicAllDay"));
        Config.flags.put("aod",false);Config.fire();Field handler=AodPolicy.class.getDeclaredField("MAIN");handler.setAccessible(true);((Handler)handler.get(null)).until(SystemClock.now);yes(!(Boolean)field(smooth,"isSupportPanoramic"));yes(!(Boolean)field(smooth,"isSupportPanoramicAllDay"));
        Config.flags.put("aod",true);
        Object manager=instance("com.oplus.systemui.aod.aodclock.off.AodUpdateManager");yes((Boolean)call(manager,"needDisplayAodInSpecialRule",new Class<?>[0]));call(manager,"setIsHideBySpecialRule",new Class<?>[]{boolean.class},true);yes(!(Boolean)field(manager,"hidden"));
        Config.flags.put("dark_visible",false);yes(!(Boolean)call(manager,"needDisplayAodInSpecialRule",new Class<?>[0]));call(manager,"setIsHideBySpecialRule",new Class<?>[]{boolean.class},true);yes((Boolean)field(manager,"hidden"));
        for(String name:new String[]{"com.oplus.systemui.aod.display.OplusWakeUpController$AodSingleClickWakeUpCallback","com.oplus.systemui.aod.scene.AodSceneViewHolder$AodSceneGestureCallback","com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder$AodSingleClickWakeUpCallback","com.oplus.systemui.aod.scene.PanoramicAodGestureController$PanoramicAodGestureCallback"}){
            Object target=instance(name);call(target,"onClick",new Class<?>[]{com.android.systemui.keyguard.OplusBlackScreenGestureEvent.class},new com.android.systemui.keyguard.OplusBlackScreenGestureEvent());yes((Integer)field(target,"calls")==1);
            Config.flags.put("block_tap",false);call(target,"onClick",new Class<?>[]{com.android.systemui.keyguard.OplusBlackScreenGestureEvent.class},new com.android.systemui.keyguard.OplusBlackScreenGestureEvent());yes((Integer)field(target,"calls")==2);Config.flags.put("block_tap",true);
        }
        Object service=instance("com.android.systemui.doze.DozeService");call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},.25f);yes(Math.abs((Float)field(service,"sent")-.4f)<.0001f);
        Config.flags.put("running_brightness",false);call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},.25f);yes((Float)field(service,"sent")==.25f);
        Object ex=instance("com.oplus.systemui.aod.OplusDozeServiceExImpl");yes((Integer)call(ex,"setBrightnessBeforeDozing",new Class<?>[0])==80);
        Config.flags.put("running_brightness",true);call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},79f/254);yes(Math.abs((Float)field(service,"sent")-79f/254)<.00001f);
        call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},.25f);yes(Math.abs((Float)field(service,"sent")-.4f)<.00001f);
        Config.flags.put("initial_dark",false);yes((Integer)call(ex,"setBrightnessBeforeDozing",new Class<?>[0])==20);
        Object settings=instance("com.oplus.aod.util.SettingsUtils");yes((Integer)call(settings,"getKeyAodAllDaySupportSettings",new Class<?>[]{android.content.Context.class,int.class},new android.content.Context(),0)==1);
        Config.flags.put("all_day",false);yes((Integer)call(settings,"getKeyAodAllDaySupportSettings",new Class<?>[]{android.content.Context.class,int.class},new android.content.Context(),0)==0);
        Config.flags.clear();setStatic(AdaptivePolicy.class,"ready",true);setStatic(AdaptivePolicy.class,"interactive",true);setStatic(AdaptivePolicy.class,"worker",new Handler());
        Method recompute=AdaptivePolicy.class.getDeclaredMethod("recompute");recompute.setAccessible(true);
        for(int rate:new int[]{60,90,120,144,165}){
            Config.hz=rate;setStatic(AdaptivePolicy.class,"ime",false);setStatic(AdaptivePolicy.class,"rawIme",false);recompute.invoke(null);yes((Integer)getStatic(AdaptivePolicy.class,"cap")==rate);
            setStatic(AdaptivePolicy.class,"ime",true);setStatic(AdaptivePolicy.class,"rawIme",true);recompute.invoke(null);yes((Integer)getStatic(AdaptivePolicy.class,"cap")==60);
            Config.flags.put("ime",false);recompute.invoke(null);yes((Integer)getStatic(AdaptivePolicy.class,"cap")==rate);Config.flags.remove("ime");
        }
        Config.flags.put("refresh",false);recompute.invoke(null);yes((Integer)getStatic(AdaptivePolicy.class,"cap")==0);
        System.out.println("PASS "+checks+" unified adapters / per-feature off assertions");
    }
}
