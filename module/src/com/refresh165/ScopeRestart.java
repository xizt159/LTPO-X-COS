package com.refresh165;
import java.util.concurrent.TimeUnit;
import java.io.*;
/** Only invoked by the visible UI button. No boot tasks or automatic root prompts. */
final class ScopeRestart {
    static final String COMMAND="for p in $(pidof com.oplus.aod com.android.systemui); do case \"$p\" in *[!0-9]*|'') ;; *) kill -TERM \"$p\" ;; esac; done";
    static boolean root()throws Exception{
        Process p=new ProcessBuilder("su","-c","id -u").redirectErrorStream(true).start();
        if(!p.waitFor(25,TimeUnit.SECONDS)){p.destroy();return false;}
        BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()));return p.exitValue()==0&&"0".equals(r.readLine());
    }
    static boolean restart()throws Exception{
        Process p=new ProcessBuilder("su","-c",COMMAND).redirectErrorStream(true).start();
        if(!p.waitFor(10,TimeUnit.SECONDS)){p.destroy();return false;}return p.exitValue()==0;
    }
}
