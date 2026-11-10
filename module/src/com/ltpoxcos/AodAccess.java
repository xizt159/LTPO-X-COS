package com.ltpoxcos;
import android.content.Context;
import com.ltpoxcos.xp.XposedHelpers;
import java.lang.reflect.*;
/** Reads native AOD state without rewriting its style or user-selected mode. */
final class AodAccess {
    static Context context(Object owner){
        if(owner instanceof android.view.View)return ((android.view.View)owner).getContext();
        for(Class<?> c=owner==null?null:owner.getClass();c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields())try{
            if(Context.class.isAssignableFrom(f.getType())){f.setAccessible(true);Object value=f.get(owner);if(value instanceof Context)return (Context)value;}
        }catch(Throwable ignored){}
        return Config.context();
    }
    static Object data(Context c){if(c==null)return null;try{return XposedHelpers.callStaticMethod(Class.forName("com.oplus.systemui.aod.aodclock.constant.AodData",false,c.getClassLoader()),"getInstance",c);}catch(Throwable ignored){return null;}}
    static boolean panoramic(Context c){Object d=data(c);try{return d!=null&&Boolean.TRUE.equals(XposedHelpers.callMethod(d,"isPanoramicAod"));}catch(Throwable ignored){return false;}}
    static int showing(Context c){Object d=data(c);try{if(d==null)return -1;Field f=d.getClass().getDeclaredField("mAodIsInShow");f.setAccessible(true);return f.getBoolean(d)?1:0;}catch(Throwable ignored){return -1;}}
    static boolean asleep(Context c){try{android.os.PowerManager pm=(android.os.PowerManager)c.getSystemService(Context.POWER_SERVICE);return pm!=null&&!pm.isInteractive();}catch(Throwable ignored){return false;}}
    static boolean pocketClear(Context c){try{
        Class<?> type=Class.forName("com.oplus.systemui.aod.display.OplusWakeUpController",false,c.getClassLoader());
        Field companion=type.getDeclaredField("Companion");companion.setAccessible(true);Object controller=XposedHelpers.callMethod(companion.get(null),"getInstance",c);
        Field upside=type.getDeclaredField("isUpsideDown"),near=type.getDeclaredField("proximityNear");upside.setAccessible(true);near.setAccessible(true);
        return !upside.getBoolean(controller)&&!near.getBoolean(controller);
    }catch(Throwable ignored){return false;}}
}
