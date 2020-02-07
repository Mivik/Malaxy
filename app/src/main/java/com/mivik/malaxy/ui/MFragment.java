package com.mivik.malaxy.ui;

import android.content.Context;
import android.view.View;

public abstract class MFragment {
	protected Context cx;

	public MFragment(Context cx) {
		this.cx = cx;
	}

	public abstract View getView();

	public abstract Object getTag();

	public void onAttach() {
	}

	public void onDettach() {
	}

	public Context getContext() {
		return cx;
	}
}