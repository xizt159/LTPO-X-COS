package com.ltpoxcos;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.WindowManager;
import com.ltpoxcos.xp.*;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;

/**165 interaction, validatedIME60, and verified idle-playback ceiling60. */
final class AdaptivePolicy {
    private static volatile Object director;
    private static volatile Context context;
    private static Handler worker;
    private static volatile boolean voteHook,sceneHook,focusHook,ready,ime,interactive;
    private static volatile int cap;
    private static String appliedAppPolicy="";
    private static boolean nativeFixedHeld;
    private static int physicalUserPriority=-1;
    // Scene identity is retained only to keep the proven WM snapshot path unchanged.
    private static volatile String pkg="",reason="initializing",fault="none";
    private static volatile int pid;
    private static volatile boolean sceneFull,rawIme;
    private static volatile long voteReads,policyChanges,sceneReads;
    private static volatile float resolvedPhysicalMax=-1f,resolvedRenderMax=-1f;
    private static Method physicalVote,renderVote,notifySpecs;
    private static Constructor<?> combined;
    private static int priority,baselinePriority;
    private static volatile boolean specsDeoptimized;
    private static final java.util.concurrent.atomic.AtomicReference<Scene> PENDING=new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicBoolean SCENE_POSTED=new java.util.concurrent.atomic.AtomicBoolean();
    private static final ImeLatch IME=new ImeLatch();
    private static boolean imeHidePending;
    private static volatile String imeSource="unknown";
    private static final class Scene {
        final String pkg;final int pid;final boolean ime,full;
        Scene(String p,int process,boolean shown){this(p,process,shown,false);}
        Scene(String p,int process,boolean shown,boolean fullscreen){pkg=p;pid=process;ime=shown;full=fullscreen;}
    }
    static void install(ClassLoader cl) {
        android.os.HandlerThread thread=new android.os.HandlerThread("LTPO-X-COSAdaptive");
        thread.start(); worker=new Handler(thread.getLooper()){
            @Override public void dispatchMessage(android.os.Message message){
                try{super.dispatchMessage(message);}
                catch(Throwable t){
                    // A failed optional adapter must never terminate system_server.
                    fault="worker-disabled-"+t.getClass().getSimpleName();ready=false;recompute();
                }
            }
        };
        try {
            Class<?> d=Class.forName("com.android.server.display.mode.DisplayModeDirector",false,cl);
            Class<?> v=Class.forName("com.android.server.display.mode.Vote",false,cl);
            Class<?> storage=Class.forName("com.android.server.display.mode.VotesStorage",false,cl);
            Class<?> combo=Class.forName("com.android.server.display.mode.CombinedVote",false,cl);
            priority=integer(v,"PRIORITY_FLICKER_REFRESH_RATE_SWITCH");
            baselinePriority=integer(v,"PRIORITY_USER_SETTING_PEAK_RENDER_FRAME_RATE");
            try{physicalUserPriority=integer(v,"PRIORITY_USER_SETTING_PEAK_REFRESH_RATE");}catch(Throwable ignored){}
            int thermal=integer(v,"PRIORITY_SKIN_TEMPERATURE");
            int app=integer(v,"PRIORITY_APP_REQUEST_BASE_MODE_REFRESH_RATE");
            if(!(app<baselinePriority && baselinePriority<priority && priority<thermal))throw new IllegalStateException("priority-order");
            physicalVote=v.getDeclaredMethod("forPhysicalRefreshRates",float.class,float.class);
            renderVote=v.getDeclaredMethod("forRenderFrameRates",float.class,float.class);
            physicalVote.setAccessible(true);renderVote.setAccessible(true);
            combined=combo.getDeclaredConstructor(List.class);combined.setAccessible(true);
            notifySpecs=d.getDeclaredMethod("notifyDesiredDisplayModeSpecsChangedLocked");
            notifySpecs.setAccessible(true);
            Method getSpecs=d.getDeclaredMethod("getDesiredDisplayModeSpecs",int.class);
            Method votes=storage.getDeclaredMethod("getVotes",int.class);
            if(!SparseArray.class.isAssignableFrom(votes.getReturnType()))throw new IllegalStateException("votes-type");
            XC_MethodHook.Unhook a=XposedBridge.hookMethod(getSpecs,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!Integer.valueOf(0).equals(p.args[0]))return;
                    director=p.thisObject;
                    Object c=field(director,"mContext");
                    if(c instanceof Context && context==null){context=(Context)c;worker.post(AdaptivePolicy::initialize);}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(!Integer.valueOf(0).equals(p.args[0]) || p.getThrowable()!=null)return;
                    Object ranges=field(p.getResult(),"primary");
                    Object physical=field(field(ranges,"physical"),"max");
                    Object render=field(field(ranges,"render"),"max");
                    resolvedPhysicalMax=physical instanceof Number?((Number)physical).floatValue():-1f;
                    resolvedRenderMax=render instanceof Number?((Number)render).floatValue():-1f;
                    if(cap>0 && resolvedRenderMax>cap+0.1f)reason="ceiling-yielded-to-system-or-unsupported";
                }
            });
            XC_MethodHook.Unhook b=XposedBridge.hookMethod(votes,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    int limit=cap;
                    if(limit>0&&!ime&&VideoIdlePolicy.recentInteraction())limit=Config.effectiveRate();
                    if(!Config.on("refresh") || !ready || limit==0 || !Integer.valueOf(0).equals(p.args[0])
                            || p.getThrowable()!=null || !(p.getResult() instanceof SparseArray))return;
                    // Only the captured director's storage; never another display/director.
                    if(p.thisObject!=field(director,"mVotesStorage"))return;
                    try {
                        SparseArray<Object> copy=((SparseArray<Object>)p.getResult()).clone();
                        if(Config.explicitProfile()){
                            // Replace only user peak votes in our returned copy, never thermal/FOD or native storage.
                            copy.remove(baselinePriority);
                            if(physicalUserPriority>=0&&physicalUserPriority<baselinePriority)copy.remove(physicalUserPriority);
                        }
                        addCeiling(copy,baselinePriority,Config.effectiveRate(),Config.effectiveRate());
                        if(Config.fixed()&&interactive&&!activeRequest()){
                            List<Object> fixed=new ArrayList<>();Object old=copy.get(baselinePriority);if(old!=null)fixed.add(old);
                            fixed.add(physicalVote.invoke(null,(float)Config.effectiveRate(),(float)Config.effectiveRate()+0.01f));
                            fixed.add(renderVote.invoke(null,(float)Config.effectiveRate(),(float)Config.effectiveRate()+0.01f));copy.put(baselinePriority,combined.newInstance(fixed));
                        }
                        if(limit<Config.effectiveRate()||activeRequest()||BatteryDisplayPolicy.capEnabled()){
                            addCeiling(copy,priority,limit,limit);
                        }
                        p.setResult(copy);voteReads++;
                    }catch(Throwable t){fault="vote-"+t.getClass().getSimpleName();}
                }
            });
            voteHook=a!=null && b!=null;
            if(voteHook)specsDeoptimized=XposedBridge.deoptimize(getSpecs);
            if(!voteHook){if(a!=null)a.unhook();if(b!=null)b.unhook();}
        }catch(Throwable t){fault="vote-adapter-"+t.getClass().getSimpleName();}
        try {
            Class<?> dc=Class.forName("com.android.server.wm.DisplayContent",false,cl);
            XC_MethodHook capture=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){if(p.getThrowable()==null)captureScene(p.thisObject);}};
            Method traversal=dc.getDeclaredMethod("applySurfaceChangesTransaction");
            sceneHook=XposedBridge.hookMethod(traversal,capture)!=null;
            if(sceneHook)XposedBridge.deoptimize(traversal);
            // Ordinary app focus changes may finish without a later WM surface traversal.
            // Read after the verified ColorOS17 focus method has committed mCurrentFocus.
            try{Method focus=dc.getDeclaredMethod("updateFocusedWindowLocked",int.class,boolean.class,int.class);
                if(focus.getReturnType()==boolean.class){focusHook=XposedBridge.hookMethod(focus,capture)!=null;if(focusHook)XposedBridge.deoptimize(focus);}
                for(String owner:new String[]{"com.android.server.wm.RootWindowContainer","com.android.server.wm.WindowManagerService"})try{
                    XposedBridge.deoptimize(Class.forName(owner,false,cl).getDeclaredMethod("updateFocusedWindowLocked",int.class,boolean.class));
                }catch(Throwable ignored){}
            }catch(Throwable ignored){}

        }catch(Throwable t){fault="scene-adapter-"+t.getClass().getSimpleName();}
        VideoIdlePolicy.installInput(cl);
        // Diagnostics are query-only and never include media titles/URIs.
        try {
            Class<?> d=Class.forName("com.android.server.display.mode.DisplayModeDirector",false,cl);
            XposedBridge.hookMethod(d.getDeclaredMethod("dump",PrintWriter.class),new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    ((PrintWriter)p.args[0]).println(status());
                }
            });
        }catch(Throwable ignored){}
    }
    private static void captureScene(Object display){sceneReads++;
                    try {
                        if(!Integer.valueOf(0).equals(XposedHelpers.callMethod(display,"getDisplayId")))return;
                        Object focus=field(display,"mCurrentFocus");
                        Object keyboard=field(display,"mInputMethodWindow");
                        Object attr=field(keyboard,"mAttrs");
                        boolean shown;
                        try {
                            Object controller=XposedHelpers.callMethod(display,"getInsetsStateController");
                            Object provider=XposedHelpers.callMethod(controller,"getImeSourceProvider");
                            shown=Boolean.TRUE.equals(XposedHelpers.callMethod(provider,"isImeShowing"));
                            imeSource="insets-provider";
                        }catch(Throwable unsupported){
                            shown=attr instanceof WindowManager.LayoutParams
                                && ((WindowManager.LayoutParams)attr).type==WindowManager.LayoutParams.TYPE_INPUT_METHOD
                                && Boolean.TRUE.equals(XposedHelpers.callMethod(keyboard,"isVisible"))
                                && Boolean.TRUE.equals(XposedHelpers.callMethod(keyboard,"isDisplayed"));
                            imeSource="window-fallback";
                        }
                        Object focusAttrs=field(focus,"mAttrs");
                        if(focus==null || (focusAttrs instanceof WindowManager.LayoutParams
                            && (((WindowManager.LayoutParams)focusAttrs).type==WindowManager.LayoutParams.TYPE_INPUT_METHOD
                                || ((WindowManager.LayoutParams)focusAttrs).type==WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG))){
                            enqueue(new Scene(pkg,pid,shown,sceneFull));return;
                        }
                        Object owner=focus==null?null:XposedHelpers.callMethod(focus,"getOwningPackage");
                        Object session=field(focus,"mSession"), process=field(session,"mPid");
                        boolean full=false;try{Object task=XposedHelpers.callMethod(focus,"getTask");full=Boolean.FALSE.equals(XposedHelpers.callMethod(task,"inMultiWindowMode"));}catch(Throwable ignored){}
                        enqueue(new Scene(owner instanceof String?(String)owner:"",
                            process instanceof Number?((Number)process).intValue():0,shown,full));
                    }catch(Throwable t){fault="scene-"+t.getClass().getSimpleName();}
    }
    private static void initialize(){
        if(context==null||powerRegistered)return;
        ready=voteHook&&(sceneHook||focusHook);
        Config.listen(()->worker.post(()->{Hook.configurationChanged(context);NativeIdlePolicy.refresh();NativeReservePolicy.refresh();AnimationPolicy.refresh();cap=-1;recompute();}));
        Config.init(context);
        Idle144Policy.start(worker,AdaptivePolicy::recompute);
        VideoIdlePolicy.start(context,worker,AdaptivePolicy::recompute);
        android.content.IntentFilter f=new android.content.IntentFilter();
        f.addAction(android.content.Intent.ACTION_SCREEN_OFF);f.addAction(android.content.Intent.ACTION_SCREEN_ON);
        f.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        context.registerReceiver(new android.content.BroadcastReceiver(){
            @Override public void onReceive(Context c,android.content.Intent i){worker.post(()->{
                BatteryDisplayPolicy.updateBatterySaver(isPowerSaveMode());
                interactive=isInteractive();if(!interactive){IME.clear();ime=false;rawIme=false;}VideoIdlePolicy.screen(interactive);recompute();
            });}
        },f,null,worker,Context.RECEIVER_NOT_EXPORTED);
        BatteryDisplayPolicy.updateBatterySaver(isPowerSaveMode());
        powerRegistered=true;interactive=isInteractive();recompute();
    }
    private static boolean powerRegistered;
    private static void enqueue(Scene s){
        PENDING.set(s);if(!SCENE_POSTED.compareAndSet(false,true))return;
        worker.post(new Runnable(){@Override public void run(){
            try{Scene next=PENDING.getAndSet(null);if(next!=null)scene(next);}
            finally{SCENE_POSTED.set(false);if(PENDING.get()!=null&&SCENE_POSTED.compareAndSet(false,true))worker.post(this);}
        }});
    }
    private static void scene(Scene s){
        rawIme=Config.refresh("ime")&&s.ime;ime=IME.observe(rawIme,SystemClock.elapsedRealtime());pkg=s.pkg;pid=s.pid;Config.scene(pkg,s.full);interactive=isInteractive();
        sceneFull=s.full;
        long deadline=IME.deadline();
        if(deadline>=0&&!imeHidePending){imeHidePending=true;worker.postDelayed(AdaptivePolicy::settleIme,Math.max(1,deadline-SystemClock.elapsedRealtime()));}
        recompute();
    }
    private static void settleIme(){
        long now=SystemClock.elapsedRealtime();ime=Config.refresh("ime")&&IME.value(now);recompute();long deadline=IME.deadline();
        if(deadline>=0)worker.postDelayed(AdaptivePolicy::settleIme,Math.max(1,deadline-now));else imeHidePending=false;
    }
    private static void addCeiling(SparseArray<Object> copy,int slot,float physical,float render)throws Exception{
        List<Object> all=new ArrayList<>();Object old=copy.get(slot);if(old!=null)all.add(old);
        all.add(physicalVote.invoke(null,0f,physical+0.01f));all.add(renderVote.invoke(null,0f,render+0.01f));
        copy.put(slot,combined.newInstance(all));
    }
    private static void recompute(){
        VideoIdlePolicy.scene(pkg,pid,sceneFull,interactive&&!ime&&!rawIme);
        if(!Config.refresh("ime")){IME.clear();ime=false;rawIme=false;}
        Idle144Policy.scene(pkg,pid,sceneFull,interactive&&!ime&&!rawIme&&!VideoIdlePolicy.playingVideo());
        int next=ready&&Config.on("refresh")?(interactive?Math.min(Config.effectiveRate(),ime||VideoIdlePolicy.watching()?60:Idle144Policy.active()?120:Config.effectiveRate()):0):0;
        String policy=pkg+":"+Config.effectiveRate()+":"+Config.fixed()+":"+Config.explicitProfile()+":"+BatteryDisplayPolicy.capEnabled();
        boolean held=holdFixedRate();if(held!=nativeFixedHeld){nativeFixedHeld=held;NativeIdlePolicy.refresh();NativeReservePolicy.refresh();}
        if(next==cap&&policy.equals(appliedAppPolicy))return;appliedAppPolicy=policy;cap=next;policyChanges++;
        Hook.refreshVideoPolicy();
        reason=ime?"ime-ceiling60":VideoIdlePolicy.watching()?"watching-idle-ceiling60-native30":interactive?"native-content-under"+Config.effectiveRate():"screen-off-native";
        Object d=director,lock=field(d,"mLock");if(lock==null)return;
        try{synchronized(lock){notifySpecs.invoke(d);}}catch(Throwable t){fault="notify-"+t.getClass().getSimpleName();}
    }
    private static boolean isInteractive(){
        PowerManager pm=context==null?null:(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return pm!=null&&pm.isInteractive();
    }
    private static boolean isPowerSaveMode(){
        PowerManager pm=context==null?null:(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        return pm!=null&&pm.isPowerSaveMode();
    }
    static boolean holdFixedRate(){return ready&&interactive&&Config.fixed()&&!activeRequest();}
    static boolean activeRequest(){return interactive&&(ime||VideoIdlePolicy.watching()||Idle144Policy.active());}
    static boolean coldAndUnrestricted(){
        try{
            PowerManager p=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            return p!=null&&!p.isPowerSaveMode()&&p.getCurrentThermalStatus()==PowerManager.THERMAL_STATUS_NONE;
        }catch(Throwable ignored){return false;}
    }
    static String status(){return "LTPO-X (COS) Beta 4 (20009): voteHook="+voteHook+" sceneHook="+sceneHook+" ready="+ready
        +" focusHook="+focusHook+" sceneReads="+sceneReads+" scenePackage="+pkg+" sceneFull="+sceneFull+" config="+Config.debugStatus()+" cap="+cap+" ime="+ime+" rawIme="+rawIme+" imeShowDelayMs=300"+" imeSource="+imeSource+" policyChanges="+policyChanges+" voteReads="+voteReads
        +" profile="+Config.effectiveRate()+" baselinePriority="+baselinePriority+" specsDeoptimized="+specsDeoptimized
        +" resolvedPhysicalMax="+resolvedPhysicalMax+" resolvedRenderMax="+resolvedRenderMax
        +" animation="+AnimationPolicy.status()+" idle144="+Idle144Policy.status()+" video="+VideoIdlePolicy.status()+","+Hook.videoCandidateStatus()+" refreshCap="+BatteryDisplayPolicy.status()+" reserve="+NativeReservePolicy.status()+" lowIdle="+Hook.lowBrightnessStatus()+" reason="+reason+" fault="+fault;}
    private static int integer(Class<?>c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.getInt(null);}
    private static Object field(Object o,String name){
        if(o==null)return null;for(Class<?>c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
        catch(NoSuchFieldException ignored){}catch(Throwable ignored){return null;}return null;
    }
}
