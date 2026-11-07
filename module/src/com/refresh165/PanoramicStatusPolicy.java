package com.refresh165;
import com.refresh165.xp.*;
import java.lang.reflect.*;
/** LuckyTool's installed hide_panoramic_aod_status_bar path: prevent the OEM
 * KeyguardStatusBarViewExImpl from keeping its keyguard bar visible in doze. */
final class PanoramicStatusPolicy {
    static boolean suppress(boolean feature,boolean dozing,boolean panoramic){return feature&&dozing&&panoramic;}
    static void install(ClassLoader cl){try{
        Class<?> type=Class.forName("com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl",false,cl);
        Method method=type.getDeclaredMethod("hookDozingState",boolean.class);if(method.getReturnType()!=boolean.class)return;
        XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            if(Boolean.TRUE.equals(p.args[0])&&Config.aod("battery_only")){p.setResult(false);return;}
            if(suppress(Config.aod("panoramic"),Boolean.TRUE.equals(p.args[0]),AodAccess.panoramic(AodAccess.context(p.thisObject))))p.setResult(false);
        }});
    }catch(Throwable ignored){}}
}
