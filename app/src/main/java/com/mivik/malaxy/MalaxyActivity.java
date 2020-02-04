package com.mivik.malaxy;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.mivik.medit.InputChannel;
import com.mivik.medit.Malaxy;
import com.mivik.medit.OutputChannel;

public class MalaxyActivity extends BaseActivity {
	private Malaxy malaxy;
	private OutputChannel output;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		malaxy = new Malaxy(this);
		setContentView(malaxy);
		malaxy.setInputChannel(new MyInputChannel());
		output = malaxy.getOutputChannel();
	}

	private class MyInputChannel extends InputChannel {
		@Override
		public void onRead(char[] cs, int off, int len) {
			output.onWrite("hello, ");
			output.onWrite(cs, off, len - 1);
			output.onWrite("!\n");
		}
	}
}