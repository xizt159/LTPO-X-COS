package com.ltpoxcos;
/** Android17 float brightness is normalized; -1/NaN/off must retain native meaning. */
final class AodValues {
    static float scale(float nativeValue,int percent){
        if(!Float.isFinite(nativeValue)||nativeValue<=0f||nativeValue>1f)return nativeValue;
        return Math.min(1f,nativeValue*Math.max(25,Math.min(300,percent))/100f);
    }
    static float fixed(float nativeValue,int level){if(!Float.isFinite(nativeValue)||nativeValue<=0f||nativeValue>1f)return nativeValue;return (Math.max(2,Math.min(255,level))-1)/254f;}
    static int fixedInitial(int nativeValue,int level){return nativeValue<=0||nativeValue>255?nativeValue:Math.max(2,Math.min(255,level));}
    static int initial(int nativeValue,int dark,int bright){
        if(nativeValue<0||nativeValue>255)return nativeValue;
        return Math.max(1,Math.min(255,nativeValue<40?dark:bright));
    }
}
