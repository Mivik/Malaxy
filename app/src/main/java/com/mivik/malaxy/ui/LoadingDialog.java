package com.mivik.malaxy.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import com.mivik.malaxy.G;

public class LoadingDialog extends FullScreenDialog {
	public LoadingDialog(Context cx) {
		super(cx);
		setCanceledOnTouchOutside(false);
		setCancelable(false);
		Initialize();
	}

	private TextView Message;

	private void Initialize() {
		LinearLayout root = new LinearLayout(getContext());
		root.setGravity(Gravity.CENTER);
		root.setBackgroundDrawable(null);
		root.setOrientation(LinearLayout.VERTICAL);
		ProgressBar content = new ProgressBar(getContext());
		CircularProgressDrawable drawable = new CircularProgressDrawable(getContext());
		drawable.setColorSchemeColors(G.REFRESH_COLORS);
		drawable.setStrokeWidth(15);
		content.setIndeterminateDrawable(drawable);
		root.addView(content);
		Message = new TextView(getContext());
		Message.setTextColor(Color.WHITE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
		params.topMargin = UI.dp2px(10);
		root.addView(Message, params);
		Message.setVisibility(View.GONE);
		setContentView(root);
	}

	public LoadingDialog setMessage(int res) {
		return setMessage(getContext().getString(res));
	}

	public LoadingDialog setMessage(CharSequence cs) {
		Message.setText(cs);
		Message.setVisibility(cs == null ? View.GONE : View.VISIBLE);
		return this;
	}
}