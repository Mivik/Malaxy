package com.mivik.malaxy;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.mivik.malaxy.ui.FullScreenDialog;
import com.mivik.malaxy.ui.UI;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

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
		SmoothProgressDrawable drawable = new SmoothProgressDrawable.Builder(getContext())
				.strokeWidth(15)
				.backgroundDrawable(new ColorDrawable(Color.TRANSPARENT))
				.colors(Const.REFRESH_COLORS)
				.build();
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
