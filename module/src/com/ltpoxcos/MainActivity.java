package com.ltpoxcos;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
    private boolean refreshRateOverlayStateLoading;
    private static final String SUPPORTED_APPS_FRAGMENT="com.oplus.settings.feature.display.refreshhigh.ScreenRefreshHighAppNewFragment";
    @Override protected void onResume(){
        super.onResume();
        syncRefreshRateOverlayState();
    }
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "1.0.1";}}
    private String systemVersion(){
        return SystemVersion.label(systemProperty("ro.oxygenos.version","ro.oxygen.version"),
                systemProperty("ro.build.version.oplusrom"),Build.DISPLAY+" "+systemProperty("ro.build.version.ota"),
                Build.VERSION.RELEASE,Build.MANUFACTURER);
    }
    private String systemProperty(String... keys){
        for(String key:keys)try{
            String value=(String)Class.forName("android.os.SystemProperties").getMethod("get",String.class,String.class).invoke(null,key,"");
            if(value!=null&&!value.isEmpty())return value;
        }catch(ReflectiveOperationException|SecurityException|LinkageError ignored){}
        return "";
    }
    private void help(){
        new AlertDialog.Builder(this).setTitle("How to activate")
                .setMessage("Enable LTPO-X (COS) in LSPosed for Android, Settings, SystemUI and AOD, then reboot. Disable overlapping display modules. AOD availability changes may require reopening its settings or restarting its scopes.")
                .setPositiveButton("Close",null).show();
    }
    private void openSupportedApps(){
        Intent intent=new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
        intent.putExtra(":settings:show_fragment",SUPPORTED_APPS_FRAGMENT);
        intent.putExtra(":settings:show_fragment_title","Supported apps");
        intent.putExtra(":settings:show_fragment_as_subsetting",true);
        try{startActivity(intent);}
        catch(ActivityNotFoundException e){
            new AlertDialog.Builder(this).setTitle("Supported apps")
                    .setMessage("Could not open the native Supported apps screen.\n\n"+e.getMessage())
                    .setPositiveButton("OK",null).show();
        }
    }
    private void restartScopes(){
        if(restarting)return;
        new AlertDialog.Builder(this).setTitle("Restart scopes").setMessage("Root is required to restart AOD and SystemUI. The interface may briefly disappear. Without root, settings still work; restart the scopes manually. An APK update requires one full reboot to load system-server changes.").setPositiveButton("Restart",(dialog,w)->{
            restarting=true;new Thread(()->{boolean granted=false,ok=false;try{granted=ScopeRestart.root();if(granted)ok=ScopeRestart.restart();}catch(Exception ignored){}final boolean root=granted,done=ok;runOnUiThread(()->{restarting=false;if(isFinishing())return;new AlertDialog.Builder(this).setTitle(done?"Scopes restarted":"Scope restart").setMessage(done?"Restart request sent to AOD and SystemUI.":!root?"Root permission was not granted. Settings still work; restart AOD and SystemUI manually or reboot the phone.":"Restart failed. Restart the scopes manually.").setPositiveButton("OK",null).show();});},"RestartScopes").start();
        }).setNegativeButton("Cancel",null).show();
    }
    private int tab=0,bg,card,ink,muted,accent=0xff56c8d7;
    private Bundle state;private LinearLayout root,body;private ScrollView contentScroll;
    private String[] rates={"60 Hz","90 Hz","120 Hz","144 Hz","165 Hz"};
    private int[] hz={60,90,120,144,165};
    @Override public void onCreate(Bundle b){super.onCreate(b);if(b!=null)tab=b.getInt("tab");detectRefreshRates();render();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("tab",tab);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void load(){state=getContentResolver().call(Config.URI,"read",null,null);if(state==null)state=new Bundle();}
    private boolean on(String k){return state.getBoolean(k,Config.defaultOn(k));}
    private void save(String key,boolean val){Bundle b=new Bundle();b.putBoolean(key,val);write(b);}
    private void write(Bundle b){getContentResolver().call(Config.URI,"write",null,b);load();}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(3),0,dp(3));return t;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void render(){
        refreshRateOverlaySwitch=null;
        load();boolean dark=(getResources().getConfiguration().uiMode&0x30)==0x20;
        bg=dark?0xff101214:0xfff3f5f7;card=dark?0xff1d2226:Color.WHITE;ink=dark?0xffedf3f5:0xff172327;muted=dark?0xffa3b1b9:0xff62737b;
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=vertical();root.setPadding(dp(20),dp(16),dp(20),0);root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("LTPO-X (COS)",28,ink);title.setTypeface(null,Typeface.BOLD);title.setIncludeFontPadding(false);title.setPadding(0,0,0,0);title.setGravity(Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        ImageButton restart=new ImageButton(this);restart.setImageDrawable(new ToolbarIcon(ink));restart.setBackground(shape(card,21));restart.setContentDescription("Restart scopes");restart.setPadding(dp(11),dp(11),dp(11),dp(11));restart.setOnClickListener(v->restartScopes());header.addView(restart,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView helpButton=text("?",20,ink);helpButton.setIncludeFontPadding(false);helpButton.setGravity(Gravity.CENTER);helpButton.setPadding(0,0,0,0);helpButton.setBackground(shape(card,21));helpButton.setContentDescription("Help");helpButton.setOnClickListener(v->help());LinearLayout.LayoutParams helpParams=new LinearLayout.LayoutParams(dp(42),dp(42));helpParams.setMargins(dp(8),0,0,0);header.addView(helpButton,helpParams);
        root.addView(header);
        root.addView(text(systemVersion()+"  ·  "+version(),13,muted));
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(0,dp(15),0,dp(12));root.addView(tabs);
        for(int i=0;i<2;i++){final int selected=i;TextView t=text(i==0?"Display & Brightness":"Always-on Display",15,tab==i?0xff102327:muted);t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(13),dp(8),dp(13));t.setBackground(shape(tab==i?accent:card,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);tabs.addView(t,lp);t.setOnClickListener(v->{tab=selected;render();});}
        ScrollView scroll=new ScrollView(this);contentScroll=scroll;scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=vertical();body.setPadding(0,0,0,dp(24));scroll.addView(body);
        if(tab==0)display();else aod();
    }
    private void renderPreservingScroll(){
        int scrollY=contentScroll==null?0:contentScroll.getScrollY();
        render();
        ScrollView scroll=contentScroll;
        scroll.post(()->scroll.scrollTo(0,scrollY));
    }
    private LinearLayout group(String title){body.addView(text(title,14,muted));LinearLayout l=vertical();l.setPadding(dp(16),dp(7),dp(16),dp(7));l.setBackground(shape(card,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(20));body.addView(l,lp);return l;}
    private void info(LinearLayout group,String message){TextView t=text(message,13,muted);t.setPadding(0,dp(8),0,dp(10));group.addView(t);}
    private void toggle(LinearLayout group,String key,String title,String summary,String master){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));group.addView(row);
        LinearLayout labels=vertical();row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView t=text(title,16,ink);labels.addView(t);labels.addView(text(summary,12,muted));
        Switch sw=new Switch(this);sw.setContentDescription(title);sw.setChecked(on(key));sw.setEnabled(master==null||on(master));sw.setPadding(dp(12),0,0,0);row.addView(sw);
        if(!sw.isEnabled())row.setAlpha(0.45f);
        sw.setOnCheckedChangeListener((button,value)->{save(key,value);if(master==null)renderPreservingScroll();});
        row.setOnClickListener(v->{if(sw.isEnabled())sw.setChecked(!sw.isChecked());});
    }
    private void action(LinearLayout group,String title,String subtitle,boolean selection,Runnable click){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));group.addView(row);
        LinearLayout labels=vertical();row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));labels.addView(text(title,16,ink));labels.addView(text(subtitle,12,muted));
        View mark;
        if(selection){ImageView icon=new ImageView(this);icon.setImageDrawable(new ExpandMoreIcon(ink));icon.setContentDescription("Select");mark=icon;}
        else{TextView icon=text("↗",24,ink);icon.setIncludeFontPadding(false);icon.setGravity(Gravity.CENTER);icon.setPadding(0,0,0,0);icon.setContentDescription("Open");mark=icon;}
        row.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));
        row.setOnClickListener(v->click.run());
    }
    private void display(){
        LinearLayout l=group("Display & Brightness");
        toggle(l,"refresh","Enable display adjustments","Independent of AOD features",null);
        boolean enabled=on("refresh");
        l=group("Refresh rate");
        action(l,"Open display settings","Review system display options",false,()->{try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(Exception ignored){}});
        if(enabled){
            action(l,"Maximum refresh rate: "+state.getInt("rate",165)+" Hz","Caps interaction and animation rates while preserving adaptive LTPO idle.",true,()->{
                int selected=Arrays.binarySearch(hz,state.getInt("rate",165));new AlertDialog.Builder(this).setTitle("Maximum refresh rate").setSingleChoiceItems(rates,Math.max(0,selected),(d,n)->{Bundle b=new Bundle();b.putInt("rate",hz[n]);write(b);d.dismiss();renderPreservingScroll();}).setNegativeButton("Cancel",null).show();
            });
            info(l,"LTPO-X applies the selected maximum regardless of the system refresh mode. Idle minimum: 1 Hz at 60 / 90 / 120 Hz; at 144 Hz, enable “144 → 120 while idle” to reach 1 Hz. At 165 Hz, the idle floor is firmware-dependent (about 33–55 Hz).");
            toggle(l,"disable_ltpo","Disable LTPO during interaction","Hold the selected rate; video, keyboard and thermal ceilings still apply","refresh");
            toggle(l,"app_policies","Enable per-app refresh policies","Choose fixed or variable per app in the native Supported apps page","refresh");
            action(l,"Supported apps","Per-app refresh controls in system settings",false,this::openSupportedApps);
            toggle(l,"lock_camera_120","Lock screen & camera at 120 Hz","Cap refresh at 120 Hz while locked or when a camera app is foreground; the camera viewfinder remains camera-controlled.","refresh");
            l=group("Battery Saver");
            toggle(l,"battery_saver_cap","Battery Saver Cap","Limits the maximum to 60 Hz while Android Battery Saver is active; LTPO idle remains adaptive.","refresh");
            l=group("LTPO and interaction");
            toggle(l,"low_light","Allow idle at low brightness","Retain DC eligibility and the guarded native idle path","refresh");
            toggle(l,"tables","1 Hz idle tables","Adapt known 60 / 90 / 120 / 165 Hz tables; hardware confirmation is required","refresh");
            toggle(l,"fast_idle","Shorter idle timeout","600 ms instead of 2500 ms for the known default profile; no forced sequence","refresh");
            toggle(l,"animation","Animations at the selected rate","Supported launcher and transition paths; thermal and fingerprint policies remain effective","refresh");
            toggle(l,"ime","Keyboard: 60 Hz ceiling","Start 300 ms after the keyboard appears, then restore the previous policy","refresh");
            toggle(l,"video","Video: 60 / 30 Hz while watching","After content settles and input stops; scrolling immediately restores the interaction ceiling","refresh");
        }
        l=group("Developer options");
        refreshRateOverlayToggle(l);
        if(enabled){
            toggle(l,"gate144","144 → 120 while idle","Use the real 120 Hz timing for 1 Hz idle, then return to 144 Hz on interaction","refresh");
            info(l,"Switch only after static content and no input. No 120 Hz panel commands are copied into the 144 Hz timing. Device testing is required.");
        }
        syncRefreshRateOverlayState();
    }
    private void aod(){
        LinearLayout l=group("Always-On Display");
        toggle(l,"aod","Enable AOD features","Independent of display and refresh adjustments",null);
        if(on("aod")){
            l=group("AOD features");
            toggle(l,"panoramic","Full-screen AOD","Enable supported panoramic AOD paths","aod");
            toggle(l,"all_day","All-day display","Expose the all-day panoramic option in system settings","aod");
            toggle(l,"block_tap","Two taps to reveal AOD, then one tap to lock","Once AOD is visible, the next tap wakes the lock screen without dismissing it","aod");
            toggle(l,"block_lift","Block lift-to-show AOD","Prevent motion from revealing AOD; tap gestures still work","aod");
            toggle(l,"battery_only","Battery percentage only on AOD","Show only the percentage instead of the status bar; lock screen stays unchanged","aod");
            toggle(l,"dark_visible","Prevent hiding in darkness","Keep AOD visible through the low-light hiding path","aod");
            l=group("Wallpaper");
            toggle(l,"hide_wallpaper","Hide AOD wallpaper","Black backdrop in all AOD modes; switch off to restore native behavior","aod");
            info(l,"Full-screen keeps its design with a black backdrop like Partial, in every display mode. The lock screen wallpaper returns on wake.");
            l=group("Brightness");
            toggle(l,"fixed_brightness","Fixed AOD brightness","Overrides initial brightness and multiplier; switch off to restore your previous settings","aod");
            slider(l,"fixed","Fixed brightness level",80,2,255,"");
            toggle(l,"initial_dark","Customize initial brightness in darkness","Independent of initial brightness in light","aod");
            slider(l,"dark","Initial brightness in darkness",80,1,255,"");
            toggle(l,"initial_bright","Customize initial brightness in light","Use the native value when this switch is off","aod");
            slider(l,"bright","Initial brightness in light",160,1,255,"");
            toggle(l,"running_brightness","Adjust running brightness","Scale normalized Android 17 values once, without multiplying forwarded calls twice","aod");
            slider(l,"multiplier","Brightness multiplier",160,25,300,"%");
        }
    }
    private void slider(LinearLayout group,String key,String title,int def,int min,int max,String suffix){
        TextView value=text(title+": "+state.getInt(key,def)+suffix,14,ink);value.setPadding(0,dp(12),0,0);group.addView(value);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(state.getInt(key,def)-min);bar.setContentDescription(title);bar.setEnabled(on("aod"));group.addView(bar,new LinearLayout.LayoutParams(-1,dp(45)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){value.setText(title+": "+(n+min)+suffix);}public void onStopTrackingTouch(SeekBar b){Bundle next=new Bundle();next.putInt(key,b.getProgress()+min);write(next);}});
    }
    private void detectRefreshRates(){
        TreeSet<Integer> available=new TreeSet<>(Arrays.asList(60,90,120,144,165));
        for(android.view.Display.Mode mode:getWindowManager().getDefaultDisplay().getSupportedModes()){
            float refresh=mode.getRefreshRate();
            if(Math.abs(refresh-185f)<0.5f)available.add(185);
        }
        hz=new int[available.size()];rates=new String[available.size()];
        int index=0;for(int rate:available){hz[index]=rate;rates[index]=rate+" Hz";index++;}
    }
    private void refreshRateOverlayToggle(LinearLayout group){
        Switch overlay=new Switch(this);refreshRateOverlaySwitch=overlay;
        overlay.setContentDescription("Show refresh rate");
        overlay.setEnabled(false);
        overlay.setOnCheckedChangeListener((button,enabled)->{
            if(!refreshRateOverlaySyncing)setRefreshRateOverlay(overlay,enabled);
        });
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));group.addView(row);
        LinearLayout labels=vertical();row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        labels.addView(text("Show refresh rate",16,ink));
        labels.addView(text("Show the green display refresh-rate indicator in the top-left corner. Root access is required.",12,muted));
        row.addView(overlay);row.setOnClickListener(v->{if(overlay.isEnabled())overlay.setChecked(!overlay.isChecked());});
    }
    private void syncRefreshRateOverlayState(){
        Switch toggle=refreshRateOverlaySwitch;
        if(toggle==null||refreshRateOverlayUpdating||refreshRateOverlayStateLoading)return;
        refreshRateOverlayStateLoading=true;toggle.setEnabled(false);
        new Thread(()->{
            boolean enabled=false;String failure=null;
            try{enabled=RefreshRateOverlay.isEnabled();}
            catch(Exception e){failure=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
            final boolean actual=enabled;final String error=failure;
            runOnUiThread(()->{
                refreshRateOverlayStateLoading=false;
                Switch current=refreshRateOverlaySwitch;
                if(isFinishing()||current==null)return;
                refreshRateOverlaySyncing=true;current.setChecked(actual);refreshRateOverlaySyncing=false;
                current.setEnabled(true);
                if(error!=null){
                    new AlertDialog.Builder(this).setTitle("Show refresh rate")
                            .setMessage("Could not read the refresh-rate overlay state. Grant root access, then try again.\n\n"+error)
                            .setPositiveButton("OK",null).show();
                }
            });
        },"ReadRefreshRateOverlay").start();
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
                Switch current=refreshRateOverlaySwitch;
                if(isFinishing()||current==null)return;
                refreshRateOverlaySyncing=true;
                current.setChecked(error==null?enabled:!enabled);
                refreshRateOverlaySyncing=false;
                current.setEnabled(true);
                if(error!=null){
                    new AlertDialog.Builder(this).setTitle("Show refresh rate")
                            .setMessage("Could not change the setting. Make sure root access is granted, then try again.\n\n"+error)
                            .setPositiveButton("OK",null).show();
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
            runOnUiThread(()->{if(isFinishing())return;AlertDialog d=new AlertDialog.Builder(this).setTitle("Supported apps").setMultiChoiceItems(labels,checked,(dialog,n,c)->checked[n]=c).setPositiveButton("Save",(dialog,w)->saveApps(ids,checked)).setNegativeButton("Cancel",null).setNeutralButton("All / None",null).create();d.setOnShowListener(v->d.getButton(-3).setOnClickListener(button->{boolean all=true;for(boolean c:checked)all&=c;for(int i=0;i<checked.length;i++){checked[i]=!all;d.getListView().setItemChecked(i,!all);}}));d.show();});
        },"DisplayAppList").start();
    }
    private void saveApps(List<String> ids,boolean[] checked){List<String> chosen=new ArrayList<>();for(int i=0;i<ids.size();i++)if(checked[i])chosen.add(ids.get(i));Bundle b=new Bundle();b.putString("apps",String.join(";",chosen));write(b);}
    private static final class ExpandMoreIcon extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ExpandMoreIcon(int color){paint.setColor(color);paint.setStrokeWidth(3);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override public void draw(Canvas canvas){
            Rect bounds=getBounds();int save=canvas.save();canvas.translate((bounds.left+bounds.right)/2f,(bounds.top+bounds.bottom)/2f);float scale=Math.min(bounds.width(),bounds.height())/48f;canvas.scale(scale,scale);
            Path path=new Path();path.moveTo(-10,-4);path.lineTo(0,6);path.lineTo(10,-4);canvas.drawPath(path,paint);canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
