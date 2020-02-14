package com.mivik.malaxy;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import com.mivik.malaxy.ui.FullScreenDialog;
import com.mivik.malaxy.ui.UI;

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
		drawable.setColorSchemeColors(G.REFRESH_COLORS);
		drawable.setStrokeWidth(15);
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

	public LoadingDialog setMessage(int res) {
		return setMessage(getContext().getString(res));
	}

	public LoadingDialog setMessage(CharSequence cs) {
		Message.setText(cs);
		Message.setVisibility(cs == null ? View.GONE : View.VISIBLE);
		return this;
	}
}