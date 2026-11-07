package com.refresh165;

import android.content.Context;
import com.refresh165.xp.IXposedHookLoadPackage;
import com.refresh165.xp.XC_MethodHook;
import com.refresh165.xp.XposedBridge;
import com.refresh165.xp.XposedHelpers;
import com.refresh165.xp.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

// No module file writes. Detailed status is available through dumpsys.
/** OP15 r6: native165 profile, keep verified low-light LTPO adapters,
 * and delegateIME and idle-playback ceilings to AdaptivePolicy. */
public final class Hook implements IXposedHookLoadPackage {
    private static final String SYSTEM_PACKAGE = "android";
    private static volatile int highRateId=-1;
    private static volatile Class<?> rateUtils;
    private static volatile java.util.Map<Integer,Integer> rateIds=java.util.Collections.emptyMap();
    static int idleAwareRateId(){return Idle144Policy.active()?3:highRateId();}
    static int highRateId(){
        return rateId(Config.effectiveRate());
    }
    static int rateId(int rate){Integer id=rateIds.get(rate);return id==null?highRateId:id;
    }
    private static int appliedRate=-1;
    private static volatile Object lastDc;
    private static volatile int lastDcRequested=-1;
    static void configurationChanged(Context context){
        int rate=Config.rate();
        if(Config.on("refresh")&&context!=null&&rate!=appliedRate)try{
            int id=rateId(Config.rate());if(id>0){
                android.provider.Settings.Secure.putInt(context.getContentResolver(),"oplus_customize_screen_refresh_rate",id);
                android.provider.Settings.Secure.putInt(context.getContentResolver(),"choice_mode_rate",rate);
                appliedRate=rate;
            }
        }catch(Throwable ignored){}
        Object dc=lastDc;int nativeLimit=lastDcRequested;
        if(dc!=null&&nativeLimit>=0)try{XposedHelpers.callMethod(dc,"setLimitMaxRefreshRateId",nativeLimit);}catch(Throwable ignored){}
        refreshVideoPolicy();
    }

    private static volatile Object videoPolicy;
    private static volatile long videoCandidates;
    static void refreshVideoPolicy(){Object p=videoPolicy;if(p!=null)try{XposedHelpers.callMethod(p,"requestUpadate","refresh165-video-idle");}catch(Throwable ignored){}}
    static String videoCandidateStatus(){return "candidateCaps:"+videoCandidates;}

    private static final java.util.concurrent.atomic.AtomicBoolean INSTALLED=new java.util.concurrent.atomic.AtomicBoolean();

    private static volatile int selectedSettingMode = -1;
    private static final Set<String> STOCK_LOW_RATE_PACKAGES = new HashSet<>(Arrays.asList(
            // C.61 adfr_to_hw list
            "tv.danmaku.bili",
            "com.hunantv.imgo.activity",
            "com.ss.android.article.video",
            "com.tencent.qqlive",
            "com.qiyi.video",
            "com.sina.weibo",
            // C.61 short-video/content-idle list
            "com.ss.android.ugc.aweme",
            "com.ss.android.ugc.aweme.lite",
            "com.ss.android.ugc.live",
            "com.smile.gifmaker",
            "com.kuaishou.nebula",
            "com.tencent.wemeet.app",
            "com.sankuai.meituan"));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        android.util.Log.i("Refresh165", "load pkg=" + lp.packageName);
        if ("com.android.settings".equals(lp.packageName)) {
            Config.bootstrap(lp.classLoader);
            SettingsIntegration.install(lp.classLoader);SupportedAppsIntegration.install(lp.classLoader);
            return;
        }
        if (!SYSTEM_PACKAGE.equals(lp.packageName) && !"system".equals(lp.packageName)) {
            return;
        }
        // 不限制机型：非 OPPO 设备上各 Hook 找不到类会自行跳过，不会影响系统。

        if(!INSTALLED.compareAndSet(false,true))return;
        rateMapCompatible = hasExpectedRateMap(lp.classLoader);
        if (rateMapCompatible) {
            hookVendorRateClamp(lp.classLoader);
            hookDefaultRateMapping(lp.classLoader);
            hookDefaultWindowRate(lp.classLoader);
            removeDcRateCap(lp.classLoader);
            AdaptivePolicy.install(lp.classLoader);
            AnimationPolicy.install(lp.classLoader);
            preserveFullBrightnessDc(lp.classLoader);
            NativeIdlePolicy.install(lp.classLoader);
            NativeReservePolicy.install(lp.classLoader);
        }
    }

    private static volatile int labelFixed;
    private static volatile boolean rateMapCompatible;

    private static boolean hasExpectedRateMap(ClassLoader cl) {
        try {
            Class<?> utils = XposedHelpers.findClass("com.android.server.wm.OplusRefreshRateUtils", cl);
            rateUtils=utils;
            java.util.Map<Integer,Integer> map=new java.util.HashMap<>();
            for(int id=1;id<=16;id++)try{Object n=XposedHelpers.callStaticMethod(utils,"getRefreshRateById",id);if(n instanceof Number){float hz=((Number)n).floatValue();for(int rate:new int[]{60,90,120,144,165})if(Math.abs(hz-rate)<0.1f&&!map.containsKey(rate))map.put(rate,id);}}catch(Throwable ignored){}
            rateIds=java.util.Collections.unmodifiableMap(map);
            int[] ids = {1, 2, 3};
            float[] rates = {90f, 60f, 120f};
            for (int i = 0; i < ids.length; i++) {
                Object value=XposedHelpers.callStaticMethod(utils,"getRefreshRateById",ids[i]);
                if(!(value instanceof Number)||Math.abs(((Number)value).floatValue()-rates[i])>=0.1f)return false;
            }
            highRateId=RateProfile.highRateId(id->{
                Object value=XposedHelpers.callStaticMethod(utils,"getRefreshRateById",id);
                return value instanceof Number?((Number)value).floatValue():Float.NaN;
            });
            if(highRateId<0)return false;
            return true;
        } catch (Throwable ignored) { return false; }
    }

    static String lowBrightnessStatus(){return NativeIdlePolicy.status();}

    private static boolean writeField(Object obj, String name, Object value) {
        if (obj == null) return false;
        for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(obj, value); return true; }
            catch (NoSuchFieldException ignored) { }
            catch (Throwable ignored) { return false; }
        }
        return false;
    }

    private static void preserveFullBrightnessDc(ClassLoader cl) {
        try {
            Class<?> manager = XposedHelpers.findClass(
                    "com.oplus.vrr.OPlusExternalRefreshRateManager", cl);
            Method reset = manager.getDeclaredMethod("handleDcModeStatus");
            if (reset.getReturnType() != Void.TYPE) {
                return;
            }
            XposedBridge.hookMethod(reset, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (!Config.refresh("low_light") || p.args == null || p.args.length != 0) {
                        return;
                    }
                    Object lock = field(p.thisObject, "mStateLock");
                    if (lock == null) {
                        return;
                    }
                    synchronized (lock) {
                        Object mode = field(p.thisObject, "mCurrentEPMode");
                        Object enabled = field(p.thisObject, "mIsGameDcFpsEnable");
                        if (Integer.valueOf(2).equals(mode) && Boolean.TRUE.equals(enabled)) {
                            p.setResult(null);
                        }
                    }
                }
            });
        } catch (Throwable ignored) {
            // Unknown builds/classes keep their original behavior; no module logs.
        }
    }

    private static void removeDcRateCap(ClassLoader cl) {
        try {
            Class<?> dc = XposedHelpers.findClass(
                    "com.android.server.wm.OplusRefreshRateDCController", cl);
            XposedBridge.hookAllMethods(dc, "setLimitMaxRefreshRateId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if(p.args!=null&&p.args.length==1&&p.args[0] instanceof Integer){lastDc=p.thisObject;lastDcRequested=(Integer)p.args[0];}
                    if (Config.refresh("low_light") && p.args != null && p.args.length == 1 && p.args[0] instanceof Integer) {
                        // Zero is the vendor's existing no-cap sentinel.
                        p.args[0] = Integer.valueOf(0);
                    }
                }
            });
            XposedBridge.hookAllMethods(dc, "getDCLimitRefreshRateId", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (Config.refresh("low_light") && p.getThrowable() == null && p.args != null && p.args.length == 1
                            && p.args[0] instanceof Integer) {
                        p.setResult(p.args[0]);
                    }
                }
            });
        } catch (Throwable ignored) {
            // No logging; leave any unavailable vendor entry unmodified.
        }
    }

    private static boolean unlockSetting(int mode) {
        return Config.on("refresh")&&(mode==0||mode==highRateId()||mode==7||mode==4||mode==3||mode==2||mode==1); // legacy extreme setting now resolves to165
    }

    private static void hookDefaultWindowRate(ClassLoader cl) {
        try {
            Class<?> policy = XposedHelpers.findClass(
                    "com.android.server.wm.OplusRefreshRatePolicyImpl", cl);
            XposedBridge.hookAllMethods(policy, "getWindowPreferredModeId",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.args == null || p.args.length < 1
                                    || !(p.args[0] instanceof Number)) {
                                return;
                            }
                            if(!Config.on("refresh"))return;
                            int picked = ((Number) p.args[0]).intValue();
                            if(Idle144Policy.owner(candidatePackage(field(p.thisObject,"mPickRefreshRateData")))&&(picked==4||picked==3||picked==7)){p.args[0]=3;return;}
                            videoPolicy=p.thisObject;
                            if(VideoIdlePolicy.watching()){
                                Object candidate=field(p.thisObject,"mPickRefreshRateData");
                                if(VideoIdlePolicy.watchingPackage(candidatePackage(candidate))){
                                    if(picked==1||picked==3||picked==4||picked==7){p.args[0]=Integer.valueOf(2);videoCandidates++;}return;
                                }
                            }

                            // Skip reflection entirely for non-120 candidates or other settings.
                            Object observer = field(p.thisObject, "mSettingsObserver");
                            Object modeValue = field(observer, "mCurrentPickMode");
                            int mode = modeValue instanceof Number
                                    ? ((Number) modeValue).intValue() : selectedSettingMode;
                            Object selectedPicker=field(p.thisObject,"mPickRefreshRateData");
                            String selectedPkg=candidatePackage(selectedPicker);
                            AppProfiles.Rule appRule=Config.profile(selectedPkg);
                            if(appRule!=null){if(!Config.foregroundOwner(selectedPkg))return;if(appRule.explicit()&&(picked==1||picked==2||picked==3||picked==4||picked==7))p.args[0]=rateId(Config.profileRate(selectedPkg));return;}
                            if(Config.app(selectedPkg)&&candidateWindowRequestRate(selectedPicker)==0){p.args[0]=highRateId();return;}
                            if (picked != 3 || !unlockSetting(mode)) {
                                return;
                            }
                            Object picker = field(p.thisObject, "mPickRefreshRateData");
                            String pkg = candidatePackage(picker);
                            if (pkg.isEmpty() || STOCK_LOW_RATE_PACKAGES.contains(pkg)
                                    || (candidateWindowRequestRate(picker) != 0
                                        && !("com.android.launcher".equals(pkg) && candidateWindowRequestRate(picker) == 3))
                                    || explicitUserOverrideRate(p.thisObject, pkg, mode) != 0) {
                                return;
                            }
                            p.args[0] = Integer.valueOf(highRateId());
                        }
                    });
        } catch (Throwable ignored) {
            // Preserve system behavior if a vendor hook cannot be installed.
        }
    }

    private static String candidatePackage(Object picker) {
        if (picker == null) {
            return "";
        }
        try {
            Object value = XposedHelpers.callMethod(picker, "candidateWinPkgName");
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable t) {
            return "";
        }
    }

    private static int candidateWindowRequestRate(Object picker) {
        if (picker == null) {
            return -1;
        }
        try {
            Object value = XposedHelpers.callMethod(picker, "candidateWinReqRateId");
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int explicitUserOverrideRate(Object policy, String pkg, int mode) {
        if (policy == null || pkg == null || pkg.isEmpty()) {
            return -1;
        }
        try {
            Object configs = field(policy, "mConfigs");
            if (configs == null) {
                return -1;
            }
            Object data = XposedHelpers.callMethod(
                    configs, "getPreferredRefreshRateData", pkg, null);
            if (data == null) {
                return 0;
            }
            Object value = XposedHelpers.callMethod(
                    data, "getOverridePreferredRefreshRateId", Integer.valueOf(mode == 0 ? 0 : 3));
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Throwable t) {
            // Fail safe: preserve the stock candidate when its source is unknown.
            return 3;
        }
    }

    private static int settingMode(Object policy) {
        Object mode = field(field(policy, "mSettingsObserver"), "mCurrentPickMode");
        return mode instanceof Integer ? (Integer)mode : selectedSettingMode;
    }

    private static void hookVendorRateClamp(ClassLoader cl) {
        try {
            Class<?> policy = XposedHelpers.findClass(
                    "com.android.server.wm.OplusRefreshRatePolicyImpl", cl);
            XposedBridge.hookAllMethods(policy, "forceMaxRefreshRateSettings",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            if (p.args == null || p.args.length != 1
                                    || !(p.args[0] instanceof Number)) {
                                return;
                            }
                            int requested = ((Number) p.args[0]).intValue();
                            selectedSettingMode = requested;
                            if (!Config.on("refresh") || !unlockSetting(requested)) {
                                return;
                            }
                            p.setResult(Integer.valueOf(highRateId()));
                        }
                    });
        } catch (Throwable ignored) {
            // Preserve system behavior if a vendor hook cannot be installed.
        }
    }

    private static void hookDefaultRateMapping(ClassLoader cl) {
        try {
            Class<?> configs = XposedHelpers.findClass(
                    "com.android.server.wm.OplusRefreshRateConfigs", cl);
            XposedBridge.hookAllMethods(configs, "getDefaultRateId", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length != 1
                            || !(p.args[0] instanceof Number)) {
                        return;
                    }
                    int pickMode = ((Number) p.args[0]).intValue();
                    if (unlockSetting(pickMode)) {
                        p.setResult(Integer.valueOf(highRateId()));
                    }
                }
            });
        } catch (Throwable ignored) {
            // Preserve system behavior if a vendor hook cannot be installed.
        }
    }

    private static Object field(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Throwable ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

}
