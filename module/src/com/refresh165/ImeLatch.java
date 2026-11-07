package com.refresh165;
/** Delay the opening cap; debounce brief hidden samples after activation. */
final class ImeLatch {
    static final long SHOW_DELAY_MS=300,HIDE_GRACE_MS=350;
    private boolean shown;private long showAt=-1,hideAt=-1;
    boolean observe(boolean visible,long now){
        value(now);
        if(visible){hideAt=-1;if(!shown&&showAt<0)showAt=now+SHOW_DELAY_MS;}
        else if(!shown){showAt=-1;}else if(hideAt<0)hideAt=now+HIDE_GRACE_MS;
        return value(now);
    }
    boolean value(long now){
        if(showAt>=0&&now>=showAt){shown=true;showAt=-1;}
        if(hideAt>=0&&now>=hideAt){shown=false;hideAt=-1;}
        return shown;
    }
    long deadline(){return showAt>=0?showAt:hideAt;}
    void clear(){shown=false;showAt=hideAt=-1;}
}
