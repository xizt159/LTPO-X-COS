package com.ltpoxcos;
import com.ltpoxcos.xp.*;
/** Firmware callback reasons: click=0, tilt=1, AMD motion=2. Never block unknown reasons. */
final class AodLiftPolicy {
 static boolean blocked(boolean enabled,int reason){return enabled&&(reason==1||reason==2);}
 static void install(ClassLoader cl){try{
  java.lang.reflect.Method m=Class.forName("com.oplus.systemui.aod.display.OplusWakeUpController",false,cl).getDeclaredMethod("notifyWakeUpCallback",int.class);
  if(m.getReturnType()!=void.class)return;
  XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){if(blocked(Config.aod("block_lift"),(Integer)p.args[0]))p.setResult(null);}});
 }catch(Throwable ignored){}}
}
