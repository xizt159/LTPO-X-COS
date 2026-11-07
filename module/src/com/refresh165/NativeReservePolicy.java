package com.refresh165;

import com.refresh165.xp.*;
import java.lang.reflect.*;
import java.util.ArrayDeque;

/** Lower the verified165 reserve floor during the native serialization only.
 * This does not create panel timings, DSI commands, a55Hz cap, or an idle sequence. */
final class NativeReservePolicy {
    private static final ThreadLocal<ArrayDeque<Frame>> STACK=new ThreadLocal<>();
    private static volatile boolean installed;
    private static volatile Object lastOwner;
    private static volatile String lastArg;
    private static final AdfrVersion VERSION=new AdfrVersion();
    private static final ThreadLocal<ArrayDeque<java.util.List<Frame>>> NORMAL=new ThreadLocal<>();
    private static volatile long calls,applied,restored,failed;
    private static volatile String reason="not-called";
    private static final class Frame {Object config;Field field;int[] original,replacement;}
    static int[] replacement(int panel,int enabled,int mode,int[] original){
        if(panel!=0||enabled!=1)return null;
        return RateProfile.reserveIdleTable(mode,original);
    }
    static void install(ClassLoader cl){
        try{
            Class<?> helper=Class.forName("com.android.server.display.util.OplusDisplayPanelFeatureHelper",false,cl);
            XposedBridge.hookMethod(helper.getDeclaredMethod("setDisplayPanelFeatureValueArray",int.class,int[].class),new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){ArrayDeque<Frame> s=STACK.get();if(s!=null&&!s.isEmpty()&&Integer.valueOf(234).equals(p.args[0])&&p.args[1] instanceof int[])p.args[1]=VERSION.outgoing((int[])p.args[1]);}
            });
        }catch(Throwable ignored){}
        try{
            Class<?> rus=Class.forName("com.android.server.display.feature.panel.hwdpfeatures.OplusHwDisplayRusAdfr",false,cl);
            Method update=rus.getDeclaredMethod("updateAdfrXmlConfig",String.class);
            if(update.getReturnType()!=void.class)return;
            installed=XposedBridge.hookMethod(update,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    ArrayDeque<Frame> s=STACK.get();if(s==null){s=new ArrayDeque<>();STACK.set(s);}Frame f=new Frame();s.push(f);calls++;
                    lastOwner=p.thisObject;lastArg=(String)p.args[0];
                    ArrayDeque<java.util.List<Frame>> ns=NORMAL.get();if(ns==null){ns=new ArrayDeque<>();NORMAL.set(ns);}java.util.List<Frame> normal=new java.util.ArrayList<>();ns.push(normal);
                    try{
                        Object config=read(p.thisObject,"mAdfrXmlConfig");
                        if(!Config.refresh("tables")||AdaptivePolicy.holdFixedRate())return;
                        if(!Integer.valueOf(0).equals(read(config,"mPanelId"))||!Integer.valueOf(1).equals(read(config,"mEnable")))return;
                        for(int rate:new int[]{60,90,120})try{
                            String name="mMinfps"+rate+"level";int[] source=(int[])read(config,name),table=NativeTables.lower(rate,source);
                            if(table==null)continue;Frame nf=new Frame();nf.config=config;nf.field=config.getClass().getDeclaredField(name);nf.field.setAccessible(true);nf.original=source;nf.replacement=table;normal.add(nf);nf.field.set(config,table);
                        }catch(Throwable ignored){}
                        Object value=read(config,"mReserveMinfpslevel1");
                        if(!(value instanceof int[])){reason="unknown-table";return;}
                        int[] next=replacement((Integer)read(config,"mPanelId"),(Integer)read(config,"mEnable"),
                            (Integer)read(config,"mReserveMode"),(int[])value);
                        if(next==null){reason="unverified-config-preserved";return;}
                        Field field=config.getClass().getDeclaredField("mReserveMinfpslevel1");field.setAccessible(true);
                        f.config=config;f.field=field;f.original=(int[])value;f.replacement=next;
                        field.set(config,next);applied++;reason="idle-floor1-submitted-unverified";
                    }catch(Throwable ignored){failed++;reason="adapter-failed";}
                }
                @Override protected void afterHookedMethod(MethodHookParam p){
                    ArrayDeque<java.util.List<Frame>> ns=NORMAL.get();if(ns!=null&&!ns.isEmpty()){for(Frame nf:ns.pop())try{if(nf.field.get(nf.config)==nf.replacement)nf.field.set(nf.config,nf.original);}catch(Throwable ignored){}if(ns.isEmpty())NORMAL.remove();}
                    ArrayDeque<Frame> s=STACK.get();if(s==null||s.isEmpty())return;Frame f=s.pop();
                    try{
                        // Preserve an intervening vendor replacement; never overwrite it.
                        if(f.field!=null&&f.field.get(f.config)==f.replacement){f.field.set(f.config,f.original);restored++;}
                    }catch(Throwable ignored){failed++;reason="restore-failed";}
                    finally{if(s.isEmpty())STACK.remove();}
                }
            })!=null;
        }catch(Throwable ignored){reason="hook-unavailable";}
    }
    static void refresh(){Object owner=lastOwner;if(owner!=null)try{Object handler=read(owner,"mHandler");if(handler instanceof android.os.Handler)((android.os.Handler)handler).post(()->{try{XposedHelpers.callMethod(owner,"updateAdfrXmlConfig",lastArg);}catch(Throwable ignored){}});}catch(Throwable ignored){}}
    private static Object read(Object o,String name)throws Exception{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    static String status(){return "installed:"+installed+",calls:"+calls+",applied:"+applied+",restored:"+restored+",failed:"+failed+",reason:"+reason;}
}
