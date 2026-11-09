package com.refresh165;

import android.content.Context;
import android.media.session.*;
import android.os.*;
import android.view.*;
import com.refresh165.xp.*;
import java.lang.reflect.Method;
import java.util.*;

/** Playback + visible advancing video + stable UI + input idle. Never package-only. */
final class VideoIdlePolicy {
    static final long QUIET_MS=2000,LEASE_MS=1800;
    private static volatile Handler main,io;private static Runnable changed;
    private static volatile String pkg="",reason="initializing";private static volatile int pid;
    private static volatile boolean full,enabled,inputHook,held,playing,active;
    private static volatile long lastInput,lease;
    private static final java.util.concurrent.atomic.AtomicLong GENERATION=new java.util.concurrent.atomic.AtomicLong();
    private static MediaSessionManager sessions;private static List<MediaController> controllers=Collections.emptyList();
    private static MediaController selected;private static MediaController.Callback callback;
    private static boolean pending;private static VideoFrameEvidence.Sample previous;private static long previousAt;
    private static volatile long snapshots,engaged;
    private static volatile double uiFps=-1,videoFps=-1;
    static void installInput(ClassLoader cl){
        try{
            Class<?> dispatcher=Class.forName("com.android.server.wm.PointerEventDispatcher",false,cl);
            Method method=dispatcher.getDeclaredMethod("onInputEvent",InputEvent.class);
            Method display=InputEvent.class.getDeclaredMethod("getDisplayId");display.setAccessible(true);
            inputHook=XposedBridge.hookMethod(method,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(!(p.args[0] instanceof MotionEvent))return;MotionEvent e=(MotionEvent)p.args[0];
                    try{if(!Integer.valueOf(0).equals(display.invoke(e)))return;}catch(Throwable ignored){inputHook=false;invalidate("input-display-unavailable");return;}
                    int action=e.getActionMasked();
                    if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_MOVE&&action!=MotionEvent.ACTION_UP
                        &&action!=MotionEvent.ACTION_CANCEL&&action!=MotionEvent.ACTION_SCROLL&&action!=MotionEvent.ACTION_POINTER_DOWN&&action!=MotionEvent.ACTION_POINTER_UP)return;
                    Idle144Policy.interaction();lastInput=SystemClock.elapsedRealtime();if(action==MotionEvent.ACTION_DOWN)held=true;
                    if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)held=false;
                    invalidate("interaction");Handler h=io;if(h!=null)h.post(()->schedule(QUIET_MS+20));
                }
            })!=null;
        }catch(Throwable ignored){reason="input-hook-unavailable";}
    }
    static synchronized void start(Context c,Handler handler,Runnable onChanged){
        if(io!=null)return;main=handler;changed=onChanged;HandlerThread t=new HandlerThread("Refresh165VideoEvidence");t.start();io=new Handler(t.getLooper()){
            @Override public void dispatchMessage(Message message){try{super.dispatchMessage(message);}catch(Throwable ignored){pending=false;playing=false;invalidate("observer-failed");}}
        };
        android.content.IntentFilter filter=new android.content.IntentFilter();filter.addAction(android.content.Intent.ACTION_BOOT_COMPLETED);filter.addAction(android.content.Intent.ACTION_SCREEN_ON);
        try{c.registerReceiver(new android.content.BroadcastReceiver(){@Override public void onReceive(Context context,android.content.Intent intent){io.post(()->initialize(c));}},filter,null,io,Context.RECEIVER_NOT_EXPORTED);}catch(Throwable ignored){}
        io.post(()->initialize(c));
    }
    private static boolean registered;
    private static void initialize(Context c){
        try{
            if(!registered){
                sessions=(MediaSessionManager)c.getSystemService(Context.MEDIA_SESSION_SERVICE);
                if(sessions==null)throw new IllegalStateException();
                sessions.addOnActiveSessionsChangedListener(list->{controllers=list==null?Collections.emptyList():list;select();},null,io);registered=true;
            }
            controllers=sessions.getActiveSessions(null);select();
        }catch(Throwable ignored){invalidate("media-observer-unavailable");}
    }

    static void scene(String next,int process,boolean fullscreen,boolean on){
        on=on&&Config.refresh("video");
        if(Objects.equals(pkg,next)&&pid==process&&full==fullscreen&&enabled==on)return;
        pkg=next;pid=process;full=fullscreen;enabled=on;invalidate("scene-change");
        Handler h=io;if(h!=null)h.post(()->{previous=null;select();if(sessionlessVideoTarget())schedule(0);});
    }
    static void screen(boolean on){if(!on){enabled=false;held=false;invalidate("screen-off");}}
    private static void select(){
        MediaController candidate=null;int count=0;
        for(MediaController c:controllers)if(pkg.equals(c.getPackageName())){candidate=c;count++;}
        if(count!=1)candidate=null;
        if(selected!=null&&candidate!=null&&selected.getSessionToken().equals(candidate.getSessionToken())){state(selected.getPlaybackState());return;}
        if(selected!=null&&callback!=null)try{selected.unregisterCallback(callback);}catch(Throwable ignored){}
        selected=candidate;previous=null;playing=false;invalidate("session-change");
        if(candidate==null){if(sessionlessVideoTarget())schedule(0);return;}
        final MediaController owner=candidate;
        callback=new MediaController.Callback(){
            @Override public void onPlaybackStateChanged(PlaybackState s){if(selected==owner)state(s);}
            @Override public void onSessionDestroyed(){if(selected==owner){selected=null;playing=false;invalidate("session-destroyed");}}
        };
        try{candidate.registerCallback(callback,io);state(candidate.getPlaybackState());}catch(Throwable ignored){playing=false;invalidate("session-unavailable");}
    }
    private static void state(PlaybackState s){
        boolean now=s!=null&&s.getState()==PlaybackState.STATE_PLAYING&&s.getPlaybackSpeed()>0;
        if(playing!=now){playing=now;previous=null;invalidate(now?"playback-start":"not-playing");}
        if(now||sessionlessVideoTarget())schedule(0);
    }
    private static boolean eligible(){return inputHook&&(playing||sessionlessVideoTarget())&&enabled&&full&&pid>0&&!pkg.isEmpty();}
    private static boolean sessionlessVideoTarget(){return "com.google.android.apps.photos".equals(pkg);}
    static boolean recentInteraction(){return held||SystemClock.elapsedRealtime()-lastInput<QUIET_MS;}
    static boolean playingVideo(){return enabled&&(playing||sessionlessVideoTarget()&&active);}
    static boolean pointerHeld(){return held;}
    static boolean watchingPackage(String name){return pkg.equals(name)&&watching();}
    static boolean watching(){return Config.refresh("video")&&active&&eligible()&&!recentInteraction()&&SystemClock.elapsedRealtime()<lease;}
    private static void schedule(long delay){if(!pending&&eligible()){pending=true;io.postDelayed(VideoIdlePolicy::sample,delay);}}
    private static void sample(){
        pending=false;if(!eligible()){previous=null;invalidate("ineligible");return;}
        if(recentInteraction()){previous=null;schedule(500);return;}
        final long token=GENERATION.get();final String owner=pkg;final int process=pid;
        try{
            VideoFrameEvidence.Sample current=VideoFrameEvidence.parse(SurfaceSnapshot.read(),owner,process);long now=SystemClock.elapsedRealtime();snapshots++;
            if(token!=GENERATION.get()||!eligible()||recentInteraction()||!owner.equals(pkg)||pid!=process){previous=null;schedule(1000);return;}
            long elapsed=now-previousAt;
            if(previous!=null&&current!=null&&elapsed>0&&previous.ui==current.ui&&previous.video==current.video){
                uiFps=(current.uiFrame-previous.uiFrame)*1000d/elapsed;
                videoFps=(current.videoFrame-previous.videoFrame)*1000d/elapsed;
            }else{uiFps=-1;videoFps=-1;}
            boolean good=VideoFrameEvidence.stablePlayback(previous,current,elapsed,!held&&now-lastInput>=4000);previous=current;previousAt=now;
            if(good){lease=now+LEASE_MS;boolean was=active;active=true;reason="verified-idle-playback";if(!was){engaged++;notifyChange();}
                final long deadline=lease;main.postDelayed(()->{if(lease==deadline&&SystemClock.elapsedRealtime()>=deadline){active=false;reason="evidence-expired";notifyChange();}},LEASE_MS+1);
            }else invalidate(current==null?"no-visible-video":"ui-moving-or-unconfirmed");
        }catch(Throwable ignored){previous=null;invalidate("evidence-unavailable");}
        schedule(1000);
    }
    private static void invalidate(String why){GENERATION.incrementAndGet();boolean was=active;active=false;lease=0;reason=why;if(was)notifyChange();}
    private static void notifyChange(){Handler h=main;Runnable r=changed;if(h!=null&&r!=null)h.post(r);}
    static String status(){return "input:"+inputHook+",playing:"+playing+",full:"+full+",watching:"+watching()+",snapshots:"+snapshots+",engaged:"+engaged+",uiFps:"+Math.round(uiFps*10d)/10d+",videoFps:"+Math.round(videoFps*10d)/10d+",reason:"+reason;}
}
