package com.mivik.medit.theme;

import com.xsjiong.vlexer.VLexer;

public abstract class VEditTheme {
	protected int[] C = new int[VLexer.TOTAL_COUNT];
	protected int _SplitLine, _Selection, _CursorLine, _Cursor, _CursorGlass, _BackgroundColor, _LineNumberColor, _SlideBarColor;

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

	protected void setCursorColor(int color) {
		_Cursor = color;
	}

	public int getCursorColor() {
		return _Cursor;
	}

	protected void setCursorGlassColor(int color) {
		_CursorGlass = color;
	}

	public int getCursorGlassColor() {
		return _CursorGlass;
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