package com.ltpoxcos;
import android.os.*;
import java.util.concurrent.atomic.AtomicLong;

/** Use the real stock120 timing only after verified static foreground content.
 * Does not relabel a120 table as144 or create missing panel commands. */
final class Idle144Policy {
    private static final AtomicLong GEN=new AtomicLong();
    private static volatile long interactionAt=SystemClock.elapsedRealtime();
    private static volatile boolean idle,enabled;
    private static volatile String pkg="";private static volatile int pid;
    private static Handler io,main;private static Runnable changed;
    private static boolean pending;private static RootFrame previous;
    private static volatile long samples,parsed;private static volatile String reason="initializing";
    static synchronized void start(Handler owner,Runnable callback){if(io!=null)return;main=owner;changed=callback;HandlerThread t=new HandlerThread("Idle144Evidence");t.start();io=new Handler(t.getLooper());}
    static boolean active(){return idle&&enabled&&!VideoIdlePolicy.pointerHeld()&&!Config.fixed()&&Config.refresh("gate144")&&Config.effectiveRate()==144&&SystemClock.elapsedRealtime()-interactionAt>=1200;}
    static boolean owner(String name){return active()&&pkg.equals(name);}
    static void interaction(){interactionAt=SystemClock.elapsedRealtime();GEN.incrementAndGet();if(idle){idle=false;notifyChange();}}
    static void scene(String name,int process,boolean fullTask,boolean on){
        boolean allow=on&&fullTask&&!Config.fixed()&&Config.effectiveRate()==144&&Config.refresh("gate144")&&process>0&&!name.isEmpty();
        if(enabled==allow&&pid==process&&pkg.equals(name)){if(allow&&io!=null)io.post(Idle144Policy::schedule);return;}
        pkg=name;pid=process;enabled=allow;GEN.incrementAndGet();if(idle){idle=false;notifyChange();}
        Handler h=io;if(h!=null)h.post(()->{previous=null;schedule();});
    }
    private static void schedule(){Handler h=io;if(h!=null&&!pending&&enabled){pending=true;h.postDelayed(Idle144Policy::sample,500);}}
    private static void sample(){pending=false;if(!enabled){previous=null;return;}
        long token=GEN.get();
        try{
            if(VideoIdlePolicy.pointerHeld()||SystemClock.elapsedRealtime()-interactionAt<1200){previous=null;schedule();return;}
            RootFrame now=RootFrame.parse(SurfaceSnapshot.read(),pkg,pid);samples++;if(now!=null)parsed++;
            boolean stable=RootFrame.same(previous,now);previous=now;
            reason=now==null?"no-unique-visible-root":stable?"static-root-request120":"root-changing-or-first-sample";
            if(token==GEN.get()&&enabled&&!VideoIdlePolicy.playingVideo()){
                if(idle!=stable){idle=stable;notifyChange();}
            }
        }catch(Throwable ignored){reason="snapshot-unavailable";previous=null;if(idle){idle=false;notifyChange();}}
        schedule();
    }
    private static void notifyChange(){Handler h=main;Runnable r=changed;if(h!=null&&r!=null)h.post(r);}
    static String status(){return "enabled:"+enabled+",idle120:"+active()+",samples:"+samples+",parsed:"+parsed+",reason:"+reason;}
}
