package com.ltpoxcos;

/** Runtime OEM rate IDs are distinct from physical Display.Mode IDs. */
final class RateProfile {
    static final int HIGH=165;
    interface Lookup {float rate(int id)throws Throwable;}
    static int highRateId(Lookup source){
        try{if(Math.abs(source.rate(7)-HIGH)<0.1f)return 7;}catch(Throwable ignored){}
        int found=-1;
        for(int id=1;id<=16;id++)try{
            if(Math.abs(source.rate(id)-HIGH)<0.1f){if(found>=0)return -1;found=id;}
        }catch(Throwable ignored){}
        return found;
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
