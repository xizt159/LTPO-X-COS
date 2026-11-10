package com.ltpoxcos;

import com.ltpoxcos.xp.*;
import java.lang.reflect.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Ordinary animations40/90 and observed native launcher85 use165. FOD120 is untouched.
 * Native40 remains below thermal80. Native90 is promoted only after an explicit
 * thermal80 clear/unrestricted vote has actually been observed.
 */
final class AnimationPolicy {
    private static final ThreadLocal<ArrayDeque<Integer>> SCOPE=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<>();
    private static volatile boolean requestHook, thermalHook, thermalKnown;
    private static volatile float thermalMax=Float.NaN;
    private static volatile long promoted40,promoted85,promoted90,guarded90,restored;
    private static volatile Object promotedCore, thermalCore;
    private static volatile boolean restoreRequested;
    private static final Map<Integer,Object> originals=new HashMap<>();
    private static Method updateVote;
    private static void push(int priority){
        ArrayDeque<Integer> s=SCOPE.get();if(s==null){s=new ArrayDeque<>();SCOPE.set(s);}s.push(priority);
    }
    private static void pop(){ArrayDeque<Integer>s=SCOPE.get();if(s!=null&&!s.isEmpty())s.pop();if(s==null||s.isEmpty())SCOPE.remove();}
    private static boolean inside(){ArrayDeque<Integer>s=SCOPE.get();return s!=null&&!s.isEmpty()&&s.peek()!=0;}
    private static boolean insideLow(){ArrayDeque<Integer>s=SCOPE.get();return s!=null&&!s.isEmpty()&&s.peek()==40;}
    private static boolean allowed(int priority){
        if(priority==40||priority==90)Idle144Policy.interaction();
        if(!Config.refresh("animation")||AdaptivePolicy.activeRequest())return false;
        if(priority==40)return true;
        boolean yes=thermalHook&&thermalKnown&&thermalMax>=Config.effectiveRate()&&AdaptivePolicy.coldAndUnrestricted();
        if(!yes)guarded90++;return yes;
    }
    static void install(ClassLoader cl){
        try{
            Class<?> core=Class.forName("com.android.server.wm.OplusRefreshRateCore",false,cl);
            Class<?> vote=Class.forName("com.android.server.wm.OplusRefreshRateCore$Vote",false,cl);
            Method factory=vote.getDeclaredMethod("forRefreshRate",float.class,float.class);factory.setAccessible(true);
            updateVote=core.getDeclaredMethod("updateVoteLocked",int.class,vote);updateVote.setAccessible(true);
            thermalHook=XposedBridge.hookMethod(updateVote,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if(Boolean.TRUE.equals(INTERNAL.get()))return;
                    if(restoreRequested&&promotedCore==p.thisObject){
                        try{INTERNAL.set(true);for(Map.Entry<Integer,Object> e:originals.entrySet())updateVote.invoke(p.thisObject,e.getKey(),e.getValue());}
                        catch(Throwable ignored){}finally{INTERNAL.remove();originals.clear();promotedCore=null;restoreRequested=false;}
                    }
                    int priority=(Integer)p.args[0];
                    if(priority==80){
                        thermalCore=p.thisObject;
                        Object rate=field(p.args[1],"mRefreshRate");
                        thermalKnown=p.args[1]==null||rate instanceof Number;
                        thermalMax=p.args[1]==null?Float.POSITIVE_INFINITY:rate instanceof Number?((Number)rate).floatValue():Float.NaN;
                        if((!thermalKnown||thermalMax<Config.effectiveRate())&&promotedCore==p.thisObject){
                            try{INTERNAL.set(true);for(Map.Entry<Integer,Object> entry:originals.entrySet()){
                                updateVote.invoke(p.thisObject,entry.getKey(),entry.getValue());restored++;
                            }}catch(Throwable ignored){}finally{INTERNAL.remove();originals.clear();promotedCore=null;}
                        }
                    }
                    if(priority==85||priority==90){
                        if(promotedCore!=null&&promotedCore!=p.thisObject)return;
                        originals.remove(priority);
                        Object nativeVote=p.args[1],rate=field(nativeVote,"mRefreshRate"),source=field(nativeVote,"mRequestSource");
                        boolean launcher=priority==85&&source instanceof String&&((String)source).matches("com\\.android\\.launcher[0-9]+");
                        if(rate instanceof Number&&Math.abs(((Number)rate).floatValue()-120f)<0.1f&&(launcher||priority==90&&inside())){
                            // Read the actual current vote map, including a clear80 vote,
                            // rather than waiting for a future thermal callback.
                            try{Object votes=field(p.thisObject,"mVotes");if(votes!=null){
                                Object heat=XposedHelpers.callMethod(votes,"get",80),value=field(heat,"mRefreshRate");
                                thermalCore=p.thisObject;thermalKnown=heat==null||value instanceof Number;
                                thermalMax=heat==null?Float.POSITIVE_INFINITY:value instanceof Number?((Number)value).floatValue():Float.NaN;
                            }}catch(Throwable ignored){thermalKnown=false;}
                            if(thermalCore==p.thisObject&&allowed(priority))try{
                                Object replacement=factory.invoke(null,(float)Config.effectiveRate(),(float)Config.effectiveRate());
                                if(source!=null){Field f=vote.getDeclaredField("mRequestSource");f.setAccessible(true);f.set(replacement,source);}
                                p.args[1]=replacement;originals.put(priority,nativeVote);promotedCore=p.thisObject;
                                if(priority==85)promoted85++;else promoted90++;
                            }catch(Throwable ignored){}
                        }
                    }
                }
            })!=null;
        }catch(Throwable ignored){}
        try{
            Class<?> policy=Class.forName("com.android.server.wm.OplusRefreshRatePolicyImpl",false,cl);
            Class<?> utils=Class.forName("com.android.server.wm.OplusRefreshRateUtils",false,cl);
            for(Method method:policy.getDeclaredMethods()){
                if(method.getName().equals("requestRefreshRate")&&method.getParameterCount()==4
                    &&method.getParameterTypes()[0]==int.class&&method.getParameterTypes()[2]==float.class){
                    boolean hooked=XposedBridge.hookMethod(method,new XC_MethodHook(){
                        @Override protected void beforeHookedMethod(MethodHookParam p){
                            int priority=(Integer)p.args[0];boolean eligible=false;
                            if((priority==40||priority==90) && p.args[2] instanceof Float
                                && Math.abs((Float)p.args[2]-120f)<0.1f && allowed(priority)){
                                try{Object source=XposedHelpers.callStaticMethod(utils,"getRequestSource",field(p.thisObject,"mContext"));
                                    eligible=source instanceof String&&((String)source).matches("com\\.android\\.launcher[0-9]+");}
                                catch(Throwable ignored){}
                            }
                            push(eligible?priority:0);
                            // The low-priority request can be promoted directly. Priority90
                            // is promoted at core update so its original can be restored on heat.
                            if(eligible&&priority==40){p.args[2]=(float)Config.effectiveRate();promoted40++;}
                        }
                        @Override protected void afterHookedMethod(MethodHookParam p){pop();}
                    })!=null;
                    requestHook|=hooked;
                }
                if(method.getName().equals("startAppSwitchBoost")&&method.getParameterCount()==1){
                    XposedBridge.hookMethod(method,new XC_MethodHook(){
                        @Override protected void beforeHookedMethod(MethodHookParam p){
                            boolean yes=false;
                            if(allowed(90))try{yes=Boolean.FALSE.equals(XposedHelpers.callMethod(p.thisObject,"isActivityDisableBoostForHome",p.args[0]));}
                            catch(Throwable ignored){}push(yes?90:0);
                        }
                        @Override protected void afterHookedMethod(MethodHookParam p){pop();}
                    });
                }
            }
            // Restore only helper mappings scoped by native ordinary animation requests.
            XposedBridge.hookAllMethods(policy,"getLauncherRateId",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(insideLow() && p.getThrowable()==null && Integer.valueOf(3).equals(p.getResult())
                        &&p.args.length==1&&(Integer.valueOf(3).equals(p.args[0])||Integer.valueOf(7).equals(p.args[0])||Integer.valueOf(Hook.highRateId()).equals(p.args[0])))p.setResult(Hook.highRateId());
                }
            });
            XposedBridge.hookAllMethods(policy,"adjustRefreshForDefaultMax",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(insideLow() && p.getThrowable()==null && Integer.valueOf(3).equals(p.getResult())
                        &&p.args.length==2&&"limit appSwitchBoost".equals(p.args[1])
                        &&(Integer.valueOf(3).equals(p.args[0])||Integer.valueOf(7).equals(p.args[0])||Integer.valueOf(Hook.highRateId()).equals(p.args[0])))p.setResult(Hook.highRateId());
                }
            });
        }catch(Throwable ignored){}
    }
    // Restore inside the next native vote callback, under its owning WM lock.
    static void refresh(){restoreRequested=true;}
    static String status(){return "requestHook:"+requestHook+",thermalHook:"+thermalHook+",thermalKnown:"+thermalKnown
        +",thermalMax:"+thermalMax+",promoted40:"+promoted40+",promoted85:"+promoted85+",promoted90:"+promoted90+",guarded90:"+guarded90+",restored:"+restored;}
    private static Object field(Object o,String name){
        if(o==null)return null;
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
        catch(NoSuchFieldException ignored){}catch(Throwable ignored){return null;}return null;
    }
}
