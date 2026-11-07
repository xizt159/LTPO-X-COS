package com.refresh165;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Two circular arrows, following the toolbar foreground color. */
final class ToolbarIcon extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 ToolbarIcon(int color){paint.setColor(color);paint.setStrokeWidth(2);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
 @Override public void draw(Canvas c){android.graphics.Rect b=getBounds();int save=c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);c.drawArc(4,4,20,20,220,140,false,paint);c.drawArc(4,4,20,20,40,140,false,paint);Path p=new Path();p.moveTo(16,8);p.lineTo(20,12);p.lineTo(23,8);p.moveTo(1,16);p.lineTo(4,12);p.lineTo(8,16);c.drawPath(p,paint);c.restoreToCount(save);}
 @Override public void setAlpha(int n){paint.setAlpha(n);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
