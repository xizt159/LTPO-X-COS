package com.refresh165;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.lang.reflect.*;
import java.util.*;
/** Native COUIPreference jump slot; popup, assignment and hit target stay owned by COUI. */
final class MenuIndicator {
 private static final Map<Object,Drawable> OLD=Collections.synchronizedMap(new WeakHashMap<>());
 static void apply(Object pref,Context c){try{
  synchronized(OLD){if(OLD.containsKey(pref))return;Field f=field(pref.getClass(),"mJumpRes");if(f==null)return;f.setAccessible(true);Drawable old=(Drawable)f.get(pref);jump(pref,new Chevrons(c));OLD.put(pref,old);}
 }catch(Throwable ignored){}}
 static void restore(Object pref){try{synchronized(OLD){if(OLD.containsKey(pref)){jump(pref,OLD.get(pref));OLD.remove(pref);}}}catch(Throwable ignored){}}
 private static Field field(Class<?> c,String n){while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException ignored){c=c.getSuperclass();}}return null;}
 private static void jump(Object o,Drawable d)throws Exception{o.getClass().getMethod("setJump",Drawable.class).invoke(o,d);}
 private static final class Chevrons extends Drawable {
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final int size;
  Chevrons(Context c){size=Math.round(20*c.getResources().getDisplayMetrics().density);boolean dark=(c.getResources().getConfiguration().uiMode&0x30)==0x20;p.setColor(dark?0xff888888:0xff777777);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
  @Override public int getIntrinsicWidth(){return size;}@Override public int getIntrinsicHeight(){return size;}
  @Override public void draw(Canvas c){Rect b=getBounds();int save=c.save();c.translate(b.left,b.top);c.scale(b.width()/20f,b.height()/20f);Path q=new Path();q.moveTo(5,7);q.lineTo(10,2);q.lineTo(15,7);q.moveTo(5,13);q.lineTo(10,18);q.lineTo(15,13);c.drawPath(q,p);c.restoreToCount(save);}
  @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
