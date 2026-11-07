package com.refresh165;
import android.content.*;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.*;
import android.view.View;
import com.refresh165.xp.*;
import java.lang.reflect.*;
import java.util.*;

/** The native scrim_behind is below clocks/notifications. Make this backdrop
 * opaque black during any noninteractive AOD mode; leave wallpaper surfaces native. */
final class AodBackdropPolicy {
    private static final Map<Object,State> VIEWS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<>();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static boolean registered;
    private static final class State {volatile float alpha;volatile int tint;volatile boolean overridden;}
    static boolean black(boolean feature,boolean interactive,String role){return feature&&!interactive&&"scrim_behind".equals(role);}
    static void install(ClassLoader cl){try{
        Class<?> scrim=Class.forName("com.android.systemui.scrim.ScrimView",false,cl);
        Method alpha=scrim.getDeclaredMethod("setViewAlpha",float.class),tint=scrim.getDeclaredMethod("setTint",int.class),draw=scrim.getDeclaredMethod("onDraw",Canvas.class);
        XposedBridge.hookMethod(alpha,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            float value=(Float)p.args[0];if(!Float.isFinite(value)||value<0f||value>1f)return;
            State s=track(p.thisObject);if(s==null)return;if(!Boolean.TRUE.equals(INTERNAL.get()))s.alpha=value;if(enabled((View)p.thisObject)){p.args[0]=1f;s.overridden=true;}
        }});
        XposedBridge.hookMethod(tint,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            State s=track(p.thisObject);if(s==null)return;if(!Boolean.TRUE.equals(INTERNAL.get()))s.tint=(Integer)p.args[0];if(enabled((View)p.thisObject)){p.args[0]=Color.BLACK;s.overridden=true;}
        }});
        XposedBridge.hookMethod(draw,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
            View view=(View)p.thisObject;State s=track(view);if(s!=null&&enabled(view)){((Canvas)p.args[0]).drawColor(Color.BLACK);p.setResult(null);}
        }});
        Config.listen(()->MAIN.post(AodBackdropPolicy::refresh));
    }catch(Throwable ignored){}}
    private static boolean enabled(View view){return Config.aod("hide_wallpaper")&&AodAccess.asleep(view.getContext());}
    private static State track(Object object){
        if(!(object instanceof View))return null;View view=(View)object;
        try{if(!"scrim_behind".equals(view.getResources().getResourceEntryName(view.getId())))return null;
            synchronized(VIEWS){State s=VIEWS.get(view);if(s==null){s=new State();s.alpha=((Number)XposedHelpers.callMethod(view,"getViewAlpha")).floatValue();s.tint=((Number)XposedHelpers.callMethod(view,"getTint")).intValue();VIEWS.put(view,s);}ensureReceiver(view.getContext());return s;}
        }catch(Throwable ignored){return null;}
    }
    private static void ensureReceiver(Context context){if(registered)return;try{
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){MAIN.post(AodBackdropPolicy::refresh);}},f,null,MAIN,Context.RECEIVER_NOT_EXPORTED);registered=true;
    }catch(Throwable ignored){}}
    private static void refresh(){
        Map<Object,State> copy;synchronized(VIEWS){copy=new HashMap<>(VIEWS);}
        for(Map.Entry<Object,State> e:copy.entrySet())try{View v=(View)e.getKey();State s=e.getValue();boolean hide=enabled(v);
            INTERNAL.set(true);if(hide||s.overridden){XposedHelpers.callMethod(v,"setTint",hide?Color.BLACK:s.tint);XposedHelpers.callMethod(v,"setViewAlpha",hide?1f:s.alpha);s.overridden=hide;}v.invalidate();
        }catch(Throwable ignored){}finally{INTERNAL.remove();}
    }
}
