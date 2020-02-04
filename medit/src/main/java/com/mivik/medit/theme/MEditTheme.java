package com.mivik.medit.theme;

import com.mivik.mlexer.MLexer;

public abstract class MEditTheme {
	protected int[] C = new int[MLexer.TOTAL_COUNT];
	protected int _SplitLine, _Selection, _CursorLine, _Indicator, _IndicatorGlass, _BackgroundColor, _LineNumberColor, _SlideBarColor;

	protected void setTypeColor(short type, int color) {
		C[type] = color;
	}

	public int getTypeColor(short type) {
		return C[type];
	}

	protected void setSplitLineColor(int color) {
		_SplitLine = color;
	}

	public int getSplitLineColor() {
		return _SplitLine;
	}

	protected void setSelectionColor(int color) {
		_Selection = color;
	}

	public int getSelectionColor() {
		return _Selection;
	}

	protected void setCursorLineColor(int color) {
		_CursorLine = color;
	}

	public int getCursorLineColor() {
		return _CursorLine;
	}

	protected void setIndicatorColor(int color) {
		_Indicator = color;
	}

	public int getIndicatorColor() {
		return _Indicator;
	}

	protected void setIndicatorGlassColor(int color) {
		_IndicatorGlass = color;
	}

	public int getIndicatorGlassColor() {
		return _IndicatorGlass;
	}

	protected void setBackgroundColor(int color) {
		_BackgroundColor = color;
	}

	public int getBackgroundColor() {
		return _BackgroundColor;
	}

	protected void setLineNumberColor(int color) {
		_LineNumberColor = color;
	}

	public int getLineNumberColor() {
		return _LineNumberColor;
	}

	protected void setSlideBarColor(int color) {
		_SlideBarColor = color;
	}

	public int getSlideBarColor() {
		return _SlideBarColor;
	}
}