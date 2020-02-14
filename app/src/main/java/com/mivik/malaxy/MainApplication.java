package com.mivik.malaxy;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.mivik.malaxy.ui.UI;

import java.io.File;
import java.io.FileOutputStream;

public class MainApplication extends Application implements CrashHandler.CrashListener, Const {
	private static File ERROR_FILE = new File(Environment.getExternalStorageDirectory(), "MalaxyCrash.txt");

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		InitializeConst();
		G.Initialize(base);
		CrashHandler.install(this);
	}

	private void InitializeConst() {
		Const.LEXER_NAMES[0] = getString(R.string.lexer_name_auto);
		Const.LEXER_NAMES[Const.LEXER_NAMES.length - 1] = getString(R.string.lexer_name_none);
		Const.SYSTEM_FONTS[0] = getString(R.string.font_system_default);
		Const.SYSTEM_FONTS[1] = getString(R.string.font_system_monospace);
		Const.SYSTEM_FONTS[2] = getString(R.string.font_system_bold);
	}

	@Override
	public void onCrash(Thread t, Throwable err) {
		final String es = Log.getStackTraceString(err);
		Log.e(T, "Crash Captured:" + es);
		try {
			FileOutputStream out = new FileOutputStream(ERROR_FILE);
			out.write(es.getBytes());
			out.close();
		} catch (Throwable th) {
			Log.e(T, "Failed to save crash log", th);
		}
		UI.onUI(new Runnable() {
			@Override
			public void run() {
				CrashActivity.showMessage(getApplicationContext(), es);
			}
		});
	}
}