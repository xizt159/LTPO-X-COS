package com.ltpoxcos;
import java.util.*;
import java.util.regex.*;

/** Read-only SF evidence, matched to the foreground process. No titles retained. */
final class VideoFrameEvidence {
    private static final Pattern HEADER=Pattern.compile("(?m)^  Layer \\[(\\d+)\\] (.+)$");
    private static final Pattern FRAME=Pattern.compile("\\bframe=(\\d+)\\b"),PID=Pattern.compile("\\bownerPid=(\\d+)\\b");
    static final class Sample {
        final int video,ui;final long videoFrame,uiFrame;
        Sample(int v,int u,long vf,long uf){video=v;ui=u;videoFrame=vf;uiFrame=uf;}
    }
    static Sample parse(String dump,String pkg,int pid){
        if(dump==null||pkg==null||pkg.isEmpty()||pid<=0||dump.length()>1_500_000)return null;
        dump=dump.replace("\r","");
        Set<String> yuv=new HashSet<>();
        for(String line:dump.split("\n"))if(line.contains("layer:")&&line.contains("name:")&&line.contains("SurfaceView["+pkg+"/")
            &&line.matches(".*format:.*(Y_CBCR|YUV|YCrCb|YCbCr|NV12|NV21).*")){
            String name=line.substring(line.indexOf("name:")+5).trim();int end=name.indexOf(']');if(end>0)yuv.add(name.substring(0,end+1));
        }
        Map<Integer,Long> videos=new HashMap<>(),uis=new HashMap<>();Matcher h=HEADER.matcher(dump);
        while(h.find()){
            int id=Integer.parseInt(h.group(1));String name=h.group(2);int start=h.end();Matcher next=HEADER.matcher(dump);int end=next.find(start)?next.start():dump.length();
            String body=dump.substring(start,Math.min(end,start+8000));Matcher f=FRAME.matcher(body),p=PID.matcher(body);
            if(!body.contains("isVisible=true")||!f.find()||!p.find()||Integer.parseInt(p.group(1))!=pid)continue;
            long frame=Long.parseLong(f.group(1));Map<Integer,Long> selected=null;
            if(name.startsWith("VRI-"+pkg+"/"))selected=uis;
            int bracket=name.indexOf(']');
            if(bracket>0&&name.contains("SurfaceView["+pkg+"/")&&name.contains("(BLAST)#")&&yuv.contains(name.substring(0,bracket+1))
                &&body.contains("isOpaque=true")&&body.contains("blend=NONE (1) alpha=1.000000"))selected=videos;
            if(selected!=null){Long before=selected.put(id,frame);if(before!=null&&before.longValue()!=frame)return null;}
        }
        if(videos.size()!=1||uis.size()!=1)return null;
        int v=videos.keySet().iterator().next(),u=uis.keySet().iterator().next();return new Sample(v,u,videos.get(v),uis.get(u));
    }
    static boolean stablePlayback(Sample previous,Sample current,long elapsed){
        return stablePlayback(previous,current,elapsed,false);
    }
    static boolean stablePlayback(Sample previous,Sample current,long elapsed,boolean ambientUi){
        if(previous==null||current==null||elapsed<300||elapsed>2500||previous.video!=current.video||previous.ui!=current.ui
            ||current.uiFrame<previous.uiFrame||current.videoFrame<=previous.videoFrame)return false;
        double fps=(current.videoFrame-previous.videoFrame)*1000d/elapsed;
        // YouTube refreshes progress/ambient UI without user interaction.
        // Keep the independent pointer/held-finger gate; reject sustained UI animation.
        double uiFps=(current.uiFrame-previous.uiFrame)*1000d/elapsed;
        return fps>=2&&fps<=70&&uiFps<=(ambientUi?72d:6d);
    }
}
