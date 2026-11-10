package com.ltpoxcos;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import com.ltpoxcos.xp.*;
import java.util.*;
/** An AOD-only label in the existing shade root, independent of the hidden keyguard bar.
 * Native frame invalidations and battery events drive it; no timer, wake lock or sensor. */
final class AodBatteryPolicy {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final Map<FrameLayout,java.lang.ref.WeakReference<Label>> ROOTS=new WeakHashMap<>();
 private static boolean receiver;private static int percent=-1;
 static boolean visible(boolean enabled,boolean asleep,int showing,int level){return enabled&&asleep&&showing==1&&level>=0&&level<=100;}
 static void install(ClassLoader cl){try{
  Class<?> type=Class.forName("com.android.systemui.shade.NotificationShadeWindowView",false,cl);
  XposedBridge.hookMethod(type.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.thisObject instanceof FrameLayout)attach((FrameLayout)p.thisObject);}});
  Config.listen(()->MAIN.post(AodBatteryPolicy::refresh));
 }catch(Throwable ignored){}}
 private static void attach(FrameLayout root){
  if(ROOTS.containsKey(root))return;Context c=root.getContext();Label label=new Label(c);ROOTS.put(root,new java.lang.ref.WeakReference<>(label));
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);float density=c.getResources().getDisplayMetrics().density;lp.setMarginEnd(Math.round(28*density));int id=c.getResources().getIdentifier("status_bar_height","dimen","android");lp.topMargin=id==0?Math.round(28*density):Math.max(0,(c.getResources().getDimensionPixelSize(id)-Math.round(20*density))/2);root.addView(label,lp);label.setTranslationZ(10000f);
  root.getViewTreeObserver().addOnPreDrawListener(()->{update(root,label);return true;});
  if(!receiver){receiver=true;IntentFilter f=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_SCREEN_OFF);Intent sticky=c.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context context,Intent i){read(i);refresh();}},f,Context.RECEIVER_EXPORTED);read(sticky);}
  update(root,label);
 }
 private static void read(Intent i){if(i==null||!Intent.ACTION_BATTERY_CHANGED.equals(i.getAction()))return;int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1);percent=level>=0&&scale>0&&level<=scale?(int)Math.round(level*100.0/scale):-1;}
 private static void refresh(){for(Map.Entry<FrameLayout,java.lang.ref.WeakReference<Label>> e:new ArrayList<>(ROOTS.entrySet())){FrameLayout root=e.getKey();Label label=e.getValue().get();if(root!=null&&label!=null){update(root,label);root.invalidate();}}}
 private static void update(FrameLayout root,Label label){boolean on=Config.aod("battery_only");boolean show=on&&visible(true,AodAccess.asleep(root.getContext()),AodAccess.showing(root.getContext()),percent);int visibility=show?View.VISIBLE:View.GONE;if(label.getVisibility()!=visibility)label.setVisibility(visibility);if(show){String text=percent+"%";if(!text.contentEquals(label.getText())){label.setText(text);label.setContentDescription(text);}}}
 private static final class Label extends TextView {Label(Context c){super(c);setTextColor(Color.WHITE);setTextSize(14);setVisibility(View.GONE);setClickable(false);setFocusable(false);}@Override public boolean onTouchEvent(MotionEvent event){return false;}}
}
