package com.ltpoxcos;
/** Hidden AOD: two taps reveal it. Visible AOD: a separate next tap wakes.
 * Duplicate delivery of the reveal event must not become that next tap. */
final class AodTapSequence {
    static final int BLOCK=0,REVEAL=1,WAKE=2;
    private static long first=-1,reveal=-10000,lastWake=-10000;
    static synchronized int tap(long now,boolean visible){
        if(now-lastWake<100)return BLOCK;
        if(now-reveal>=0&&now-reveal<70)return REVEAL;
        if(visible){first=-1;lastWake=now;return WAKE;}
        if(first>=0&&now-first<70)return BLOCK;
        if(first>=0&&now-first<=450){first=-1;reveal=now;return REVEAL;}
        first=now;return BLOCK;
    }
    static synchronized void clear(){first=-1;reveal=-10000;lastWake=-10000;}
}
