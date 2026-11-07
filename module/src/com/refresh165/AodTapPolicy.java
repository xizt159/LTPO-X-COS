package com.refresh165;
import android.content.Context;
import android.os.SystemClock;
import com.refresh165.xp.*;
/** Native reveal path first; wake only an already-visible AOD, never dismiss keyguard. */
final class AodTapPolicy {
    static boolean block(Object owner,Object event){
        if(!Config.aod("block_tap"))return false;
        Context c=AodAccess.context(owner);if(c==null||!AodAccess.asleep(c))return false;
        int showing=AodAccess.showing(c);if(showing<0)return false;
        long time=SystemClock.uptimeMillis();
        try{Object stamp=event instanceof android.view.MotionEvent?((android.view.MotionEvent)event).getEventTime():XposedHelpers.callMethod(event,"getTime");if(stamp instanceof Long&&(Long)stamp>0){long value=(Long)stamp;if(Math.abs(time-value)<5000)time=value;else if(Math.abs(System.currentTimeMillis()-value)<5000)time-=System.currentTimeMillis()-value;}}catch(Throwable ignored){}
        int action=AodTapSequence.tap(time,showing==1);if(action==AodTapSequence.BLOCK)return true;
        if(action==AodTapSequence.REVEAL)return showing==1;
        if(!AodAccess.pocketClear(c))return true;
        try{Object pm=c.getSystemService(Context.POWER_SERVICE);XposedHelpers.callMethod(pm,"wakeUp",SystemClock.uptimeMillis(),4,"LTPO-X-COS:AOD_NEXT_TAP");return true;}catch(Throwable ignored){return false;}
    }
}
