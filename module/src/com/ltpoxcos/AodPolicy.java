package com.ltpoxcos;

import com.ltpoxcos.xp.*;
import java.lang.reflect.*;
import java.util.*;

/** Adapted to defined methods on CPH2745 ColorOS17; unavailable adapters fail closed. */
final class AodPolicy {
    private static final Map<Object,Map<String,Boolean>> ORIGINALS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final android.os.Handler MAIN=new android.os.Handler(android.os.Looper.getMainLooper());
    private static final ThreadLocal<Initial> INITIAL=new ThreadLocal<>();
    private static final class Initial {final int level;final long at=android.os.SystemClock.elapsedRealtime();Initial(int n){level=n;}}
    static void install(ClassLoader cl,String pkg){
        if("com.oplus.aod".equals(pkg)){
            hook(cl,"com.oplus.aod.util.SettingsUtils","getKeyAodAllDaySupportSettings",int.class,new Class<?>[]{android.content.Context.class,int.class},new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){if(Config.aod("all_day"))p.setResult(1);}
            });return;
        }
        AodBackdropPolicy.install(cl);PanoramicStatusPolicy.install(cl);AodLiftPolicy.install(cl);AodBatteryPolicy.install(cl);
        XC_MethodHook support=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.getThrowable()==null)applySupport(p.thisObject);}};
        for(String name:new String[]{"initSmoothTransitionState","setPanoramicSupportedByRemote"})hook(cl,"com.oplus.systemui.aod.display.SmoothTransitionController",name,void.class,new Class<?>[0],support);
        Config.listen(()->MAIN.post(()->{Object[] all;synchronized(ORIGINALS){all=ORIGINALS.keySet().toArray();}for(Object o:all){restore(o);applySupport(o);}}));
        hook(cl,"com.oplus.systemui.aod.OplusDozeServiceExImpl","setBrightnessBeforeDozing",int.class,new Class<?>[0],new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(p.getThrowable()==null&&p.getResult() instanceof Integer){int nativeValue=(Integer)p.getResult();if(Config.aod("fixed_brightness")){p.setResult(AodValues.fixedInitial(nativeValue,Config.number("fixed",80,2,255)));return;}if(!Config.aod(nativeValue<40?"initial_dark":"initial_bright"))return;int n=AodValues.initial(nativeValue,Config.number("dark",80,1,255),Config.number("bright",160,1,255));p.setResult(n);INITIAL.set(new Initial(n));}}
        });
        // One terminal path only, so fallback/forwarding cannot multiply brightness twice.
        hook(cl,"com.android.systemui.doze.DozeService","setDozeScreenBrightness",void.class,new Class<?>[]{float.class},new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){Initial token=INITIAL.get();INITIAL.remove();float value=(Float)p.args[0];boolean initial=token!=null&&android.os.SystemClock.elapsedRealtime()-token.at<1000&&Math.abs(value-(token.level-1)/254f)<.005f;if(Config.aod("fixed_brightness")){p.args[0]=AodValues.fixed(value,Config.number("fixed",80,2,255));return;}if(!initial&&Config.aod("running_brightness"))p.args[0]=AodValues.scale(value,Config.number("multiplier",160,25,300));}
        });
        hook(cl,"com.oplus.systemui.aod.aodclock.off.AodUpdateManager","needDisplayAodInSpecialRule",boolean.class,new Class<?>[0],new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(Config.aod("dark_visible"))p.setResult(true);}
        });
        hook(cl,"com.oplus.systemui.aod.aodclock.off.AodUpdateManager","setIsHideBySpecialRule",void.class,new Class<?>[]{boolean.class},new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(Config.aod("dark_visible"))p.args[0]=false;}
        });
        hook(cl,"com.oplus.systemui.aod.aodclock.off.AodUpdateManager$2","hideAodByDarkLight",void.class,new Class<?>[]{int.class},new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(Config.aod("dark_visible"))p.setResult(null);}
        });
        // Resolve event from the actual callback signature: its package changed across releases.
        for(String type:new String[]{"com.oplus.systemui.aod.display.OplusWakeUpController$AodSingleClickWakeUpCallback","com.oplus.systemui.aod.scene.AodSceneViewHolder$AodSceneGestureCallback","com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder$AodSingleClickWakeUpCallback","com.oplus.systemui.aod.scene.PanoramicAodGestureController$PanoramicAodGestureCallback"}){
            try{Class<?> c=Class.forName(type,false,cl);for(Method m:c.getDeclaredMethods())if(m.getName().equals("onClick")&&m.getReturnType()==void.class&&m.getParameterCount()==1&&m.getParameterTypes()[0].getSimpleName().equals("OplusBlackScreenGestureEvent"))XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(AodTapPolicy.block(p.thisObject,p.args[0]))p.setResult(null);}});}catch(Throwable ignored){}
        }
        hook(cl,"com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep$OnDoubleClickListener","onSingleTapConfirmed",boolean.class,new Class<?>[]{android.view.MotionEvent.class},new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(AodTapPolicy.block(p.thisObject,p.args[0]))p.setResult(true);}
        });
    }
    private static void applySupport(Object o){
        String[] fields={"isSupportPanoramic","isSupportPanoramicByPanelFeature","isSupportPanoramicAllDay","isSupportPanoramicAllDayByPanelFeature"};
        synchronized(ORIGINALS){Map<String,Boolean> old=ORIGINALS.computeIfAbsent(o,k->new HashMap<>());
            for(int i=0;i<fields.length;i++)if(Config.aod(i<2?"panoramic":"all_day"))try{Field f=o.getClass().getDeclaredField(fields[i]);f.setAccessible(true);old.putIfAbsent(fields[i],f.getBoolean(o));f.setBoolean(o,true);}catch(Throwable ignored){}
        }
    }
    private static void restore(Object o){synchronized(ORIGINALS){Map<String,Boolean> old=ORIGINALS.remove(o);if(old!=null)for(Map.Entry<String,Boolean> e:old.entrySet())try{Field f=o.getClass().getDeclaredField(e.getKey());f.setAccessible(true);if(f.getBoolean(o))f.setBoolean(o,e.getValue());}catch(Throwable ignored){}}}
    private static void hook(ClassLoader cl,String type,String name,Class<?> result,Class<?>[] args,XC_MethodHook callback){
        try{Method m=Class.forName(type,false,cl).getDeclaredMethod(name,args);if(m.getReturnType()==result)XposedBridge.hookMethod(m,callback);}catch(Throwable ignored){}
    }
}
