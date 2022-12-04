package me.antonio.noack.thedollargame;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by antonio on 23. May 2018
 */

public abstract class ZoomingListener implements View.OnTouchListener {

	public abstract void onTouchMove(float x, float y, float dx, float dy, boolean notFirst);
	public abstract void onRotZoomTwice(View v, boolean notFirst, float da, float a, float s, float ls, float x, float x2, float y, float y2, float lx, float lx2, float ly, float ly2);
	public abstract void onClick(float x, float y, long durationMillis);

	private long clickStart;
	private int count;
	private boolean hasLast1, hasLast2;
	private float lastAngle, lastSize, lastX2, lastY2;
	private float lastX, lastY;

	@Override public boolean onTouch(View view, final MotionEvent event) {
		final float thatX, thatY, deltaX, deltaY;

		float w = view.getWidth(), h = view.getHeight();

		thatX = event.getX() / w * 2 - 1;
		thatY = event.getY() / h * 2 - 1;

		deltaX = thatX - lastX;
		deltaY = thatY - lastY;

		// eine Reihe von ordentlichen Standard-Bewegungen wäre ja echt nett, z.B. auch zoomen und drehen :)
		// das sollte uns Events geben, mit denen man arbeiten kann :)
		switch(event.getActionMasked()){
			case MotionEvent.ACTION_DOWN:// 1st down
				clickStart = System.currentTimeMillis();
				count = 1;
				hasLast1 = false;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:// 2nd - 100th down
				count++;
				hasLast2 = false;
				lastSize = 1000;
				break;
			case MotionEvent.ACTION_MOVE:
				if(count == 1){// ggf queue Event, wenn wir zu 3D übergehen und Performance brauchen ...
					onTouchMove(thatX, thatY, deltaX, deltaY, hasLast1);
					hasLast1 = true;
				} else if(count == 2){

					float thatX2 = event.getX(1) / w * 2 - 1, thatY2 = event.getY(1) / h * 2 - 1;
					float dx = thatX2-thatX, dy = (thatY2-thatY) * w / h;
					float angle = (float) Math.atan2(dy, dx);
					float size = (float) Math.sqrt(dx*dx+dy*dy);
					float deltaAngle = hasLast2 ? makeAngle(angle - lastAngle) : 0;

					onRotZoomTwice(view, hasLast2, deltaAngle, angle, size, lastSize, /*Rendering.width, */thatX, thatX2, thatY, thatY2, lastX, lastX2, lastY, lastY2);

					lastAngle = angle;
					lastSize = size;
					hasLast2 = true;
					lastX2 = thatX2;
					lastY2 = thatY2;
				}

				break;
			case MotionEvent.ACTION_UP:// last one leaves
				count = 0;
				onClick(lastX, lastY, System.currentTimeMillis() - clickStart);
				break;
			case MotionEvent.ACTION_POINTER_UP:// somebody leaves
				count = 0;break;

		}

		lastX = thatX;
		lastY = thatY;

		return true;// consumed
	}

	private static final float PI = (float) Math.PI;
	private static float makeAngle(float delta){
		return delta < -PI ? delta + 2*PI : delta > PI ? delta - 2*PI : delta;
	}
}
