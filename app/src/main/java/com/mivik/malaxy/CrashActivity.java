package com.mivik.malaxy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

public class CrashActivity extends BaseActivity {
	private ScrollView Root;
	private TextView Message;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Root = new ScrollView(this);
		Message = new TextView(this);
		Message.setText(getIntent().getCharSequenceExtra("msg"));
		Message.setGravity(Gravity.CENTER);
		Root.addView(Message);
		Root.setFillViewport(true);
		setContentView(Root);
		enableBackButton();
	}

	public static Intent getIntent(Context cx, CharSequence msg) {
		Intent i = new Intent(cx, CrashActivity.class);
		i.putExtra("msg", msg);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return i;
	}

	public static void showMessage(Context cx, CharSequence msg) {
		cx.startActivity(getIntent(cx, msg));
	}
}
