package com.mivik.medit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import com.mivik.malaxy.C;
import com.xsjiong.vlexer.*;

import java.io.File;
import java.util.ArrayList;

public final class G implements C {
	private static SharedPreferences S;
	public static int _LEXER_ID;
	public static int _TEXT_SIZE;
	public static File _HOME_DIR;
	public static boolean _SHOW_LINE_NUMBER;
	public static boolean _NIGHT_THEME;
	public static final ArrayList<File> _BOOKMARKS = new ArrayList<>();

	public static final void Initialize(Context cx) {
		S = cx.getSharedPreferences("editor_config", Context.MODE_PRIVATE);
		String str = S.getString("lexer_name", LEXER_NAMES[0]);
		for (int i = 0; i < LEXER_NAMES.length; i++)
			if (LEXER_NAMES[i].equals(str)) {
				_LEXER_ID = i;
				break;
			}
		_TEXT_SIZE = S.getInt("text_size", 14);
		_HOME_DIR = new File(S.getString("home_dir", Environment.getExternalStorageDirectory().getAbsolutePath()));
		str = S.getString("bookmarks", null);
		if (str != null) {
			String[] all = str.split(File.pathSeparator);
			for (int i = 0; i < all.length; i++) _BOOKMARKS.add(new File(all[i]));
		}
		_SHOW_LINE_NUMBER = S.getBoolean("show_line_number", true);
		_NIGHT_THEME = S.getBoolean("night_theme", true);
	}

	public static void setLexerId(int id) {
		S.edit().putString("lexer_name", LEXER_NAMES[_LEXER_ID = id]).apply();
	}

	public static void setTextSize(int size) {
		S.edit().putInt("text_size", _TEXT_SIZE = size).apply();
	}

	public static void setHomeDir(File dir) {
		S.edit().putString("home_dir", (_HOME_DIR = dir).getAbsolutePath()).apply();
	}

	public static void setShowLineNumber(boolean flag) {
		S.edit().putBoolean("show_line_number", _SHOW_LINE_NUMBER = flag).apply();
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

	public static final int getLexerIndex(VLexer lexer) {
		Class<? extends VLexer> cl = lexer.getClass();
		for (int i = 0; i < LEXERS.length; i++)
			if (LEXERS[i] == cl) return i;
		return -1;
	}

	public static final VLexer newLexer(int index) {
		try {
			return LEXERS[index].newInstance();
		} catch (Throwable t) {
			return null;
		}
	}
}