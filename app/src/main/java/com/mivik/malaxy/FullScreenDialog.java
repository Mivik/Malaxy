package com.mivik.malaxy;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;
import com.mivik.malaxy.R;

public class ullScreenDialog extends Dialog {
	public FullScreenDialog(Context cx) {
		super(cx, R.style.FullScreenDialog);
	}

	@Override
	public void show() {
		super.show();
		Window w = getWindow();
		WindowManager.LayoutParams para = w.getAttributes();
		para.width = -1;
		para.height = -2;
		w.setAttributes(para);
	}
}