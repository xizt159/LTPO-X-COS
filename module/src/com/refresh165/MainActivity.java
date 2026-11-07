package com.refresh165;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Rounded cards, two distinct sections, and root only for explicitly requested actions. */
public final class MainActivity extends Activity {
    private boolean restarting;
    private Switch refreshRateOverlaySwitch;
    private boolean refreshRateOverlayUpdating;
    private boolean refreshRateOverlaySyncing;
    @Override protected void onResume(){
        super.onResume();
        if(refreshRateOverlaySwitch!=null&&!refreshRateOverlayUpdating){
            refreshRateOverlaySyncing=true;
            refreshRateOverlaySwitch.setChecked(android.provider.Settings.Global.getInt(getContentResolver(),RefreshRateOverlay.SETTING,0)!=0);
            refreshRateOverlaySyncing=false;
        }
    }
    @Override protected void attachBaseContext(Context base){
        String lang=base.createDeviceProtectedStorageContext().getSharedPreferences("ui",0).getString("language","system");
        if(!lang.equals("system")){android.content.res.Configuration c=new android.content.res.Configuration(base.getResources().getConfiguration());c.setLocale(new java.util.Locale(lang));base=base.createConfigurationContext(c);}super.attachBaseContext(base);
    }
    private boolean arabic(){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");}
    private String tr(String value){return UiText.translate(value,arabic());}
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "1.0.0-beta.1";}}
    private void settings(){new AlertDialog.Builder(this).setTitle(tr("الإعدادات")).setMessage(tr("الإصدار: ")+version()).setPositiveButton(tr("اللغة"),(d,w)->{
        String selected=createDeviceProtectedStorageContext().getSharedPreferences("ui",0).getString("language","system");String[] ids={"system","en","ar"};int index=java.util.Arrays.asList(ids).indexOf(selected);
        new AlertDialog.Builder(this).setTitle(tr("اللغة")).setSingleChoiceItems(new String[]{tr("لغة النظام"),"English","العربية"},Math.max(0,index),(dialog,n)->{createDeviceProtectedStorageContext().getSharedPreferences("ui",0).edit().putString("language",ids[n]).apply();dialog.dismiss();recreate();}).setNegativeButton(tr("إلغاء"),null).show();
    }).setNegativeButton(tr("إغلاق"),null).show();}
    private void restartScopes(){
        if(restarting)return;
        new AlertDialog.Builder(this).setTitle("Restart scopes").setMessage(tr("الروت مطلوب لإعادة تشغيل AOD وSystemUI. قد تختفي الواجهة لحظيًا. بدون سماح الروت ستظل الإعدادات تعمل، وتحتاج إعادة تشغيل النطاقات يدويًا. تحديث APK يحتاج إعادة تشغيل الهاتف مرة واحدة لتحميل جزء النظام.")).setPositiveButton(tr("إعادة التشغيل"),(dialog,w)->{
            restarting=true;new Thread(()->{boolean granted=false,ok=false;try{granted=ScopeRestart.root();if(granted)ok=ScopeRestart.restart();}catch(Exception ignored){}final boolean root=granted,done=ok;runOnUiThread(()->{restarting=false;if(isFinishing())return;new AlertDialog.Builder(this).setTitle(tr(done?"تمت إعادة تشغيل النطاقات":"إعادة تشغيل النطاقات")).setMessage(tr(done?"أُرسل أمر إعادة تشغيل AOD وSystemUI.":!root?"لم يتم منح الروت. كل الإعدادات تظل تعمل؛ أعد تشغيل AOD وSystemUI يدويًا أو أعد تشغيل الهاتف.":"تعذر إعادة التشغيل. أعد تشغيل النطاقات يدويًا.")).setPositiveButton(tr("حسنًا"),null).show();});},"RestartScopes").start();
        }).setNegativeButton(tr("إلغاء"),null).show();
    }
    private void scopeHeader(){body.addView(text(tab==0?"DISPLAY & BRIGHTNESS":"ALWAYS-ON DISPLAY",12,muted));}

    private int tab=0,bg,card,ink,muted,accent=0xff56c8d7;
    private Bundle state;private LinearLayout root,body;
    private final String[] rates={"60 Hz","90 Hz","120 Hz","144 Hz","165 Hz"};
    private final int[] hz={60,90,120,144,165};
    @Override public void onCreate(Bundle b){super.onCreate(b);if(b!=null)tab=b.getInt("tab");render();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("tab",tab);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void load(){state=getContentResolver().call(Config.URI,"read",null,null);if(state==null)state=new Bundle();}
    private boolean on(String k){return state.getBoolean(k,Config.defaultOn(k));}
    private void save(String key,boolean val){Bundle b=new Bundle();b.putBoolean(key,val);write(b);}
    private void write(Bundle b){getContentResolver().call(Config.URI,"write",null,b);load();}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(tr(value));t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(3),0,dp(3));return t;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void render(){
        load();boolean dark=(getResources().getConfiguration().uiMode&0x30)==0x20;
        bg=dark?0xff101214:0xfff3f5f7;card=dark?0xff1d2226:Color.WHITE;ink=dark?0xffedf3f5:0xff172327;muted=dark?0xffa3b1b9:0xff62737b;
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=vertical();root.setPadding(dp(20),dp(16),dp(20),0);root.setBackgroundColor(bg);root.setLayoutDirection(arabic()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);header.setGravity(Gravity.CENTER_VERTICAL);TextView title=text("LTPO-X (COS)",28,ink);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));ImageButton restart=new ImageButton(this);restart.setImageDrawable(new ToolbarIcon(ink));restart.setBackground(shape(card,24));restart.setContentDescription(tr("إعادة تشغيل النطاقات"));restart.setPadding(dp(13),dp(13),dp(13),dp(13));restart.setOnClickListener(v->restartScopes());header.addView(restart,new LinearLayout.LayoutParams(dp(48),dp(48)));Button gear=new Button(this);gear.setText("⚙");gear.setContentDescription(tr("الإعدادات"));gear.setOnClickListener(v->settings());header.addView(gear,new LinearLayout.LayoutParams(dp(55),dp(48)));root.addView(header);
        root.addView(text("OnePlus 15  ·  ColorOS 17  ·  "+version(),13,muted));
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(0,dp(15),0,dp(12));root.addView(tabs);
        for(int i=0;i<2;i++){final int selected=i;TextView t=text(i==0?"الشاشة والترددات":"Always-on Display",15,tab==i?0xff102327:muted);t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(13),dp(8),dp(13));t.setBackground(shape(tab==i?accent:card,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);tabs.addView(t,lp);t.setOnClickListener(v->{tab=selected;render();});}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=vertical();body.setPadding(0,0,0,dp(24));scroll.addView(body);
        scopeHeader();if(tab==0)display();else aod();
    }
    private LinearLayout group(String title){body.addView(text(title,14,muted));LinearLayout l=vertical();l.setPadding(dp(16),dp(7),dp(16),dp(7));l.setBackground(shape(card,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(20));body.addView(l,lp);return l;}
    private void info(LinearLayout group,String message){TextView t=text(message,13,muted);t.setPadding(0,dp(8),0,dp(10));group.addView(t);}
    private void toggle(LinearLayout group,String key,String title,String summary,String master){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));group.addView(row);
        LinearLayout labels=vertical();row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView t=text(title,16,ink);labels.addView(t);labels.addView(text(summary,12,muted));
        Switch sw=new Switch(this);sw.setContentDescription(tr(title));sw.setChecked(on(key));sw.setEnabled(master==null||on(master));sw.setPadding(dp(12),0,0,0);row.addView(sw);
        if(!sw.isEnabled())row.setAlpha(0.45f);
        sw.setOnCheckedChangeListener((button,value)->{save(key,value);if(master==null)render();});
        row.setOnClickListener(v->{if(sw.isEnabled())sw.setChecked(!sw.isChecked());});
    }
    private void action(LinearLayout group,String title,String subtitle,Runnable click){LinearLayout l=vertical();l.setPadding(0,dp(12),0,dp(12));l.addView(text(title+"  ‹",16,ink));l.addView(text(subtitle,12,muted));group.addView(l);l.setOnClickListener(v->click.run());}
    private void display(){
        LinearLayout l=group("DISPLAY & BRIGHTNESS");
        toggle(l,"refresh","تفعيل تعديلات الشاشة","مفتاح مستقل عن خصائص AOD",null);
        action(l,"تردد التفاعل: "+state.getInt("rate",165)+" Hz","سقف التفاعل والأنيميشن؛ الخمول يظل تكيفيًا",()->{
            int selected=Arrays.binarySearch(hz,state.getInt("rate",165));new AlertDialog.Builder(this).setTitle(tr("تردد الشاشة")).setSingleChoiceItems(rates,Math.max(0,selected),(d,n)->{Bundle b=new Bundle();b.putInt("rate",hz[n]);write(b);d.dismiss();render();}).setNegativeButton(tr("إلغاء"),null).show();
        });
        toggle(l,"menu","كل الترددات داخل الإعدادات","نفس قائمة النظام الأصلية مع 60 / 90 / 120 / 144 / 165","refresh");
        toggle(l,"disable_ltpo","تعطيل LTPO أثناء التفاعل العادي","يثبّت التردد المختار؛ حدود الفيديو والكيبورد والحرارة تظل نافذة","refresh");
        toggle(l,"app_policies","تفعيل سياسات تردد التطبيقات","ثابت أو متغير لكل تطبيق؛ تُضبط من صفحة التطبيقات المدعومة في إعدادات النظام","refresh");
        action(l,"التطبيقات المدعومة","إعدادات الشاشة ← معدل التحديث ← التطبيقات المدعومة",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(Exception ignored){}});
        l=group("LTPO والتفاعل");
        toggle(l,"low_light","السماح بالخمول في السطوع المنخفض","الحفاظ على DC مع بوابة الخمول المقيّدة بشروط النظام","refresh");
        toggle(l,"tables","جداول خمول 1 Hz","تعديل الجداول المعروفة لـ60 / 90 / 120 / 165؛ النتيجة تحتاج قياسًا","refresh");
        toggle(l,"fast_idle","خفض زمن انتظار الخمول","600ms بدل 2500ms للملف الافتراضي فقط؛ لا تتابع إجباري","refresh");
        toggle(l,"animation","أنيميشن على التردد المختار","لللانشر والانتقالات المدعومة، مع احترام الحرارة والبصمة","refresh");
        toggle(l,"ime","الكيبورد: سقف 60 Hz","يبدأ بعد 300ms من الظهور، ثم يستعيد السياسة السابقة","refresh");
        toggle(l,"video","الفيديو: 60 / 30 Hz عند المشاهدة","بعد ثبات المحتوى وعدم اللمس؛ التمرير يعيد سقف التفاعل فورًا","refresh");
        l=group("تجريبي · 144 Hz");
        toggle(l,"gate144","144 → 120 أثناء الخمول","استخدام توقيت120 الحقيقي للوصول إلى1Hz، والرجوع إلى144 عند التفاعل","refresh");
        info(l,"تحويل تلقائي بعد ثبات الصفحة وعدم اللمس. لا يتم نسخ أوامر120 إلى توقيت144. التجربة تحتاج قياسًا على الجهاز.");
        footer();
    }
    private void aod(){
        LinearLayout l=group("COLOROS AOD");
        toggle(l,"aod","تفعيل خصائص AOD","مستقل عن تعديلات الشاشة والترددات",null);
        toggle(l,"panoramic","Full-screen AOD","إتاحة العرض البانورامي في مسارات النظام المتوافقة","aod");
        toggle(l,"all_day","العرض طوال اليوم","إتاحة خيار البانوراما طوال اليوم في الإعدادات","aod");
        toggle(l,"block_tap","نقرتان لإظهار AOD ثم نقرة للقفل","بعد ظهور AOD، النقرة التالية تفتح شاشة القفل دون تجاوزها","aod");
        toggle(l,"block_lift","منع تشغيل AOD عند رفع الهاتف","يوقف إظهاره بالحركة فقط؛ النقر يظل يعمل","aod");
        toggle(l,"battery_only","نسبة البطارية فقط أثناء AOD","يعرض النسبة وحدها بدل شريط الحالة؛ شاشة القفل تظل طبيعية","aod");
        toggle(l,"dark_visible","منع الإخفاء بسبب الظلام","يبقي AOD ظاهرًا في مسار الإضاءة المنخفضة","aod");
        l=group("الخلفية");
        toggle(l,"hide_wallpaper","إخفاء خلفية AOD","خلفية سوداء لجميع أوضاع AOD؛ إيقافه يعيد سلوك النظام","aod");
        info(l,"Full-screen يحتفظ بتصميمه مع خلفية سوداء مثل Partial، في جميع أوضاع العرض. خلفية القفل تعود عند الاستيقاظ.");
        l=group("السطوع");
        toggle(l,"fixed_brightness","سطوع AOD ثابت","يتقدّم على سطوع البداية والمعامل؛ إيقافه يعيد التعديلات السابقة","aod");
        slider(l,"fixed","قيمة السطوع الثابت",80,2,255,"");
        toggle(l,"initial_dark","تخصيص سطوع البداية في الظلام","مفتاح مستقل عن سطوع البداية في الضوء","aod");
        slider(l,"dark","سطوع البداية في الظلام",80,1,255,"");
        toggle(l,"initial_bright","تخصيص سطوع البداية في الضوء","يستخدم قيمة النظام عند إيقاف هذا المفتاح","aod");
        slider(l,"bright","سطوع البداية في الضوء",160,1,255,"");
        toggle(l,"running_brightness","تعديل السطوع أثناء التشغيل","معامل على قيم Android17 العشرية، دون ضرب القيمة مرتين","aod");
        slider(l,"multiplier","معامل السطوع",160,25,300,"%");
        footer();
    }
    private void slider(LinearLayout group,String key,String title,int def,int min,int max,String suffix){
        TextView value=text(title+": "+state.getInt(key,def)+suffix,14,ink);value.setPadding(0,dp(12),0,0);group.addView(value);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(state.getInt(key,def)-min);bar.setContentDescription(tr(title));bar.setEnabled(on("aod"));group.addView(bar,new LinearLayout.LayoutParams(-1,dp(45)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){value.setText(tr(title)+": "+(n+min)+suffix);}public void onStopTrackingTouch(SeekBar b){Bundle next=new Bundle();next.putInt(key,b.getProgress()+min);write(next);}});
    }
    private void footer(){
        LinearLayout l=group("التفعيل");
        Switch overlay=new Switch(this);refreshRateOverlaySwitch=overlay;
        overlay.setContentDescription(tr("إظهار معدل التحديث"));
        overlay.setChecked(android.provider.Settings.Global.getInt(getContentResolver(),RefreshRateOverlay.SETTING,0)!=0);
        overlay.setOnCheckedChangeListener((button,enabled)->{
            if(!refreshRateOverlaySyncing)setRefreshRateOverlay(overlay,enabled);
        });
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));l.addView(row);
        LinearLayout labels=vertical();row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        labels.addView(text("إظهار معدل التحديث",16,ink));
        labels.addView(text("عرض الرقم الأخضر لمعدل تحديث الشاشة أعلى اليسار. يتطلب صلاحية الروت.",12,muted));
        row.addView(overlay);row.setOnClickListener(v->{if(overlay.isEnabled())overlay.setChecked(!overlay.isChecked());});
        info(l,"فعّل LTPO-X (COS) داخل LSPosed للنظام والإعدادات وSystemUI وAOD، ثم أعد التشغيل. لا تفعّل FreshShift أو ColorOS AOD أو موديول هرتز آخر معه. تعديل التردد والخصائص يعمل من هنا؛ تغييرات إتاحة AOD قد تحتاج إعادة فتح صفحته أو إعادة التشغيل.");
        action(l,"فتح إعدادات الشاشة","لمراجعة خيارات النظام",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(Exception ignored){}});
    }
    private void setRefreshRateOverlay(Switch toggle,boolean enabled){
        if(refreshRateOverlayUpdating)return;
        refreshRateOverlayUpdating=true;toggle.setEnabled(false);
        new Thread(()->{
            String failure=null;
            try{RefreshRateOverlay.setEnabled(enabled);}
            catch(Exception e){failure=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
            final String error=failure;
            runOnUiThread(()->{
                refreshRateOverlayUpdating=false;
                if(isFinishing())return;
                toggle.setEnabled(true);
                if(error!=null){
                    refreshRateOverlaySyncing=true;
                    toggle.setChecked(!enabled);
                    refreshRateOverlaySyncing=false;
                    new AlertDialog.Builder(this).setTitle(tr("إظهار معدل التحديث"))
                            .setMessage(tr("تعذر تغيير الإعداد. تأكد من منح صلاحية الروت، ثم حاول مجددًا.")+"\n\n"+error)
                            .setPositiveButton(tr("حسنًا"),null).show();
                }
            });
        },"RefreshRateOverlay").start();
    }
    private void apps(){
        new Thread(()->{
            List<ResolveInfo> raw=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
            TreeMap<String,String> packages=new TreeMap<>();for(ResolveInfo r:raw){String pkg=r.activityInfo.packageName;if(!pkg.equals(getPackageName()))packages.put(pkg,String.valueOf(r.loadLabel(getPackageManager())));}
            List<String> ids=new ArrayList<>(packages.keySet());ids.sort(Comparator.comparing(packages::get,String.CASE_INSENSITIVE_ORDER));String[] labels=new String[ids.size()];boolean[] checked=new boolean[ids.size()];Set<String> saved=new HashSet<>(Arrays.asList(state.getString("apps","").split(";")));
            for(int i=0;i<ids.size();i++){labels[i]=packages.get(ids.get(i))+"\n"+ids.get(i);checked[i]=saved.contains(ids.get(i));}
            runOnUiThread(()->{if(isFinishing())return;AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("التطبيقات المدعومة")).setMultiChoiceItems(labels,checked,(dialog,n,c)->checked[n]=c).setPositiveButton(tr("حفظ"),(dialog,w)->saveApps(ids,checked)).setNegativeButton(tr("إلغاء"),null).setNeutralButton(tr("الكل / لا شيء"),null).create();d.setOnShowListener(v->d.getButton(-3).setOnClickListener(button->{boolean all=true;for(boolean c:checked)all&=c;for(int i=0;i<checked.length;i++){checked[i]=!all;d.getListView().setItemChecked(i,!all);}}));d.show();});
        },"DisplayAppList").start();
    }
    private void saveApps(List<String> ids,boolean[] checked){List<String> chosen=new ArrayList<>();for(int i=0;i<ids.size();i++)if(checked[i])chosen.add(ids.get(i));Bundle b=new Bundle();b.putString("apps",String.join(";",chosen));write(b);}
}
