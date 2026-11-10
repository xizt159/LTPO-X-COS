package com.ltpoxcos;

import com.ltpoxcos.xp.XposedBridge;
import com.ltpoxcos.xp.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** libxposed API 102 入口：把包加载事件转给现有 Hook 逻辑。 */
public final class LibEntry extends XposedModule {

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(4, "LTPO-X-COS", "module loaded, systemServer=" + param.isSystemServer());
    }

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        try {
            ClassLoader cl = param.getClassLoader();
            XposedBridge.init(this, cl);
            XC_LoadPackage.LoadPackageParam lp =
                    new XC_LoadPackage.LoadPackageParam("android", cl);
            new Hook().handleLoadPackage(lp);
            log(4, "LTPO-X-COS", "system_server hooks installed");
        } catch (Throwable t) {
            log(6, "LTPO-X-COS", "onSystemServerStarting failed: " + t);
        }
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        try {
            ClassLoader cl = param.getDefaultClassLoader();
            XposedBridge.init(this, cl);
            XC_LoadPackage.LoadPackageParam lp =
                    new XC_LoadPackage.LoadPackageParam(param.getPackageName(), cl);
            Config.bootstrap(cl);
            if("com.android.systemui".equals(param.getPackageName())||"com.oplus.aod".equals(param.getPackageName()))AodPolicy.install(cl,param.getPackageName());
            new Hook().handleLoadPackage(lp);
        } catch (Throwable t) {
            log(6, "LTPO-X-COS", "onPackageLoaded failed: " + t);
        }
    }
}
