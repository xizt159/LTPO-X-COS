package com.refresh165;
import com.refresh165.xp.*;
public final class Exp4Test {
 static int checks;static void yes(boolean b){checks++;if(!b)throw new AssertionError("exp4 "+checks);}
 static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{return XposedBridge.call(o.getClass().getDeclaredMethod(n,types),o,args);}
 public static void main(String[] args)throws Exception{
  for(int reason:new int[]{0,3,99,-1})yes(!AodLiftPolicy.blocked(true,reason));yes(AodLiftPolicy.blocked(true,1));yes(AodLiftPolicy.blocked(true,2));yes(!AodLiftPolicy.blocked(false,1));
  AodPolicy.install(Exp4Test.class.getClassLoader(),"com.android.systemui");Config.flags.put("aod",true);Config.flags.put("block_lift",true);
  com.oplus.systemui.aod.display.OplusWakeUpController controller=com.oplus.systemui.aod.display.OplusWakeUpController.INSTANCE;
  for(int reason:new int[]{1,2})call(controller,"notifyWakeUpCallback",new Class<?>[]{int.class},reason);yes(controller.motionCalls==0);
  for(int reason:new int[]{0,99})call(controller,"notifyWakeUpCallback",new Class<?>[]{int.class},reason);yes(controller.motionCalls==2);
  Config.flags.put("block_lift",false);call(controller,"notifyWakeUpCallback",new Class<?>[]{int.class},1);yes(controller.motionCalls==3);
  Config.flags.put("block_lift",true);Config.flags.put("aod",false);call(controller,"notifyWakeUpCallback",new Class<?>[]{int.class},2);yes(controller.motionCalls==4);
  for(float v:new float[]{-1f,0f,2f,Float.POSITIVE_INFINITY})yes(AodValues.fixed(v,80)==v);yes(Float.isNaN(AodValues.fixed(Float.NaN,80)));
  yes(AodValues.fixed(.1f,80)==AodValues.fixed(.9f,80));yes(AodValues.fixed(.2f,255)==1f);yes(AodValues.fixed(.2f,1)>0f);
  yes(AodValues.fixedInitial(0,80)==0);yes(AodValues.fixedInitial(-1,80)==-1);yes(AodValues.fixedInitial(256,80)==256);
  Config.flags.put("aod",true);Config.flags.put("fixed_brightness",true);Config.flags.put("running_brightness",true);
  com.android.systemui.doze.DozeService service=new com.android.systemui.doze.DozeService();
  for(float v:new float[]{.1f,.8f}){call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},v);yes(Math.abs(service.sent-79f/254)<.00001f);}
  call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},0f);yes(service.sent==0f);
  com.oplus.systemui.aod.OplusDozeServiceExImpl ex=new com.oplus.systemui.aod.OplusDozeServiceExImpl();yes((Integer)call(ex,"setBrightnessBeforeDozing",new Class<?>[0])==80);
  Config.flags.put("fixed_brightness",false);call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},.25f);yes(Math.abs(service.sent-.4f)<.0001f);
  Config.flags.put("aod",false);call(service,"setDozeScreenBrightness",new Class<?>[]{float.class},.25f);yes(service.sent==.25f);
  Config.flags.put("aod",true);Config.flags.put("battery_only",true);Config.flags.put("panoramic",false);com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl bar=new com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl();
  yes(!(Boolean)call(bar,"hookDozingState",new Class<?>[]{boolean.class},true));yes((Boolean)call(bar,"hookDozingState",new Class<?>[]{boolean.class},false));
  Config.flags.put("battery_only",false);yes((Boolean)call(bar,"hookDozingState",new Class<?>[]{boolean.class},true));
  System.out.println("PASS "+checks+" exp4 motion-only gate / fixed brightness priority and sentinels / battery-only status scope assertions");
 }
}
