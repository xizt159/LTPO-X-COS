package com.ltpoxcos;
import java.util.Arrays;
/** Exact stock ColorOS17 normal table shape; unknown updates are preserved. */
final class NativeTables {
    static int[] lower(int rate,int[] source){
        if(rate!=60&&rate!=90&&rate!=120)return null;
        int[] known={10,1,1,30,1,rate,1,rate,10,rate,10,0,0};
        if(!Arrays.equals(source,known))return null;
        return new int[]{10,1,1,1,1,1,1,1,1,1,1,0,0};
    }
}
