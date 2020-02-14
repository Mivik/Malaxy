package com.mivik.malaxy.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.snackbar.Snackbar;
import com.mivik.malaxy.Const;

import java.lang.reflect.Field;

public class UI {
	public static int ThemeColor = 0xFF2196F3;
	public static int AccentColor = 0xFFFFFFFF;
	public static int IconColor = 0xFFB3B3B3;

	public static void tintStatusBar(Activity activity, int color) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			try {
				Window window = activity.getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.setStatusBarColor(color);
			} catch (Throwable t) {
				Log.e(Const.T, "tintStatusBar of " + activity, t);
			}
		}
	}

	private static float DP_SCALE = -1;

	public static final int dp2px(int dp) {
		if (DP_SCALE == -1) DP_SCALE = Resources.getSystem().getDisplayMetrics().density;
		return (int) (DP_SCALE * dp + 0.5f);
	}

	public static void onUI(Runnable action) {
		if (Looper.getMainLooper() == Looper.myLooper()) action.run();
		else new Handler(Looper.getMainLooper()).post(action);
	}

	public static void preventDismiss(Dialog dialog) {
		try {
			Field field = Dialog.class.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, false);
		} catch (Throwable e) {
			Log.wtf(Const.T, e);
		}
	}

	public static void forceDismiss(Dialog dialog) {
		try {
			Field field = Dialog.class.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, true);
		} catch (Throwable e) {
			Log.wtf(Const.T, e);
		}
	}

	public static Drawable tintDrawable(Context cx, int res, int color) {
		Drawable d = DrawableCompat.wrap(cx.getResources().getDrawable(res).mutate());
		DrawableCompat.setTint(d, color);
		return d;
	}

	public static Drawable tintDrawable(Drawable d, int color) {
		d = DrawableCompat.wrap(d);
		DrawableCompat.setTint(d, color);
		return d;
	}

	public static void postShowError(final Context cx, final Throwable t) {
		onUI(new Runnable() {
			@Override
			public void run() {
				showError(cx, t);
			}
		});
	}

	public static AlertDialog showError(final Context cx, Throwable t) {
		final String msg = Log.getStackTraceString(t);
		AlertDialog ret = new AlertDialog.Builder(cx).setTitle("Oops").setMessage(msg).setCancelable(true).setNegativeButton("复制", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ClipboardManager manager = (ClipboardManager) cx.getSystemService(Context.CLIPBOARD_SERVICE);
				manager.setPrimaryClip(ClipData.newPlainText("Error", msg));
			}
		}).setPositiveButton("确定", null).create();
		ret.show();
		return ret;
	}

	public static void postPrint(final View container, final CharSequence cs) {
		onUI(new Runnable() {
			@Override
			public void run() {
				print(container, cs);
			}
		});
	}

	public static void print(View container, CharSequence cs) {
		Snackbar.make(container, cs, Snackbar.LENGTH_SHORT).show();
	}

	public static void postToast(final Context cx, final CharSequence cs) {
		onUI(new Runnable() {
			@Override
			public void run() {
				toast(cx, cs);
			}
		});
	}

	public static void toast(Context cx, CharSequence cs) {
		Toast.makeText(cx, cs, Toast.LENGTH_SHORT).show();
	}

	public static int lightenColor(int color, int a) {
		return Color.argb(Color.alpha(color), Math.min(Color.red(color) + a, 255), Math.min(Color.green(color) + a, 255), Math.min(Color.blue(color) + a, 255));
	}
}