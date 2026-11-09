package com.refresh165;

import android.content.*;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.*;
import java.util.*;

/** Immutable, validated snapshots. No provider calls on render/WM hot paths. */
final class Config {
    static final Uri URI=Uri.parse("content://com.refresh165.config");
    static final String[] KEYS={"refresh","low_light","tables","fast_idle","animation","ime","video","menu","disable_ltpo","bulk_apps","gate144","aod","panoramic","all_day","block_tap","dark_visible","initial_dark","initial_bright","running_brightness","hide_wallpaper","block_lift","fixed_brightness","battery_only","app_policies","battery_profile"};
    static final boolean[] DEFAULTS={true,true,true,true,true,true,true,true,false,false,false,false,true,true,true,true,false,false,false,false,false,false,false,true,false};
    private static volatile Bundle values=new Bundle();
    private static volatile Map<String,AppProfiles.Rule> profiles=Collections.emptyMap();
    private static Context context;
    static Context context(){return context;}
    private static Handler handler;
    private static volatile long successfulLoads,failedLoads;private static volatile String loadFault="not-loaded";
    private static boolean retryPending;
    static String debugStatus(){AppProfiles.Rule r=currentProfile();return "loads:"+successfulLoads+",failures:"+failedLoads+",fault:"+loadFault+",profiles:"+profiles.size()+",enabled:"+on("app_policies")+",owner:"+foreground+",rule:"+(r==null?"none":r.wire());}
    private static final List<Runnable> listeners=new ArrayList<>();
    static boolean defaultOn(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return DEFAULTS[i];return false;}
    static boolean on(String key){return values.getBoolean(key,defaultOn(key));}
    static boolean refresh(String key){return on("refresh")&&on(key);}
    static boolean aod(String key){return on("aod")&&on(key);}
    private static volatile String foreground="";
    static void scene(String pkg,boolean full){foreground=full&&pkg!=null?pkg:"";}
    static AppProfiles.Rule profile(String pkg){return refresh("app_policies")?profiles.get(pkg):null;}
    static boolean foregroundOwner(String pkg){return !foreground.isEmpty()&&foreground.equals(pkg);}
    static AppProfiles.Rule currentProfile(){return profile(foreground);}
    static int profileRate(String pkg){AppProfiles.Rule r=profile(pkg);return BatteryDisplayPolicy.limit(r!=null&&r.rate>0?r.rate:rate());}
    static int effectiveRate(){return profileRate(foreground);}
    static boolean fixed(){AppProfiles.Rule r=currentProfile();return r==null?refresh("disable_ltpo"):r.fixed();}
    static boolean explicitProfile(){AppProfiles.Rule r=currentProfile();return r!=null&&r.explicit();}
    static void previewProfiles(Bundle next){profiles=Collections.unmodifiableMap(AppProfiles.parse(next.getString("app_profiles","")));values=new Bundle(next);}
    static int rate(){return validRate(values.getInt("rate",165));}
    static void previewRate(int rate){Bundle next=new Bundle(values);next.putInt("rate",validRate(rate));values=next;}
    static int validRate(int n){return n==60||n==90||n==120||n==144||n==165?n:165;}
    static int number(String key,int def,int min,int max){return Math.max(min,Math.min(max,values.getInt(key,def)));}
    static boolean app(String pkg){return refresh("app_policies")&&on("bulk_apps")&&Arrays.asList(values.getString("apps","").split(";")).contains(pkg);}
    static synchronized void listen(Runnable r){listeners.add(r);}
    static synchronized void init(Context c){
        if(context!=null||c==null)return;
        context=c.getApplicationContext()==null?c:c.getApplicationContext();
        HandlerThread t=new HandlerThread("UnifiedDisplayConfig");t.start();handler=new Handler(t.getLooper());
        context.getContentResolver().registerContentObserver(URI,true,new ContentObserver(handler){@Override public void onChange(boolean self){load();}});
        handler.post(Config::load);
    }
    private static void load(){
        try{
            Bundle next=context.getContentResolver().call(URI,"read",null,null);if(next==null)throw new IllegalStateException("Configuration unavailable");
            previewProfiles(next);successfulLoads++;loadFault="none";
            List<Runnable> copy; synchronized(Config.class){copy=new ArrayList<>(listeners);}
            for(Runnable r:copy)try{r.run();}catch(Throwable ignored){}
        }catch(Throwable error){failedLoads++;loadFault=error.getClass().getSimpleName();
            // A provider unavailable during early boot must not leave system_server on defaults forever.
            if(handler!=null&&!retryPending){retryPending=true;handler.postDelayed(()->{retryPending=false;load();},Math.min(30000L,1000L<<Math.min(5,failedLoads)));}
        }
    }
    static void bootstrap(ClassLoader cl){
        try{
            com.refresh165.xp.XposedBridge.hookMethod(ApplicationAttach.method(),new com.refresh165.xp.XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){if(p.args[0] instanceof Context)init((Context)p.args[0]);}
            });
            Object app=com.refresh165.xp.XposedHelpers.callStaticMethod(Class.forName("android.app.ActivityThread",false,cl),"currentApplication");
            if(app instanceof Context)init((Context)app);
        }catch(Throwable ignored){}
    }
    private static final class ApplicationAttach {static java.lang.reflect.Method method()throws Exception{return android.app.Application.class.getDeclaredMethod("attach",Context.class);}}
}
