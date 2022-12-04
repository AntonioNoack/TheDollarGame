package me.antonio.noack.thedollargame;

import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class Monitor2 extends AppCompatImageView {

    private float w, h, dotSize;
    private float zoom = .3f;
    private float cx, cy;
    private Paint linePaint, dotPaint, textPaint, background;
    private RectF rect;

    public Monitor2(Context context) {
        super(context);
        init();
    }

    public Monitor2(Context context, AttributeSet set) {
        super(context, set);
        init();
    }

    public Monitor2(Context context, AttributeSet set, int a) {
        super(context, set, a);
        init();
    }

    float sq(float d) {
        return d * d;
    }

    private Dot touched = null;

    @SuppressLint("ClickableViewAccessibility")
    public void init() {

        final GestureDetector listener1 = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            long startTime = 0L;
            final float longTime = 300 * 1000L * 1000L;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float effectiveZoom = zoom * w;
                if (all.shuffleMode && touched != null) {
                    // done move the point...
                    touched.x -= distanceX / effectiveZoom;
                    touched.y -= distanceY / effectiveZoom;
                } else {
                    // done move the screen
                    cx -= distanceX / effectiveZoom;
                    cy -= distanceY / effectiveZoom;
                }
                invalidate();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                if (all.shuffleMode) {
                    touched = getNodeOnScreen(e.getX(), e.getY());
                } else touched = null;
                startTime = System.nanoTime();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onClick(e.getX(), e.getY(), (System.nanoTime() - startTime) > longTime);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                onClick(e.getX(), e.getY(), (System.nanoTime() - startTime) > longTime);
            }

        });

        final ScaleGestureDetector listener2 = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoom *= detector.getScaleFactor();
                invalidate();
                return true;
            }
        });

        setOnTouchListener((v, event) -> {
            boolean a = listener1.onTouchEvent(event);
            boolean b = listener2.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    touched = null;
                    invalidate();
                    return true;
            }
            return a || b;
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
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xff000000);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        background = new Paint();
        background.setColor(getResources().getColor(R.color.colorPrimary));
        background.setStyle(Paint.Style.FILL);

        rect = new RectF();
    }

    private Dot getNodeOnScreen(float x, float y) {

        x = fieldX(x);
        y = fieldY(y);

        Dot best = null;
        float effectiveZoom = zoom * w;
        float min = sq((dotSize + 30) / effectiveZoom);
        for (Dot dot : net.dots) {
            float dx = dot.x - x;
            float dy = dot.y - y;
            float dist = dx * dx + dy * dy;
            if (dist < min) {
                best = dot;
                min = dist;
            }
        }

        return best;
    }

    private void onClick(float x, float y, boolean isLongClick) {
        if (net != null && !all.shuffleMode) {

            Dot best = getNodeOnScreen(x, y);
            if (best != null) {

                boolean share = !isLongClick;

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

                all.runOnUiThread(() -> all.finished());

                invalidate();

            }

        }
    }

    AllManager all;

    public void setAll(AllManager all) {
        this.all = all;
    }

    public float screenX(float dx) {
        return w * 0.5f + (dx + cx) * w * zoom;
    }

    public float screenY(float dy) {
        return h * 0.5f + (dy + cy) * w * zoom;
    }

    public float fieldX(float x) {
        return (x - w * 0.5f) / (w * zoom) - cx;
    }

    public float fieldY(float y) {
        return (y - h * 0.5f) / (w * zoom) - cy;
    }

    private Net net;

    public void setNet(Net net) {
        this.net = net;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        w = getMeasuredWidth();
        h = getMeasuredHeight();

        canvas.drawRect(0, 0, w, h, background);

        if (!all.shuffleMode) {
            touched = null;
        }

        // if(net == null) net = new Net(100, 20, 100);
        if (net != null) {

            dotSize = zoom * w * .4f / (float) sqrt(Math.max(net.dots.length - 4, 1));

            // draw the connecting line
            for (Dot a : net.dots) {
                if (a.connected != null) for (int i = 0, l = a.edges(); i < l; i++) {
                    Dot b = a.get(i);
                    // todo color dependent on the money difference?
                    if (b.compareTo(a) < 0)
                        canvas.drawLine(screenX(a.x), screenY(a.y), screenX(b.x), screenY(b.y), linePaint);
                }
            }

            for (Dot dot : net.dots) {
                // draw the dot
                float x = screenX(dot.x), y = screenY(dot.y);
                rect.left = x - dotSize / 2;
                rect.top = y - dotSize / 2;
                rect.right = x + dotSize / 2;
                rect.bottom = y + dotSize / 2;
                int color = dot.value < 0 ? 0xffff0000 : dot.value == 0 ? -1 : 0xff00ff00;
                if (touched == dot) {
                    color = color & 0x7fffffff;
                }
                dotPaint.setColor(color);
                canvas.drawArc(rect, 0, 360, true, dotPaint);
                textPaint.setTextSize(dotSize * .5f);
                canvas.drawText(dot.value + "", x, y + dotSize * .18f, textPaint);
            }
        }

    }
}
