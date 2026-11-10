package com.ltpoxcos;
import java.util.regex.*;
final class RootFrame {
    final int id;final long frame;
    RootFrame(int i,long f){id=i;frame=f;}
    static RootFrame parse(String dump,String pkg,int pid){
        if(dump==null||pkg==null||pkg.isEmpty()||pid<=0||dump.length()>1500000)return null;
        dump=dump.replace("\r","");Matcher h=Pattern.compile("(?m)^  Layer \\[(\\d+)\\] (.+)$").matcher(dump);RootFrame found=null;
        while(h.find()){
            if(!h.group(2).startsWith("VRI-"+pkg+"/"))continue;
            int start=h.end(),end=dump.indexOf("\n  Layer [",start);String body=dump.substring(start,Math.min(end<0?dump.length():end,start+8000));
            Matcher p=Pattern.compile("\\bownerPid=(\\d+)\\b").matcher(body),f=Pattern.compile("\\bframe=(\\d+)\\b").matcher(body);
            if(!body.contains("isVisible=true")||!p.find()||Integer.parseInt(p.group(1))!=pid||!f.find())continue;
            RootFrame next=new RootFrame(Integer.parseInt(h.group(1)),Long.parseLong(f.group(1)));
            // SF prints the same layer in multiple sections. Identical records
            // are one source, while conflicting ids/counters remain ambiguous.
            if(found!=null){if(!same(found,next))return null;continue;}found=next;
        }return found;
    }
    static boolean same(RootFrame a,RootFrame b){return a!=null&&b!=null&&a.id==b.id&&a.frame==b.frame;}
}
