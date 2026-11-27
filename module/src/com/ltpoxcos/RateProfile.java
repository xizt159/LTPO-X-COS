package com.ltpoxcos;

import java.util.Map;

/** Runtime OEM rate IDs are distinct from physical Display.Mode IDs. */
final class RateProfile {
    static final int HIGH=165;
    interface Lookup {float rate(int id)throws Throwable;}
    static int rateId(int requestedRate,Map<Integer,Integer> ids,int fallbackId){
        Integer exact=ids.get(requestedRate);if(exact!=null)return exact;
        int bestRate=-1,bestId=-1;
        for(Map.Entry<Integer,Integer> entry:ids.entrySet()){
            if(entry.getKey()<=requestedRate&&entry.getKey()>bestRate){bestRate=entry.getKey();bestId=entry.getValue();}
        }
        return bestId>=0?bestId:fallbackId;
    }
    static int highRateId(Lookup source){
        int bestRate=-1,found=-1;
        boolean ambiguous=false;
        for(int id=1;id<=16;id++)try{
            float rate=source.rate(id);
            int known=Math.abs(rate-HIGH)<0.1f?HIGH:Math.abs(rate-144f)<0.1f?144:-1;
            if(known>bestRate){bestRate=known;found=id;ambiguous=false;}
            else if(known==bestRate&&known>0)ambiguous=true;
        }catch(Throwable ignored){}
        return ambiguous?-1:found;
    }
    static int[] reserveIdleTable(int reserveMode,int[] original){
        // Only the stock165Hz schema is supported.
        if(reserveMode!=165||original==null||original.length!=13)return null;
        if(original[0]!=10||original[1]!=33||original[2]!=33||original[11]!=0||original[12]!=0)return null;
        int peak=original[3];if(peak!=165)return null;
        for(int i=3;i<=10;i++)if(original[i]!=(i%2==1?peak:33))return null;
        return new int[]{10,1,1,1,1,1,1,1,1,1,1,0,0};
    }
}
