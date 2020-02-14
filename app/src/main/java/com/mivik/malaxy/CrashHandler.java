package com.mivik.malaxy;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
	private static final byte[] CRASH_LISTENER_LOCK = new byte[0];

	private static CrashHandler INSTANCE;
	private CrashListener _Listener;
	private Thread.UncaughtExceptionHandler DEFAULT;

	public interface CrashListener {
		void onCrash(Thread t, Throwable err);
	}

	public static void install(CrashListener listener) {
		if (INSTANCE == null) INSTANCE = new CrashHandler();
		synchronized (CRASH_LISTENER_LOCK) {
			INSTANCE.setCrashListener(listener);
		}
	}

	public static void uninstall() {
		if (INSTANCE == null) return;
		INSTANCE.rollBack();
		INSTANCE = null;
	}

	public void setCrashListener(CrashListener listener) {
		_Listener = listener;
	}

	@Override
	public void uncaughtException(Thread t, Throwable thr) {
		if (t == null && thr != null) DEFAULT.uncaughtException(t, thr);
		if (_Listener != null) _Listener.onCrash(t, thr);
	}

	private CrashHandler() {
		DEFAULT = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Looper.loop();
					} catch (Throwable e) {
						Binder.clearCallingIdentity();
						if (e instanceof ExitException) return;
						if (_Listener != null) _Listener.onCrash(Looper.getMainLooper().getThread(), e);
					}
				}
			}
		});
	}

	private void rollBack() {
		Thread.setDefaultUncaughtExceptionHandler(DEFAULT);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				throw new ExitException();
			}
		});
	}

	private static class ExitException extends RuntimeException {
	}
}