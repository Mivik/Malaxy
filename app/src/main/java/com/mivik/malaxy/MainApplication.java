package com.mivik.malaxy;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.mivik.malaxy.ui.UI;

import java.io.File;
import java.io.FileOutputStream;

public class MainApplication extends Application implements CrashHandler.CrashListener, Const {
	private static File ERROR_FILE = new File(Environment.getExternalStorageDirectory(), "VEditCrash.txt");
	private static Context cx;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		G.Initialize(base);
		CrashHandler.install(this);
		//Logs.setLogFile(new File(Environment.getExternalStorageDirectory(), "VEditLog.txt"));
	}

	public static Context getContext() {
		return cx;
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
			Log.e(T, "Error While Saving Error:" + Log.getStackTraceString(th));
		}
		UI.onUI(new Runnable() {
			@Override
			public void run() {
				CrashActivity.showMessage(getApplicationContext(), es);
			}
		});
	}
}
