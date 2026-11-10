package com.ltpoxcos;
import java.lang.reflect.*;
import java.nio.file.*;
import android.os.*;
import android.view.*;
import android.media.session.*;
import com.ltpoxcos.xp.XposedBridge;

public class VideoPolicyTest {
    static int checks;
    static void yes(boolean b){checks++;if(!b)throw new AssertionError("video check "+checks);}
    static void set(Class<?> c,String n,Object v)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    static Object get(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(null);}
    static Object call(String n,Class<?>[] types,Object...args)throws Exception{Method m=VideoIdlePolicy.class.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(null,args);}
    static void sample()throws Exception{call("sample",new Class<?>[0]);}
    static void arm()throws Exception{
        set(VideoIdlePolicy.class,"pkg","com.google.android.youtube");set(VideoIdlePolicy.class,"pid",42);
        set(VideoIdlePolicy.class,"full",true);set(VideoIdlePolicy.class,"enabled",true);set(VideoIdlePolicy.class,"playing",true);
        set(VideoIdlePolicy.class,"held",false);set(VideoIdlePolicy.class,"lastInput",SystemClock.now-10000L);
        set(VideoIdlePolicy.class,"active",true);set(VideoIdlePolicy.class,"lease",SystemClock.now+1800);
    }
    public static void main(String[]args)throws Exception{
        String fixture=Files.readString(Path.of(args[0]));
        VideoFrameEvidence.Sample a=VideoFrameEvidence.parse(fixture,"com.google.android.youtube",42);
        yes(a!=null);yes(a.video==306&&a.ui==293);yes(a.videoFrame==5540&&a.uiFrame==1062);
        yes(VideoFrameEvidence.parse(fixture,"other.app",42)==null);
        yes(VideoFrameEvidence.parse(fixture,"com.google.android.youtube",99)==null);
        yes(VideoFrameEvidence.parse("Permission denied","com.google.android.youtube",42)==null);
        yes(VideoFrameEvidence.parse(fixture.replace("isOpaque=true","isOpaque=false"),"com.google.android.youtube",42)==null);
        String photosFixture=fixture.replace("com.google.android.youtube","com.google.android.apps.photos");
        VideoFrameEvidence.Sample photos=VideoFrameEvidence.parse(photosFixture,"com.google.android.apps.photos",42);
        yes(photos!=null&&photos.videoFrame==5540);
        VideoFrameEvidence.Sample b=VideoFrameEvidence.parse(fixture.replace("frame=5540","frame=5570"),"com.google.android.youtube",42);
        yes(VideoFrameEvidence.stablePlayback(a,b,1000));
        yes(VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5570,1065),1000));
        yes(VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5570,1068),1000));
        yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5570,1069),1000));
        yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5570,1061),1000));
        yes(!VideoFrameEvidence.stablePlayback(a,a,1000));
        yes(!VideoFrameEvidence.stablePlayback(a,b,100));yes(!VideoFrameEvidence.stablePlayback(a,b,3000));
        yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5570,1074),1000));
        yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(306,293,5705,1062),1000));
        yes(!VideoFrameEvidence.stablePlayback(a,new VideoFrameEvidence.Sample(307,293,5570,1062),1000));
        yes(!VideoFrameEvidence.stablePlayback(null,b,1000));

        yes(VideoFrameEvidence.stablePlayback(new VideoFrameEvidence.Sample(498,488,2371,692),new VideoFrameEvidence.Sample(498,488,2405,696),1135));
        yes(VideoFrameEvidence.stablePlayback(new VideoFrameEvidence.Sample(498,488,2405,696),new VideoFrameEvidence.Sample(498,488,2439,699),1114));
        yes(VideoFrameEvidence.stablePlayback(new VideoFrameEvidence.Sample(498,488,2439,699),new VideoFrameEvidence.Sample(498,488,2473,702),1163));
        yes(VideoFrameEvidence.stablePlayback(new VideoFrameEvidence.Sample(498,488,2473,702),new VideoFrameEvidence.Sample(498,488,2508,707),1161));

        Handler main=new Handler(),io=new Handler();set(VideoIdlePolicy.class,"main",main);set(VideoIdlePolicy.class,"io",io);
        final int[] notifications={0};set(VideoIdlePolicy.class,"changed",(Runnable)()->notifications[0]++);
        VideoIdlePolicy.installInput(VideoPolicyTest.class.getClassLoader());yes((Boolean)get(VideoIdlePolicy.class,"inputHook"));
        arm();set(VideoIdlePolicy.class,"playing",false);yes(!(Boolean)call("eligible",new Class<?>[0]));
        set(VideoIdlePolicy.class,"pkg","com.google.android.apps.photos");yes((Boolean)call("eligible",new Class<?>[0]));
        SystemClock.now=10000;set(VideoIdlePolicy.class,"active",false);set(VideoIdlePolicy.class,"previous",null);set(VideoIdlePolicy.class,"pending",false);
        SurfaceSnapshot.dump=photosFixture;sample();yes(!VideoIdlePolicy.watching());
        SystemClock.now=11000;SurfaceSnapshot.dump=photosFixture.replace("frame=5540","frame=5570").replace("frame=1062","frame=1065");
        sample();yes(VideoIdlePolicy.watching());yes(VideoIdlePolicy.playingVideo());
        SystemClock.now=10000;arm();set(VideoIdlePolicy.class,"active",false);SurfaceSnapshot.dump=fixture;sample();yes(!VideoIdlePolicy.watching());
        SystemClock.now=11000;SurfaceSnapshot.dump=fixture.replace("frame=5540","frame=5570").replace("frame=1062","frame=1065");sample();yes(VideoIdlePolicy.watching());
        yes(VideoIdlePolicy.watchingPackage("com.google.android.youtube"));yes(!VideoIdlePolicy.watchingPackage("other.app"));
        set(VideoIdlePolicy.class,"full",false);yes(!VideoIdlePolicy.watching());set(VideoIdlePolicy.class,"full",true);
        set(VideoIdlePolicy.class,"playing",false);yes(!VideoIdlePolicy.watching());set(VideoIdlePolicy.class,"playing",true);
        SystemClock.now=13000;yes(!VideoIdlePolicy.watching());

        arm();Method input=com.android.server.wm.PointerEventDispatcher.class.getDeclaredMethod("onInputEvent",InputEvent.class);
        Object dispatcher=new com.android.server.wm.PointerEventDispatcher();
        XposedBridge.call(input,dispatcher,new MotionEvent(MotionEvent.ACTION_DOWN));yes(!VideoIdlePolicy.watching());yes(VideoIdlePolicy.recentInteraction());
        SystemClock.now+=5000;yes(VideoIdlePolicy.recentInteraction()); // A finger held still must not be capped.
        XposedBridge.call(input,dispatcher,new MotionEvent(MotionEvent.ACTION_UP));yes(VideoIdlePolicy.recentInteraction());
        SystemClock.now+=2001;yes(!VideoIdlePolicy.recentInteraction());
        arm();call("state",new Class<?>[]{PlaybackState.class},new PlaybackState(2));yes(!VideoIdlePolicy.watching());
        arm();VideoIdlePolicy.scene("other.app",99,true,true);yes(!VideoIdlePolicy.watching());
        arm();VideoIdlePolicy.screen(false);yes(!VideoIdlePolicy.watching());
        arm();set(VideoIdlePolicy.class,"previous",a);set(VideoIdlePolicy.class,"previousAt",SystemClock.now-1000);
        set(VideoIdlePolicy.class,"lastInput",SystemClock.now-2500L);SurfaceSnapshot.dump=fixture.replace("frame=5540","frame=5570").replace("frame=1062","frame=1074");sample();yes(!VideoIdlePolicy.watching());
        arm();set(VideoIdlePolicy.class,"previous",a);set(VideoIdlePolicy.class,"previousAt",SystemClock.now-1000);
        SurfaceSnapshot.dump=fixture.replace("frame=5540","frame=5570");
        SurfaceSnapshot.duringRead=()->{try{XposedBridge.call(input,dispatcher,new MotionEvent(MotionEvent.ACTION_DOWN));}catch(Exception e){throw new RuntimeException(e);}};
        sample();yes(!VideoIdlePolicy.watching());SurfaceSnapshot.duringRead=null;

        Method install=Hook.class.getDeclaredMethod("hookDefaultWindowRate",ClassLoader.class);install.setAccessible(true);install.invoke(null,VideoPolicyTest.class.getClassLoader());
        com.android.server.wm.OplusRefreshRatePolicyImpl policy=new com.android.server.wm.OplusRefreshRatePolicyImpl();
        Method candidate=policy.getClass().getDeclaredMethod("getWindowPreferredModeId",int.class);
        arm();for(int request:new int[]{1,3,4,7})yes((Integer)XposedBridge.call(candidate,policy,request)==2);
        yes((Integer)XposedBridge.call(candidate,policy,2)==2);yes((Integer)XposedBridge.call(candidate,policy,6)==6);
        policy.mPickRefreshRateData.name="com.android.systemui";yes((Integer)XposedBridge.call(candidate,policy,7)==7);
        policy.mPickRefreshRateData.name="com.google.android.youtube";set(VideoIdlePolicy.class,"playing",false);yes((Integer)XposedBridge.call(candidate,policy,7)==7);
        arm();XposedBridge.call(input,dispatcher,new MotionEvent(MotionEvent.ACTION_DOWN));yes((Integer)XposedBridge.call(candidate,policy,7)==7);
        Hook.refreshVideoPolicy();yes(policy.refreshed>0);
        // The actual adaptive policy must cap only verified watching, and release on input.
        set(AdaptivePolicy.class,"worker",main);set(AdaptivePolicy.class,"context",new android.content.Context());
        set(AdaptivePolicy.class,"ready",true);set(AdaptivePolicy.class,"interactive",true);set(AdaptivePolicy.class,"ime",false);
        set(AdaptivePolicy.class,"pkg","com.google.android.youtube");set(AdaptivePolicy.class,"pid",42);set(AdaptivePolicy.class,"sceneFull",true);
        Method recompute=AdaptivePolicy.class.getDeclaredMethod("recompute");recompute.setAccessible(true);
        set(VideoIdlePolicy.class,"changed",(Runnable)()->{try{recompute.invoke(null);}catch(Exception e){throw new RuntimeException(e);}});
        arm();recompute.invoke(null);yes((Integer)get(AdaptivePolicy.class,"cap")==60);
        XposedBridge.call(input,dispatcher,new MotionEvent(MotionEvent.ACTION_DOWN));main.until(SystemClock.now);yes((Integer)get(AdaptivePolicy.class,"cap")==165);
        arm();recompute.invoke(null);call("state",new Class<?>[]{PlaybackState.class},new PlaybackState(2));main.until(SystemClock.now);yes((Integer)get(AdaptivePolicy.class,"cap")==165);
        arm();set(AdaptivePolicy.class,"rawIme",true);recompute.invoke(null);yes((Integer)get(AdaptivePolicy.class,"cap")==165);yes(!VideoIdlePolicy.watching());
        set(AdaptivePolicy.class,"rawIme",false);
        arm();set(AdaptivePolicy.class,"ime",true);recompute.invoke(null);yes((Integer)get(AdaptivePolicy.class,"cap")==60);yes(!VideoIdlePolicy.watching());
        set(AdaptivePolicy.class,"ime",false);recompute.invoke(null);yes((Integer)get(AdaptivePolicy.class,"cap")==165);
        arm();set(AdaptivePolicy.class,"sceneFull",false);recompute.invoke(null);yes((Integer)get(AdaptivePolicy.class,"cap")==165);
        System.out.println("PASS "+checks+" real-fixture/playback/input/lease/race/vendor-candidate assertions");
    }
}
