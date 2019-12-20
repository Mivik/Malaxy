package com.mivik.medit;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.CircularProgressDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.mivik.malaxy.C;
import com.mivik.medit.ui.UI;

public class LoadingDialog extends FullScreenDialog {
	public LoadingDialog(Context cx) {
		super(cx);
		setCanceledOnTouchOutside(false);
		setCancelable(false);
		Initialize();
	}

	private LinearLayout Root;
	private ProgressBar Content;
	private TextView Message;

	private void Initialize() {
		Root = new LinearLayout(getContext());
		Root.setGravity(Gravity.CENTER);
		Root.setBackgroundDrawable(null);
		Root.setOrientation(LinearLayout.VERTICAL);
		Content = new ProgressBar(getContext());
		CircularProgressDrawable drawable = new CircularProgressDrawable(getContext());
		drawable.setStrokeWidth(15);
		drawable.setBackgroundColor(Color.TRANSPARENT);
		drawable.setColorSchemeColors(C.REFRESH_COLORS);
		Content.setIndeterminateDrawable(drawable);
		Root.addView(Content);
		Message = new TextView(getContext());
		Message.setTextColor(Color.WHITE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
		params.topMargin = UI.dp2px(10);
		Root.addView(Message, params);
		Message.setVisibility(View.GONE);
		setContentView(Root);
	}

	public LoadingDialog setMessage(CharSequence cs) {
		Message.setText(cs);
		Message.setVisibility(cs == null ? View.GONE : View.VISIBLE);
		return this;
	}
}
