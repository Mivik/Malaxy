package com.mivik.malaxy;

import android.view.MotionEvent;
import com.mivik.medit.MEdit;

public class ZoomHelper implements MEdit.EventHandler {
	public static final float MIN_TEXT_SIZE = 16;

	private int cnt = 0;
	private float oldDist;
	private float textSize = 0;
	private float relativeScrollY = 0;
	private boolean ok;

	@Override
	public boolean handleEvent(MEdit medit, MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				cnt = 1;
				ok = false;
				return false;
			case MotionEvent.ACTION_UP:
				cnt = 0;
				return ok;
			case MotionEvent.ACTION_POINTER_DOWN:
				oldDist = distant(event);
				++cnt;
				if (cnt == 2) {
					ok = true;
					relativeScrollY = 0;
					textSize = medit.getTextSize();
					relativeScrollY = (float) medit.getScrollY() / medit.getContentHeight();
				}
				return cnt >= 2;
			case MotionEvent.ACTION_POINTER_UP:
				--cnt;
				return false;
			case MotionEvent.ACTION_MOVE:
				if (cnt >= 2) {
					float newDist = distant(event);
					if (newDist > oldDist + 1 || newDist < oldDist - 1) {
						textSize *= (newDist / oldDist);
						if (textSize < MIN_TEXT_SIZE) textSize = MIN_TEXT_SIZE;
						medit.setTextSize(textSize);
						G.setTextSize(textSize / medit.getContext().getResources().getDisplayMetrics().scaledDensity);
						medit.setScrollY((int) (medit.getContentHeight() * relativeScrollY));
						oldDist = newDist;
					}
					return true;
				}
				return false;
			default:
				return false;
		}
	}

	private static float distant(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}
}