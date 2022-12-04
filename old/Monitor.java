package me.antonio.noack.thedollargame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

public class Monitor extends AppCompatImageView {

    private float w, h, dotSize;
    private float zoom = .3f, px, py;
    private Paint linePaint, dotPaint, textPaint, background;
    private RectF rect;

    public Monitor(Context context){
        super(context);
        init();
    }

    public Monitor(Context context, AttributeSet set){
        super(context, set);
        init();
    }

    public Monitor(Context context, AttributeSet set, int a){
        super(context, set, a);
        init();
    }

    float sq(float d){
        return d*d;
    }

    float sq(float d, float e){
        return d*d+e*e;
    }

    private abstract class Handler {

        float fx, fy;

        Handler(float fx, float fy){
            this.fx = fx;
            this.fy = fy;
        }

        abstract void move(float x, float y, float dx, float dy, long timeSum);

        abstract void exit(float x, float y, float dx, float dy, long timeSum);
    }

    private int len = 32;
    private long[] startTime = new long[len];
    private float[] lastX = new float[len], lastY = new float[len];
    private Handler[] handlers = new Handler[len];
    private float moveSize;

    private boolean two;

    private boolean hasMoveHandler = false;

    private Dot touched = null;

    public void init(){

        final ZoomingListener listener = new ZoomingListener() {
            @Override public void onTouchMove(float x, float y, float dx, float dy, boolean notFirst) {}
            @Override public void onRotZoomTwice(View v, boolean notFirst, float da, float a, float s, float ls, float x, float x2, float y, float y2, float lx, float lx2, float ly, float ly2) {
                if(notFirst){
                    // berechne, wo der Zoom ist...
                    // dann lege ihn an
                    zoom *= s/ls;
                    px += (x-lx + x2-lx2)/2 / zoom;
                    py += (y-ly + y2-ly2)/2 / zoom;
                    invalidate();
                }
            }

            @Override public void onClick(float x, float y, long durationMillis) {}
        };

        setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {

                try {
                    float thatX, thatY, deltaX, deltaY;

                    int index = event.getActionIndex();

                    if(index >= len) return true;

                    long currentTime = System.currentTimeMillis();
                    long timeSum = currentTime - startTime[index];

                    thatX = event.getX(index) / w * 2 - 1;
                    thatY = event.getY(index) / h * 2 - 1;

                    deltaX = thatX - lastX[index];
                    deltaY = thatY - lastY[index];

                    touched = getNodeAt(thatX, thatY);

                    switch(event.getActionMasked()){
                        case MotionEvent.ACTION_POINTER_DOWN:
                        // case MotionEvent.ACTION_POINTER_1_DOWN:
                        case MotionEvent.ACTION_POINTER_2_DOWN:
                        case MotionEvent.ACTION_POINTER_3_DOWN:
                            if(event.getPointerCount() > 1) two = true;
                            break;
                        case MotionEvent.ACTION_DOWN:// 1st

                            two = false;
                            // case MotionEvent.ACTION_POINTER_DOWN:// 2nd... 100th

                            startTime[index] = currentTime;
                            handlers[index] = new Handler(thatX, thatY) {

                                boolean good;

                                @Override void move(float x, float y, float dx, float dy, long timeSum) {
                                    if(good || sq(x-fx, y-fy) > .03){
                                        if(good){
                                            px += dx * .5f / zoom;
                                            py += dy * .5f / zoom;
                                        } else {
                                            px += (x-fx) * .5f / zoom;
                                            py += (y-fy) * .5f / zoom;
                                            good = true;
                                        }

                                        invalidate();
                                    }
                                }

                                @Override void exit(float x, float y, float dx, float dy, long timeSum) {
                                    if(!good){
                                        onClick(x, y, timeSum);
                                    }// else move
                                }
                            };

                            lastX[index] = thatX;
                            lastY[index] = thatY;

                            break;
                        case MotionEvent.ACTION_MOVE:

                            if(!two){

                                deltaX = thatX - lastX[0];
                                deltaY = thatY - lastY[0];

                                if(touched != null && all.shuffleMode){

                                    // berechne die Position und platziere dort den Punkt
                                    touched.x += fieldX((1+thatX+deltaX)*.5f*w) - fieldX((1+thatX)*.5f*w);
                                    touched.y += fieldY((1+thatY+deltaY)*.5f*h) - fieldY((1+thatY)*.5f*h);

                                    invalidate();

                                } else if(abs(deltaX) < .2 && abs(deltaY) < .2 && handlers[0] != null){
                                    handlers[0].move(thatX, thatY, deltaX, deltaY, timeSum);
                                }

                                lastX[0] = thatX;
                                lastY[0] = thatY;

                            } break;
                        case MotionEvent.ACTION_UP:
                            int ctr=0;for(Handler handler:handlers){
                            if(!two && handler != null) handler.exit(lastX[ctr], lastY[ctr], 0, 0, timeSum);ctr++;
                        } break;
                        case MotionEvent.ACTION_POINTER_UP:
                            if(!two && handlers[index] != null) handlers[index].exit(thatX, thatY, deltaX, deltaY, timeSum);
                            break;
                    }

                    listener.onTouch(v, event);
                } catch (final Exception e){
                    all.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(getContext(), e.getMessage()+", please message me, e.g. via G+ (Antonio Noack)", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return true;// consumed
            }
        });

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xff000000);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth(10);
        dotPaint = new Paint();
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xffff0000);
        dotPaint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(0xff000000);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        background = new Paint();
        background.setColor(getResources().getColor(R.color.colorPrimary));
        background.setStyle(Paint.Style.FILL);

        rect = new RectF();
    }

    private Dot getNodeAt(float x, float y){

        System.out.println("q "+x+" "+y);

        x = (1+x)*.5f*w;
        y = (1+y)*.5f*h;

        Dot best = null;
        float min = sq(dotSize + 30);
        for(Dot dot:net.dots){
            float dist = sq(screenX(dot.x) - x) + sq(screenY(dot.y) - y);
            if(dist < min){
                best = dot;
                min = dist;
            }
        }

        return best;
    }

    private void onClick(float x, float y, long durationMillis){
        if(net != null && durationMillis < 1800){

            if(!all.shuffleMode/* || selected == null*/){

                Dot best = getNodeAt(x, y);
                if(best != null) {

                    if(all.shuffleMode){

                        //selected = best;

                        //invalidate();

                    } else {
                        boolean share = durationMillis < 300;

                        int l = best.edges();
                        best.value += share ? -l : l;

                        for (int i = 0; i < l; i++) {
                            best.get(i).value += share ? 1 : -1;
                        }

                        for (Dot dot : net.dots) {
                            if (dot.value < 0) {
                                invalidate();
                                return;
                            }
                        }

                        all.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                all.finished();
                            }
                        });

                        invalidate();
                    }
                }
            } else {

                //x = (1+x)*.5f*w;
                //y = (1+y)*.5f*h;

                // berechne die Position und platziere dort den Punkt
                //selected.x = fieldX(x);
                //selected.y = fieldY(y);
                //selected = null;
                //invalidate();
            }
        }
    }

    AllManager all;
    public void setAll(AllManager all){
        this.all = all;
    }

    public float screenX(float dx){
        return w/2 + (dx+px)*w*zoom;
    }

    public float screenY(float dy){
        return h/2 + (dy+py)*w*zoom;
    }

    public float fieldX(float x){
        return (x - w/2)/(w*zoom)-px;
    }

    public float fieldY(float y){
        return (y-h/2)/(w*zoom)-py;
    }

    private Net net;

    public void setNet(Net net){
        this.net = net;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {

        w = getMeasuredWidth();
        h = getMeasuredHeight();

        canvas.drawRect(0, 0, w, h, background);

        // if(net == null) net = new Net(100, 20, 100);
        if(net != null){

            dotSize = zoom * w * .4f / (float) sqrt(Math.max(net.dots.length - 4, 1));

            // draw the connecting line
            for(Dot a:net.dots){
                if(a.connected != null) for(int i=0,l=a.edges();i<l;i++){
                    Dot b = a.get(i);
                    // todo color dependent on the money difference
                    if(b.compareTo(a) < 0) canvas.drawLine(screenX(a.x), screenY(a.y), screenX(b.x), screenY(b.y), linePaint);
                }
            }

            for(Dot dot:net.dots){
                // draw the dot
                float x = screenX(dot.x), y = screenY(dot.y);
                rect.left = x-dotSize/2;
                rect.top = y-dotSize/2;
                rect.right = x+dotSize/2;
                rect.bottom = y+dotSize/2;
                dotPaint.setColor(/*all.shuffleMode && dot == selected ? 0xff3d75f3 : */dot.value < 0 ? 0xffff0000 : dot.value == 0 ? -1 : 0xff00ff00);
                canvas.drawArc(rect, 0, 360, true, dotPaint);
                textPaint.setTextSize(dotSize * .5f);
                canvas.drawText(dot.value+"", x, y + dotSize * .18f, textPaint);
            }
        }

    }
}
