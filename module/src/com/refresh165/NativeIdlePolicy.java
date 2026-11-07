package com.refresh165;

import com.refresh165.xp.*;
import java.lang.reflect.*;
import java.util.*;

/** Actual ColorOS17 OTI path. Native content and touch detection decide when
 * hardware idle is entered. No fixed-rate writes, polling, or forced sequence. */
final class NativeIdlePolicy {
    private static final ThreadLocal<ArrayDeque<Object>> SCOPE=new ThreadLocal<>();
    private static final Map<Object,Boolean> MANAGERS=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean configHook, supportHook, sendHook;
    private static volatile long configs, brightnessOverrides, sends, adapted, rechecks;
    private static final Map<Object,String> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile String reason="not-called", outgoing="not-sent";
    static void install(ClassLoader cl){
        try {
            Class<?> manager=Class.forName("com.oplus.vrr.OPlusExternalRefreshRateManager",false,cl);
            Class<?> bean=Class.forName("com.oplus.vrr.bean.TouchIdleBean",false,cl);
            Method configure=manager.getDeclaredMethod("handleTouchIdleConfig",boolean.class);configure.setAccessible(true);
            Method support=manager.getDeclaredMethod("isSupportHwWithHistogram",bean);
            Constructor<?> copy=bean.getDeclaredConstructor(bean);copy.setAccessible(true);
            Class<?> sf=Class.forName("com.oplus.vrr.OPlusSurfaceFlingerController",false,cl);
            Method send=sf.getDeclaredMethod("setTouchIdleConfig",bean);
            if(configure.getReturnType()!=void.class||support.getReturnType()!=boolean.class||send.getReturnType()!=void.class)return;
            supportHook=XposedBridge.hookMethod(support,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.getThrowable()!=null||current()!=p.thisObject||!Boolean.FALSE.equals(p.getResult()))return;
                    String why=eligible(p.thisObject,true);reason=why;
                    if("eligible".equals(why)){p.setResult(true);brightnessOverrides++;}
                }
            })!=null;
            sendHook=XposedBridge.hookMethod(send,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Object manager=current();if(manager==null)return;sends++;
                    try {
                        Object nativeBean=p.args[0];
                        if(AdaptivePolicy.holdFixedRate()){
                            // DMD's physical floor alone cannot stop internal panel self-refresh.
                            // Disable native touch-idle only on an outgoing private copy while held.
                            Object fixed=copy.newInstance(nativeBean);
                            write(fixed,"mEnable",false);write(fixed,"mHwEnable",false);write(fixed,"mSwEnable",false);
                            p.args[0]=fixed;outgoing="fixed-app-idle-disabled";return;
                        }

                        outgoing="hw:"+field(nativeBean,"mHwEnable")+",sw:"+field(nativeBean,"mSwEnable");
                        // Never force a disabled native hardware path on here.
                        if(!"eligible".equals(eligible(manager,false))
                            ||!Boolean.TRUE.equals(field(nativeBean,"mEnable"))
                            ||!Boolean.TRUE.equals(field(nativeBean,"mHwEnable")))return;
                        Object replacement=copy.newInstance(nativeBean);
                        boolean changed=Config.refresh("fast_idle")&&profile(replacement);
                        if(changed){p.args[0]=replacement;adapted++;}
                        outgoing="hw:"+field(replacement,"mHwEnable")+",sw:"+field(replacement,"mSwEnable")
                            +",timeout:"+field(replacement,"mTimeOut")+",gap:"+field(replacement,"mContentGapMs");
                    }catch(Throwable ignored){outgoing="native-preserved-adapter-failed";}
                }
            })!=null;
            if(!supportHook||!sendHook)return;
            configHook=XposedBridge.hookMethod(configure,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    ArrayDeque<Object> stack=SCOPE.get();if(stack==null){stack=new ArrayDeque<>();SCOPE.set(stack);}stack.push(p.thisObject);configs++;MANAGERS.put(p.thisObject,true);
                    reason=eligible(p.thisObject,true);
                }
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.getThrowable()==null)LAST.put(p.thisObject,eligible(p.thisObject,true));
                    ArrayDeque<Object> stack=SCOPE.get();if(stack!=null&&!stack.isEmpty())stack.pop();if(stack==null||stack.isEmpty())SCOPE.remove();
                }
            })!=null;
            if(configHook)XposedBridge.hookAllMethods(manager,"handleBacklightChange",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.getThrowable()!=null||current()!=null)return;
                    try{Object lock=field(p.thisObject,"mStateLock");synchronized(lock){
                        String decision=eligible(p.thisObject,true);
                        if(decision.equals(LAST.get(p.thisObject)))return;
                        // Only eligibility changes, never every frame or touch event.
                        XposedHelpers.callMethod(p.thisObject,"handleTouchIdleConfig",false);rechecks++;
                    }}catch(Throwable ignored){reason="backlight-recheck-unavailable";}
                }
            });
        }catch(Throwable ignored){reason="adapter-unavailable";}
    }
    // Called only on a private copy of the already-approved outgoing native bean.
    static boolean profile(Object b)throws Exception{
        if(!Boolean.TRUE.equals(field(b,"mHwEnable"))||!Boolean.TRUE.equals(field(b,"mEnable")))return false;
        // Exact default shape only; preserve application-specific timings/rates.
        if(!Long.valueOf(2500).equals(field(b,"mTimeOut"))||!Integer.valueOf(800).equals(field(b,"mContentGapMs"))
            ||!Integer.valueOf(60).equals(field(b,"mLowestRate"))||!Integer.valueOf(-1).equals(field(b,"mSpecialFps")))return false;
        write(b,"mTimeOut",600L);write(b,"mContentGapMs",200);
        // Keep the native software fallback. On this AD296 firmware the144Hz
        // timing lacks all ADFR command families; an HW-enable flag does not
        // prove that timing can enter panel idle. Disabling SW can pin it at144.
        return true;
    }
    static String eligible(Object manager,boolean lowOnly){
        if(!Config.refresh("low_light")||AdaptivePolicy.holdFixedRate())return "disabled-by-user";
        try {
            Object nit=field(manager,"mNits");
            if(!(nit instanceof Float)||!Float.isFinite((Float)nit)||(Float)nit<0f||(lowOnly&&(Float)nit>=30f))return "not-low-or-unknown";
            if(!Integer.valueOf(1).equals(field(manager,"mCurPolicy")))return "non-system-policy";
            if(!Boolean.TRUE.equals(field(manager,"mTouchIdleGlobalSwitch"))||!Boolean.TRUE.equals(field(manager,"mTouchIdleHwSwitch"))
                ||!Boolean.TRUE.equals(field(manager,"mAmbientEnough"))||!Boolean.TRUE.equals(field(manager,"mTouchIdleHwBacklightRangeIn"))
                ||!Boolean.FALSE.equals(field(manager,"mTouchIdleKeepHight"))||!Boolean.FALSE.equals(field(manager,"mLockRefreshRate"))
                ||!Boolean.FALSE.equals(field(manager,"mIsDefectPanel")))return "native-hw-guard";
            Object bean=field(manager,"mTouchIdleBean");
            if(!Boolean.TRUE.equals(field(bean,"mHwEnable"))||!Integer.valueOf(30).equals(field(bean,"mNitLimit"))
                ||!Integer.valueOf(30).equals(field(bean,"mPwmNitLimit"))||!Integer.valueOf(-1).equals(field(bean,"mBrightnessLimit")))return "unknown-brightness-profile";
            if(field(manager,"mAppRequest")!=null)return "app-request";
            Object special=field(bean,"mSpecialHwNitLimitMap"),pkg=field(manager,"mCurPkgName");
            if(!(special instanceof Map)||!(pkg instanceof String)||((String)pkg).isEmpty()||((Map<?,?>)special).containsKey(pkg))return "special-or-unknown-app";
            Object killer=field(manager,"mKillerPattern");
            if(!Boolean.FALSE.equals(field(killer,"mEnable")))return "active-or-unknown-killer";
            Object mode=field(manager,"mCurrentModeId"),configs=field(manager,"mRefreshRateConfigs");
            Object rate=XposedHelpers.callMethod(configs,"getRefreshRate",mode);
            if(!(rate instanceof Number))return "unknown-mode";
            float hz=((Number)rate).floatValue();

            for(float known:new float[]{60,90,120,165})if(Math.abs(hz-known)<0.1f)return "eligible";
            return "unsupported-mode";
        }catch(Throwable ignored){return "unknown-context";}
    }
    private static Object current(){ArrayDeque<Object>s=SCOPE.get();return s==null||s.isEmpty()?null:s.peek();}
    private static Object field(Object o,String n)throws Exception{if(o==null)throw new NoSuchFieldException(n);Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    private static void write(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static void refresh(){
        Object[] all;synchronized(MANAGERS){all=MANAGERS.keySet().toArray();}
        for(Object manager:all)try{Object lock=field(manager,"mStateLock");synchronized(lock){XposedHelpers.callMethod(manager,"handleTouchIdleConfig",false);}}catch(Throwable ignored){}
    }
    static String status(){return "hooks:"+configHook+"/"+supportHook+"/"+sendHook+",configs:"+configs+",brightnessOverrides:"+brightnessOverrides
        +",rechecks:"+rechecks+",sends:"+sends+",adapted:"+adapted+",reason:"+reason+",outgoing:"+outgoing;}
}
