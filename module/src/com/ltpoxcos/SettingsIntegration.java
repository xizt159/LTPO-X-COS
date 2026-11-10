package com.ltpoxcos;
import android.content.Context;
import android.os.Bundle;
import com.ltpoxcos.xp.*;
import java.lang.reflect.*;
import java.util.*;
/** Expand the existing OEM RefreshRateMenuPreference, preserving its COUI popup. */
final class SettingsIntegration {
    private static final Map<Object,Object[]> ORIGINAL=Collections.synchronizedMap(new WeakHashMap<>());
    static void install(ClassLoader cl){try{
        Class<?> fragment=Class.forName("com.oplus.settings.feature.display.ScreenRefreshRateFragment",false,cl);
        XC_MethodHook callback=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
            if(!fragment.isInstance(p.thisObject))return;
            try{
                Object screen=XposedHelpers.callMethod(p.thisObject,"getPreferenceScreen");if(screen==null)return;
                Object obsolete=XposedHelpers.callMethod(screen,"findPreference","refresh_studio_rates");if(obsolete!=null)XposedHelpers.callMethod(screen,"removePreference",obsolete);
                Object pref=XposedHelpers.callMethod(screen,"findPreference","refresh_rate_setting");
                Object category=XposedHelpers.callMethod(screen,"findPreference","screen_refresh_rate_settings");
                if(pref==null||!pref.getClass().getName().equals("com.oplus.settings.feature.display.RefreshRateMenuPreference"))return;
                Object[] saved=ORIGINAL.get(pref);
                if(saved==null){saved=new Object[]{get(pref,"getEntries"),get(pref,"getEntryValues"),get(pref,"getOnPreferenceChangeListener"),get(pref,"getValue"),get(pref,"getSummary"),get(pref,"getAssignment"),get(pref,"isPersistent"),get(pref,"isEnabled"),category==null?null:get(category,"isVisible")};ORIGINAL.put(pref,saved);}
                if(!Config.refresh("menu")){
                    MenuIndicator.restore(pref);set(pref,"setEntries",saved[0]);set(pref,"setEntryValues",saved[1]);set(pref,"setOnPreferenceChangeListener",saved[2]);set(pref,"setValue",saved[3]);set(pref,"setSummary",saved[4]);set(pref,"setAssignment",saved[5]);set(pref,"setPersistent",saved[6]);set(pref,"setEnabled",saved[7]);if(category!=null&&saved[8]!=null)set(category,"setVisible",saved[8]);set(pref,"setMenuShowEnabled",(saved[0] instanceof CharSequence[])&&((CharSequence[])saved[0]).length>0);ORIGINAL.remove(pref);return;
                }
                Context c=(Context)XposedHelpers.callMethod(p.thisObject,"getContext");
                set(pref,"setEntries",new CharSequence[]{"60 Hz","90 Hz","120 Hz","144 Hz","165 Hz"});set(pref,"setEntryValues",new CharSequence[]{"60","90","120","144","165"});
                MenuIndicator.apply(pref,c);set(pref,"setPersistent",false);set(pref,"setEnabled",true);set(pref,"setMenuShowEnabled",true);if(category!=null)set(category,"setVisible",true);
                set(pref,"setValue",String.valueOf(Config.rate()));set(pref,"setAssignment",Config.rate()+" Hz");
                boolean ar=c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");set(pref,"setSummary",(ar?"التردد المختار: ":"Selected refresh rate: ")+Config.rate()+" Hz");
                Class<?> listener=Class.forName("androidx.preference.Preference$OnPreferenceChangeListener",false,cl);
                Object proxy=Proxy.newProxyInstance(cl,new Class<?>[]{listener},(o,m,args)->{
                    if(m.getName().equals("onPreferenceChange")){int rate=Config.validRate(Integer.parseInt(String.valueOf(args[1])));Bundle b=new Bundle();b.putInt("rate",rate);c.getContentResolver().call(Config.URI,"write",null,b);Config.previewRate(rate);set(pref,"setAssignment",rate+" Hz");set(pref,"setSummary",(ar?"التردد المختار: ":"Selected refresh rate: ")+rate+" Hz");return true;}
                    if(m.getName().equals("hashCode"))return System.identityHashCode(o);if(m.getName().equals("equals"))return o==args[0];return "Native refresh selector";
                });set(pref,"setOnPreferenceChangeListener",proxy);
            }catch(Throwable ignored){}
        }};
        XposedBridge.hookAllMethods(fragment,"onResume",callback);
        XposedBridge.hookAllMethods(fragment,"refreshRateSettingSummary",callback);
    }catch(Throwable ignored){}}
    private static Object get(Object o,String m){try{return XposedHelpers.callMethod(o,m);}catch(Throwable ignored){return null;}}
    private static void set(Object o,String m,Object v){try{XposedHelpers.callMethod(o,m,v);}catch(Throwable ignored){}}
}
