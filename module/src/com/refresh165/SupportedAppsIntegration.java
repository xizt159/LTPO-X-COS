package com.refresh165;
import android.app.AlertDialog;
import android.content.Context;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.refresh165.xp.*;
import java.lang.reflect.*;
import java.util.*;
/** Verified ColorOS17 preference and search adapters. No native app-switch settings are written. */
final class SupportedAppsIntegration {
 static final String PREFIX="com.oplus.settings.feature.display.refreshhigh.";
 static final int SELECT=0x52450001,CLEAR=0x52450002,FOLLOW=0x52450003;
 private static final Map<Object,Boolean> PREFS=Collections.synchronizedMap(new WeakHashMap<>()),ADAPTERS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final Map<Menu,java.lang.ref.WeakReference<Context>> MENUS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 static void install(ClassLoader cl){
  try{Class<?> pref=Class.forName(PREFIX+"ScreenRefreshAppPreference",false,cl);for(Method m:pref.getDeclaredMethods())if(m.getName().equals("onBindViewHolder")&&m.getParameterCount()==1)XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!enabled()||p.getThrowable()!=null)return;try{Object holder=p.args[0];View row=(View)field(holder,"itemView");Context c=row.getContext();int id=c.getResources().getIdentifier("assignment","id","com.android.settings");Object assignment=XposedHelpers.callMethod(holder,"findViewById",id);bind(p.thisObject,row,field(p.thisObject,"mSwitch"),assignment);}catch(Throwable ignored){}}});}catch(Throwable ignored){}
  try{Class<?> adapter=Class.forName(PREFIX+"ScreenRefreshHighAppSearchAdapter",false,cl);for(Method m:adapter.getDeclaredMethods())if(m.getName().equals("onBindViewHolder")&&m.getParameterCount()==2&&m.getParameterTypes()[0].getName().endsWith("$ScreenRefreshRateSearchViewHolder"))XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!enabled()||p.getThrowable()!=null)return;try{ADAPTERS.put(p.thisObject,true);Object holder=p.args[0];Object pref=XposedHelpers.callMethod(p.thisObject,"getItem",p.args[1]);bind(pref,(View)XposedHelpers.callMethod(holder,"getMItem"),XposedHelpers.callMethod(holder,"getMSwitch"),XposedHelpers.callMethod(holder,"getMAssignment"));}catch(Throwable ignored){}}});}catch(Throwable ignored){}
  try{Class<?> fragment=Class.forName(PREFIX+"ScreenRefreshHighAppNewFragment",false,cl);
   XposedBridge.hookMethod(fragment.getDeclaredMethod("onCreateOptionsMenu",Menu.class,MenuInflater.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Context c=(Context)XposedHelpers.callMethod(p.thisObject,"getContext");Menu menu=(Menu)p.args[0];MENUS.put(menu,new java.lang.ref.WeakReference<>(c));updateMenu(menu,c);}catch(Throwable ignored){}}});
   XposedBridge.hookMethod(fragment.getDeclaredMethod("onOptionsItemSelected",MenuItem.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(!enabled())return;int id=((MenuItem)p.args[0]).getItemId();if(id!=SELECT&&id!=CLEAR&&id!=FOLLOW)return;p.setResult(true);try{Object fragment=p.thisObject;Context c=(Context)XposedHelpers.callMethod(fragment,"getContext");List<String> packages=packages(fragment);if(packages.isEmpty()){Toast.makeText(c,tr(c,"انتظر تحميل التطبيقات","Wait for the app list to load"),Toast.LENGTH_SHORT).show();return;}String ids=String.join(";",packages);if(id==SELECT)choose(c,ids,title(c,SELECT),null);else write(c,"app_profile",ids,id==FOLLOW?"follow":"system");}catch(Throwable ignored){}}});
   XposedBridge.hookMethod(fragment.getDeclaredMethod("resetOverrideAppRefreshRate"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(!enabled()||p.getThrowable()!=null)return;try{write((Context)XposedHelpers.callMethod(p.thisObject,"getContext"),"reset_profiles","","");}catch(Throwable ignored){}}});
  }catch(Throwable ignored){}
  Config.listen(()->MAIN.post(SupportedAppsIntegration::refresh));
 }
 private static boolean enabled(){return Config.refresh("menu");}
 static List<String> packages(Object fragment){TreeSet<String> ids=new TreeSet<>();Object value=XposedHelpers.callMethod(fragment,"getPreferencesList");if(value instanceof List)for(Object pref:(List<?>)value){String pkg=packageName(pref);if(AppProfiles.packageName(pkg))ids.add(pkg);}return new ArrayList<>(ids);}
 private static String packageName(Object pref){try{return String.valueOf(XposedHelpers.callMethod(pref,"getPackageName"));}catch(Throwable ignored){return "";}}
 private static void bind(Object pref,View row,Object toggle,Object value){
  if(pref==null||row==null||!(toggle instanceof View)||!(value instanceof TextView))return;String pkg=packageName(pref);if(!AppProfiles.packageName(pkg))return;
  PREFS.put(pref,true);Context c=row.getContext();View sw=(View)toggle;TextView label=(TextView)value;
  if(toggle instanceof CompoundButton)((CompoundButton)toggle).setOnCheckedChangeListener(null);sw.setVisibility(View.GONE);sw.setClickable(false);label.setVisibility(View.VISIBLE);label.setEnabled(true);label.setMaxLines(2);label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);label.setText(label(c,Config.profile(pkg)));label.setContentDescription(tr(c,"سياسة تردد التطبيق","App refresh policy")+": "+label.getText());
  View.OnClickListener click=v->choose(c,pkg,String.valueOf(XposedHelpers.callMethod(pref,"getTitle")),Config.profile(pkg));row.setOnClickListener(click);label.setOnClickListener(click);label.setClickable(true);
 }
 static String label(Context c,AppProfiles.Rule rule){if(rule==null||rule.mode.equals("system"))return tr(c,"افتراضي النظام","System default")+" ↕";if(rule.mode.equals("follow"))return tr(c,"اتباع الحالي","Follow current")+" ("+Config.rate()+" Hz) ↕";return (rule.fixed()?tr(c,"ثابت","Fixed"):tr(c,"متغير","Variable"))+" "+rule.rate+" Hz ↕";}
 private static void choose(Context c,String packages,String title,AppProfiles.Rule current){
  String[] ids={"f165","f144","f120","f90","f60","v165","v144","v120","v90","v60","follow","system"};String[] choices=new String[ids.length];int checked=-1;
  for(int i=0;i<ids.length;i++){choices[i]=label(c,AppProfiles.rule(ids[i])).replace(" ↕","");if(current!=null&&ids[i].equals(current.wire()))checked=i;}
  new AlertDialog.Builder(c).setTitle(title).setSingleChoiceItems(choices,checked,(d,n)->{if(write(c,"app_profile",packages,ids[n]))d.dismiss();}).setNegativeButton(tr(c,"إلغاء","Cancel"),null).show();
 }
 private static boolean write(Context c,String method,String packages,String policy){try{Bundle b=new Bundle();b.putString("packages",packages);b.putString("policy",policy);Bundle result=c.getContentResolver().call(Config.URI,method,null,b);if(result==null)throw new IllegalStateException("No configuration reply");Config.previewProfiles(result);refresh();return true;}catch(Throwable error){Toast.makeText(c,tr(c,"تعذر حفظ الاختيار","Could not save the selection"),Toast.LENGTH_LONG).show();return false;}}
 private static void updateMenu(Menu menu,Context c){if(c==null)return;for(int id:new int[]{SELECT,CLEAR,FOLLOW}){if(!enabled())menu.removeItem(id);else if(menu.findItem(id)==null)menu.add(0,id,100+id-SELECT,title(c,id)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);}}
 private static void refresh(){synchronized(MENUS){for(Map.Entry<Menu,java.lang.ref.WeakReference<Context>> e:MENUS.entrySet())updateMenu(e.getKey(),e.getValue().get());}Object[] prefs,adapters;synchronized(PREFS){prefs=PREFS.keySet().toArray();}synchronized(ADAPTERS){adapters=ADAPTERS.keySet().toArray();}for(Object pref:prefs)try{XposedHelpers.callMethod(pref,"refreshUiData");}catch(Throwable ignored){}for(Object adapter:adapters)try{XposedHelpers.callMethod(adapter,"notifyDataSetChanged");}catch(Throwable ignored){}}
 private static String title(Context c,int id){return id==SELECT?tr(c,"تحديد الكل…","Select all…"):id==CLEAR?tr(c,"إلغاء الكل","Clear all"):tr(c,"اتباع التردد المختار حاليًا","Follow current refresh rate");}
 private static String tr(Context c,String ar,String en){return c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar")?ar:en;}
 private static Object field(Object o,String name){for(Class<?> c=o==null?null:o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Throwable ignored){}return null;}
}
