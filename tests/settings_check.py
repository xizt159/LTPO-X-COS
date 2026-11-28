"""Exercise the real OEM-menu adapter and restore behavior without creating a preference."""
from pathlib import Path
import ast,subprocess,sys
r=Path(__file__).resolve().parents[1];out=Path(sys.argv[1]).resolve();src=out/'src';classes=out/'classes'
t=ast.parse((r/'tests/provider_check.py').read_text(encoding='utf-8'));files=ast.literal_eval(next(n.value for n in t.body if isinstance(n,ast.Assign) and any(getattr(a,'id','')=='files' for a in n.targets)))
files.pop('com/ltpoxcos/xp/XposedHelpers.java')
files['android/content/ContentResolver.java']='''package android.content;import java.util.*;public class ContentResolver {public int notifications;public android.os.Bundle response;public android.os.Bundle modeState=new android.os.Bundle();public Map<String,String> values=new HashMap<>();public ContentResolver(){modeState.putBoolean("active",false);}public android.os.Bundle call(android.net.Uri u,String m,String arg,android.os.Bundle b){if(!"refresh_mode_state".equals(m))return response;if("read".equals(arg))return new android.os.Bundle(modeState);if("save".equals(arg)){if(!modeState.getBoolean("active")){modeState=new android.os.Bundle(b);modeState.putBoolean("active",true);}return new android.os.Bundle(modeState);}if("clear".equals(arg)){modeState=new android.os.Bundle();modeState.putBoolean("active",false);return android.os.Bundle.EMPTY;}throw new IllegalArgumentException(arg);}public void registerContentObserver(android.net.Uri u,boolean n,android.database.ContentObserver o){}public void notifyChange(android.net.Uri u,Object o){notifications++;}}'''
files['android/provider/Settings.java']='''package android.provider;public final class Settings {public static final class System {public static final String PEAK_REFRESH_RATE="peak_refresh_rate";public static String getStringForUser(android.content.ContentResolver r,String k,int u){return r.values.get(k);}public static boolean putStringForUser(android.content.ContentResolver r,String k,String v,int u){if(v==null)r.values.remove(k);else r.values.put(k,v);return true;}}}'''
files['android/os/UserHandle.java']='package android.os;public class UserHandle {public static final int USER_CURRENT=-2;}'
files['android/util/Log.java']='package android.util;public class Log {public static int e(String t,String m,Throwable e){return 0;}}'
files['android/content/Context.java']=files['android/content/Context.java'].replace('public class Context {','public class Context {public Resources getResources(){return new Resources();}public static class Resources {public Configuration getConfiguration(){return new Configuration();}}public static class Configuration {public Locales getLocales(){return new Locales();}}public static class Locales {public java.util.Locale get(int n){return java.util.Locale.ENGLISH;}}')
files['com/ltpoxcos/xp/XC_MethodHook.java']='package com.ltpoxcos.xp;public class XC_MethodHook {protected void afterHookedMethod(MethodHookParam p){}public static class MethodHookParam {public Object thisObject;public Object[] args;}}'
files['com/ltpoxcos/xp/XposedBridge.java']='''package com.ltpoxcos.xp;import java.lang.reflect.*;import java.util.*;public class XposedBridge {static Map<Method,XC_MethodHook> hooks=new HashMap<>();public static ClassLoader getClassLoader(){return XposedBridge.class.getClassLoader();}public static void hookMethod(Method m,XC_MethodHook c){hooks.put(m,c);}public static void hookAllMethods(Class<?> c,String n,XC_MethodHook cb){for(Method m:c.getDeclaredMethods())if(m.getName().equals(n))hookMethod(m,cb);}public static void run(Object o,String name)throws Exception{Method m=o.getClass().getDeclaredMethod(name);m.invoke(o);XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=o;p.args=new Object[0];hooks.get(m).afterHookedMethod(p);}}'''
files['androidx/preference/Preference.java']='''package androidx.preference;public class Preference {public interface OnPreferenceChangeListener {boolean onPreferenceChange(Preference p,Object v);}public CharSequence[] entries={"60Hz","120Hz"},values={"2","3"};public String value="3";public CharSequence summary="Native summary",assignment="120 Hz";public boolean persistent=true,enabled=true,visible=false,menu=false;public OnPreferenceChangeListener listener;public CharSequence[] getEntries(){return entries;}public CharSequence[] getEntryValues(){return values;}public void setEntries(CharSequence[] a){entries=a;}public void setEntryValues(CharSequence[] a){values=a;}public OnPreferenceChangeListener getOnPreferenceChangeListener(){return listener;}public void setOnPreferenceChangeListener(OnPreferenceChangeListener l){listener=l;}public String getValue(){return value;}public void setValue(String s){value=s;}public CharSequence getSummary(){return summary;}public void setSummary(CharSequence s){summary=s;}public CharSequence getAssignment(){return assignment;}public void setAssignment(CharSequence s){assignment=s;}public boolean isPersistent(){return persistent;}public void setPersistent(boolean b){persistent=b;}public boolean isEnabled(){return enabled;}public void setEnabled(boolean b){enabled=b;}public boolean isVisible(){return visible;}public void setVisible(boolean b){visible=b;}public void setMenuShowEnabled(boolean b){menu=b;}}'''
files['com/oplus/settings/feature/display/RefreshRateMenuPreference.java']='package com.oplus.settings.feature.display;public class RefreshRateMenuPreference extends androidx.preference.Preference {}'
files['com/oplus/settings/feature/display/ScreenRefreshRateFragment.java']='''package com.oplus.settings.feature.display;public class ScreenRefreshRateFragment {public Screen screen=new Screen();public void onResume(){}public void refreshRateSettingSummary(int a,int b){}public Screen getPreferenceScreen(){return screen;}public android.content.Context getContext(){return new android.content.Context();}public static class Screen {public RefreshRateMenuPreference pref=new RefreshRateMenuPreference();public androidx.preference.Preference category=new androidx.preference.Preference();public Object findPreference(String key){return key.equals("refresh_rate_setting")?pref:key.equals("screen_refresh_rate_settings")?category:null;}public void removePreference(Object p){throw new AssertionError("Unexpected row removal");}}}'''
files['com/ltpoxcos/SettingsTest.java']='''package com.ltpoxcos;import android.os.Bundle;public class SettingsTest {static int checks;static void yes(boolean b){checks++;if(!b)throw new AssertionError("settings "+checks);}public static void main(String[]a)throws Exception{Bundle b=new Bundle();b.putInt("rate",144);java.lang.reflect.Field f=Config.class.getDeclaredField("values");f.setAccessible(true);f.set(null,b);SettingsIntegration.install(SettingsTest.class.getClassLoader());com.oplus.settings.feature.display.ScreenRefreshRateFragment fragment=new com.oplus.settings.feature.display.ScreenRefreshRateFragment();Object original=fragment.screen.pref;com.ltpoxcos.xp.XposedBridge.run(fragment,"onResume");yes(fragment.screen.pref==original);yes(fragment.screen.pref.entries.length==5);yes(fragment.screen.pref.entries[3].equals("144 Hz"));yes(fragment.screen.pref.value.equals("144"));yes(fragment.screen.pref.menu);yes(fragment.screen.category.visible);yes(!fragment.screen.pref.persistent);yes(fragment.screen.pref.listener!=null);b.putBoolean("menu",false);com.ltpoxcos.xp.XposedBridge.run(fragment,"onResume");yes(fragment.screen.pref.entries.length==2);yes(fragment.screen.pref.value.equals("3"));yes(fragment.screen.pref.persistent);yes(!fragment.screen.category.visible);yes(fragment.screen.pref.listener==null);System.out.println("PASS "+checks+" original OEM menu / restore assertions");}}'''
files['com/ltpoxcos/ConfigInitializationTest.java']='''package com.ltpoxcos;import android.content.Context;import android.os.Bundle;public class ConfigInitializationTest {static void yes(boolean value){if(!value)throw new AssertionError("system-server config initialization");}public static void main(String[]a){Context context=new Context();Bundle values=new Bundle();values.putInt("rate",120);values.putBoolean("battery_saver_cap",true);context.resolver.response=values;Config.initForSystemServer(context);yes(Config.rate()==120);yes(Config.on("battery_saver_cap"));yes(Config.debugStatus().contains("loads:1"));System.out.println("PASS system-server initial config read");}}'''
files['com/ltpoxcos/RefreshModePolicyTest.java']='''package com.ltpoxcos;
import android.content.*;
public class RefreshModePolicyTest {
    static int checks;
    static void yes(boolean value) {
        checks++;
        if (!value) throw new AssertionError("refresh-mode policy " + checks);
    }
    public static void main(String[] args) {
        Context context = new Context();
        ContentResolver resolver = context.resolver;
        resolver.values.put("peak_refresh_rate", "120.0");
        resolver.values.put("user_refresh_rate", "120");
        RefreshModePolicy.sync(context, true);
        yes(resolver.values.get("peak_refresh_rate").equals("165.0"));
        yes(resolver.values.get("user_refresh_rate").equals("165"));
        yes(resolver.modeState.getBoolean("active"));

        resolver.values.put("peak_refresh_rate", "90.0");
        RefreshModePolicy.sync(context, true);
        yes(resolver.modeState.getString("peak", "").equals("120.0"));
        yes(resolver.values.get("peak_refresh_rate").equals("165.0"));
        RefreshModePolicy.sync(context, false);
        yes(resolver.values.get("peak_refresh_rate").equals("120.0"));
        yes(resolver.values.get("user_refresh_rate").equals("120"));
        yes(!resolver.modeState.getBoolean("active"));

        resolver.values.clear();
        RefreshModePolicy.sync(context, true);
        RefreshModePolicy.sync(context, false);
        yes(!resolver.values.containsKey("peak_refresh_rate"));
        yes(!resolver.values.containsKey("user_refresh_rate"));
        System.out.println("PASS " + checks + " refresh-mode preserve/restore assertions");
    }
}'''
files['com/ltpoxcos/MenuIndicator.java']='package com.ltpoxcos;final class MenuIndicator {static void apply(Object p,android.content.Context c){}static void restore(Object p){}}'
files['android/os/Handler.java']=files['android/os/Handler.java'].replace('public class Handler {','public class Handler {public boolean postDelayed(Runnable r,long delay){return true;}')
for name,body in files.items():f=src/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body,encoding='utf-8')
classes.mkdir(parents=True,exist_ok=True)
actual=[r/'module/src/com/ltpoxcos'/name for name in ['AppProfiles.java','Config.java','ConfigProvider.java','SettingsIntegration.java','BatteryDisplayPolicy.java','RefreshModePolicy.java','xp/XposedHelpers.java']]
subprocess.run(['javac','--release','11','-encoding','UTF-8','-d',str(classes),*[str(f) for f in src.rglob('*.java')],*[str(f) for f in actual]],check=True)
subprocess.run(['java','-cp',str(classes),'com.ltpoxcos.SettingsTest'],check=True)
subprocess.run(['java','-cp',str(classes),'com.ltpoxcos.ConfigInitializationTest'],check=True)
subprocess.run(['java','-cp',str(classes),'com.ltpoxcos.RefreshModePolicyTest'],check=True)
