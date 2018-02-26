package com.creative.informatics.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Point;

import java.util.List;

/**
 * Created by K on 9/28/2017.
 */

public class RegionSelect extends View{

    private Paint mPaint;
    private Paint nPaint;
    private List<Point> corner;
    private Bitmap mBitmap;

    private double xscalefactor;
    private double yscalefactor;

    private RectF firstarea;
    private RectF secondarea;
    private RectF thirdarea;
    private RectF fourtharea;

    private final int NONE = -1, TOUCH_TOP_FIRST = 0, TOUCH_TOP_SECOND = 1, TOUCH_BOT_THIRD = 2, TOUCH_BOT_FOURTH = 3;
    int currentTouch = NONE;


    public RegionSelect(Context context, List<Point> points, Bitmap mImage) {
        super(context);

        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.polygonViewCircleBackground));

        nPaint = new Paint();
        nPaint.setColor(getResources().getColor(R.color.blue));
        nPaint.setStyle(Paint.Style.STROKE);
        nPaint.setStrokeWidth(5.0f);

        corner = points;
        mBitmap = mImage;

        Log.d("first",""+firstarea);
        Log.d("second",""+secondarea);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                Log.d("Down",""+event.getX());
                if (firstarea.contains(event.getX(), event.getY())) {
                    currentTouch = TOUCH_TOP_FIRST;
                } else if (secondarea.contains(event.getX(),event.getY())) {
                    currentTouch = TOUCH_TOP_SECOND;
                } else if (thirdarea.contains(event.getX(),event.getY())) {
                    currentTouch = TOUCH_BOT_THIRD;
                } else if (fourtharea.contains(event.getX(), event.getY())) {
                    currentTouch = TOUCH_BOT_FOURTH;
                } else {
                    return false; //Return false if user touches none of the corners
                }

                return true;
            case MotionEvent.ACTION_MOVE:
                switch (currentTouch) {
                    case TOUCH_TOP_FIRST:

                        corner.get(0).x = event.getX()/xscalefactor;
                        corner.get(0).y = event.getY()/yscalefactor;

                        invalidate();
                        return true;
                    case TOUCH_TOP_SECOND:

                        corner.get(1).x = event.getX()/xscalefactor;
                        corner.get(1).y = event.getY()/yscalefactor;

                        invalidate();
                        return true;
                    case TOUCH_BOT_THIRD:

                        corner.get(2).x = event.getX()/xscalefactor;
                        corner.get(2).y = event.getY()/yscalefactor;

                        invalidate();
                        return true;
                    case TOUCH_BOT_FOURTH:

                        corner.get(3).x = event.getX()/xscalefactor;
                        corner.get(3).y = event.getY()/yscalefactor;
                        invalidate();

                        return true;
                }
                //We returned true for all of the above cases, because we used the event
                return false;
            case MotionEvent.ACTION_UP:
                switch (currentTouch) {
                    case TOUCH_TOP_FIRST:

                        corner.get(0).x = event.getX()/xscalefactor;
                        corner.get(0).y = event.getY()/yscalefactor;
                        invalidate();
                        currentTouch = NONE;
                        return true;
                    case TOUCH_TOP_SECOND:

                        corner.get(1).x = event.getX()/xscalefactor;
                        corner.get(1).y = event.getY()/yscalefactor;
                        invalidate();
                        currentTouch = NONE;
                        return true;
                    case TOUCH_BOT_THIRD:

                        corner.get(2).x = event.getX()/xscalefactor;
                        corner.get(2).y = event.getY()/yscalefactor;
                        invalidate();
                        currentTouch = NONE;
                        return true;
                    case TOUCH_BOT_FOURTH:

                        corner.get(3).x = event.getX()/xscalefactor;
                        corner.get(3).y = event.getY()/yscalefactor;
                        invalidate();
                        currentTouch = NONE;
                        return true;
                }
                return false;
        }
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        double scalewid = canvas.getWidth();
        double scalehei = canvas.getHeight();

        xscalefactor = scalewid/mBitmap.getWidth();
        yscalefactor = scalehei/mBitmap.getHeight();

        float sizeOfRect = 50f;

        firstarea = new RectF( (float) (corner.get(0).x*xscalefactor) - sizeOfRect, (float) (corner.get(0).y*yscalefactor) - sizeOfRect, (float) (corner.get(0).x*xscalefactor)  + sizeOfRect,  (float) (corner.get(0).y*yscalefactor) + sizeOfRect);
        secondarea = new RectF( (float) (corner.get(1).x*xscalefactor) - sizeOfRect, (float) (corner.get(1).y*yscalefactor) - sizeOfRect, (float) (corner.get(1).x*xscalefactor)  + sizeOfRect,  (float) (corner.get(1).y*yscalefactor) + sizeOfRect);
        thirdarea = new RectF( (float) (corner.get(2).x*xscalefactor) - sizeOfRect, (float) (corner.get(2).y*yscalefactor) - sizeOfRect, (float) (corner.get(2).x*xscalefactor)  + sizeOfRect,  (float) (corner.get(2).y*yscalefactor) + sizeOfRect);
        fourtharea = new RectF( (float) (corner.get(3).x*xscalefactor) - sizeOfRect, (float) (corner.get(3).y*yscalefactor) - sizeOfRect, (float) (corner.get(3).x*xscalefactor)  + sizeOfRect,  (float) (corner.get(3).y*yscalefactor) + sizeOfRect);

        canvas.drawCircle((float) (corner.get(0).x*xscalefactor),(float) (corner.get(0).y*yscalefactor),40,mPaint);
        canvas.drawCircle((float) (corner.get(1).x*xscalefactor),(float) (corner.get(1).y*yscalefactor),40,mPaint);
        canvas.drawCircle((float) (corner.get(2).x*xscalefactor),(float) (corner.get(2).y*yscalefactor),40,mPaint);
        canvas.drawCircle((float) (corner.get(3).x*xscalefactor),(float) (corner.get(3).y*yscalefactor),40,mPaint);

        canvas.drawLine((float) (corner.get(0).x*xscalefactor),(float) (corner.get(0).y*yscalefactor),(float) (corner.get(1).x*xscalefactor),(float) (corner.get(1).y*yscalefactor),nPaint);
        canvas.drawLine((float) (corner.get(1).x*xscalefactor),(float) (corner.get(1).y*yscalefactor),(float) (corner.get(2).x*xscalefactor),(float) (corner.get(2).y*yscalefactor),nPaint);
        canvas.drawLine((float) (corner.get(2).x*xscalefactor),(float) (corner.get(2).y*yscalefactor),(float) (corner.get(3).x*xscalefactor),(float) (corner.get(3).y*yscalefactor),nPaint);
        canvas.drawLine((float) (corner.get(3).x*xscalefactor),(float) (corner.get(3).y*yscalefactor),(float) (corner.get(0).x*xscalefactor),(float) (corner.get(0).y*yscalefactor),nPaint);
    }

}
