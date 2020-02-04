package com.mivik.malaxy.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class DividerDrawable extends Drawable {
	private Paint paint;

	@Override
	public void setAlpha(int a) {
	}

	@Override
	public void setColorFilter(ColorFilter filter) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	public void setColor(int color) {
		paint.setColor(color);
		invalidateSelf();
	}

	public DividerDrawable(int color) {
		paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);
		paint.setStrokeWidth(2);
	}

	@Override
	public void draw(Canvas c) {
		c.drawRect(getBounds(), paint);
	}

	@Override
	public int getIntrinsicHeight() {
		return 1;
	}
}