package com.mivik.medit.theme;

import com.mivik.mlexer.MLexer;

public abstract class MEditTheme {
	protected int[] C = new int[MLexer.TOTAL_COUNT];
	protected int mSplitLineColor, mSelectionColor, mCursorLineColor, mIndicatorColor, mBackgroundColor, mLineNumberColor, mSlideBarColor;

	protected void setTypeColor(short type, int color) {
		C[type] = color;
	}

	public int getTypeColor(short type) {
		return C[type];
	}

	protected void setSplitLineColor(int color) {
		mSplitLineColor = color;
	}

	public int getSplitLineColor() {
		return mSplitLineColor;
	}

	protected void setSelectionColor(int color) {
		mSelectionColor = color;
	}

	public int getSelectionColor() {
		return mSelectionColor;
	}

	protected void setCursorLineColor(int color) {
		mCursorLineColor = color;
	}

	public int getCursorLineColor() {
		return mCursorLineColor;
	}

	protected void setIndicatorColor(int color) {
		mIndicatorColor = color;
	}

	public int getIndicatorColor() {
		return mIndicatorColor;
	}

	protected void setBackgroundColor(int color) {
		mBackgroundColor = color;
	}

	public int getBackgroundColor() {
		return mBackgroundColor;
	}

	protected void setLineNumberColor(int color) {
		mLineNumberColor = color;
	}

	public int getLineNumberColor() {
		return mLineNumberColor;
	}

	protected void setSlideBarColor(int color) {
		mSlideBarColor = color;
	}

	public int getSlideBarColor() {
		return mSlideBarColor;
	}
}