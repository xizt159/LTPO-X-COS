package com.ltpoxcos;
import java.util.Arrays;
/** Native 225-word feature234 schema: version at index1, panel at13. */
final class AdfrVersion {
    private int[] last;private int version;
    synchronized int[] outgoing(int[] source){
        if(source==null||source.length!=225||source[0]!=1||source[13]!=0||source[1]<=0)return source;
        int[] fingerprint=source.clone();fingerprint[1]=0;
        boolean changed=last!=null&&!Arrays.equals(last,fingerprint);
        long candidate=Math.max((long)source[1],changed?(long)version+1:version);
        if(candidate>Integer.MAX_VALUE)return source;
        version=(int)candidate;last=fingerprint;
        int[] result=source.clone();result[1]=version;return result;
    }
}
