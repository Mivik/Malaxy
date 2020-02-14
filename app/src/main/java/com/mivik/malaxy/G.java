package com.mivik.malaxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import androidx.core.util.ObjectsCompat;
import com.mivik.mlexer.MLexer;

import java.io.File;
import java.util.ArrayList;

public final class G implements Const {
	private static SharedPreferences S;
	public static int _LEXER_ID;
	public static float _TEXT_SIZE;
	public static File _HOME_DIR;
	public static boolean _SHOW_LINE_NUMBER;
	public static boolean _NIGHT_THEME;
	public static String _FONT_STRING;
	public static Typeface _FONT;
	public static final ArrayList<File> _BOOKMARKS = new ArrayList<>();
	public static boolean _SPLIT_LINE;

	public static final void Initialize(Context cx) {
		S = cx.getSharedPreferences("editor_config", Context.MODE_PRIVATE);
		String str = S.getString("lexer_name", LEXER_NAMES[0]);
		for (int i = 0; i < LEXER_NAMES.length; i++)
			if (ObjectsCompat.equals(LEXER_NAMES[i], str)) {
				_LEXER_ID = i;
				break;
			}
		_TEXT_SIZE = S.getFloat("text_size", 14);
		_HOME_DIR = new File(S.getString("home_dir", Environment.getExternalStorageDirectory().getAbsolutePath()));
		str = S.getString("bookmarks", null);
		if (str != null) {
			String[] all = str.split(File.pathSeparator);
			for (int i = 0; i < all.length; i++) _BOOKMARKS.add(new File(all[i]));
		}
		_SHOW_LINE_NUMBER = S.getBoolean("show_line_number", true);
		_NIGHT_THEME = S.getBoolean("night_theme", true);
		_FONT_STRING = S.getString("font", "#FiraCode");
		_SPLIT_LINE = S.getBoolean("split_line", false);
		updateFont(cx.getAssets());
	}

	private static void updateFont(AssetManager manager) {
		_FONT = null;
		switch (_FONT_STRING.charAt(0)) {
			case '#':
				_FONT = Typeface.createFromAsset(manager, _FONT_STRING.substring(1) + ".ttf");
				break;
			case '@': {
				switch (Integer.parseInt(_FONT_STRING.substring(1))) {
					case 0:
						_FONT = Typeface.DEFAULT;
						break;
					case 1:
						_FONT = Typeface.MONOSPACE;
						break;
					case 2:
						_FONT = Typeface.DEFAULT_BOLD;
						break;
				}
				break;
			}
			default:
				try {
					_FONT = Typeface.createFromFile(_FONT_STRING);
				} catch (Throwable t) {
					Log.e(Const.T, "Failed to load font: " + _FONT_STRING, t);
				}
				break;
		}
		if (_FONT == null) _FONT = Typeface.DEFAULT;
	}

	public static void setSplitLine(boolean flag) {
		S.edit().putBoolean("split_line", _SPLIT_LINE = flag).apply();
	}

	public static void setLexerId(int id) {
		S.edit().putString("lexer_name", LEXER_NAMES[_LEXER_ID = id]).apply();
	}

	public static void setTextSize(float size) {
		S.edit().putFloat("text_size", _TEXT_SIZE = size).apply();
	}

	public static void setHomeDir(File dir) {
		S.edit().putString("home_dir", (_HOME_DIR = dir).getAbsolutePath()).apply();
	}

	public static void setShowLineNumber(boolean flag) {
		S.edit().putBoolean("show_line_number", _SHOW_LINE_NUMBER = flag).apply();
	}

	public static void setFont(Context cx, String font) {
		if (_FONT_STRING.equals(font)) return;
		S.edit().putString("font", _FONT_STRING = font).apply();
		updateFont(cx.getAssets());
	}

	public static void setNightTheme(boolean flag) {
		S.edit().putBoolean("night_theme", _NIGHT_THEME = flag).apply();
	}

	public static void onBookmarksUpdate() {
		StringBuffer buffer = new StringBuffer();
		final int size = _BOOKMARKS.size();
		for (int i = 0; i < size; i++) {
			buffer.append(_BOOKMARKS.get(i).getAbsolutePath());
			if (i != size - 1) buffer.append(File.pathSeparatorChar);
		}
		S.edit().putString("bookmarks", buffer.toString()).apply();
	}

	public static final int getLexerIndex(MLexer lexer) {
		Class<? extends MLexer> cl = lexer.getClass();
		for (int i = 0; i < LEXERS.length; i++)
			if (LEXERS[i] == cl) return i;
		return -1;
	}

	public static final MLexer newLexer(int index) {
		try {
			return LEXERS[index].newInstance();
		} catch (Throwable t) {
			return null;
		}
	}
}