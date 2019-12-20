package com.mivik.medit;

import android.util.Log;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public final class Logs {
	public static boolean EnableLog = true;
	private static final String T = "VEdit";
	private static RandomAccessFile Q;
	private static File Origin = null;
	private static final byte[] Writing = new byte[0];

	private static void safeDelete(File f) {
		if (f.isDirectory())
			for (File one : f.listFiles()) safeDelete(one);
		f.delete();
	}

	public static void setLogFile(File f) {
		if (!EnableLog) return;
		synchronized (Writing) {
			if (f == null) return;
			if (f.exists()) safeDelete(f);
			if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
			try {
				Q = new RandomAccessFile(f, "rw");
			} catch (IOException e) {
				throw new RuntimeException("Can't create RandomAccessFile for:" + f.getAbsolutePath(), e);
			}
			if (Origin != null && Origin.exists()) {
				try {
					f.createNewFile();
					FileInputStream in = new FileInputStream(Origin);
					FileChannel inc = in.getChannel();
					WritableByteChannel outc = Q.getChannel();
					inc.transferTo(0, inc.size(), outc);
					inc.close();
					in.close();
				} catch (IOException e) {
					throw new RuntimeException("Can't copy origin log file(" + Origin.getAbsolutePath() + ") to icon_create one(" + f.getAbsolutePath() + ")", e);
				}
			}
		}
	}

	public static String getLogText() {
		if (!EnableLog) return null;
		if (!Origin.exists()) return null;
		try {
			InputStream in = new FileInputStream(Origin);
			byte[] data = new byte[in.available()];
			in.read(data);
			in.close();
			String s = new String(data);
			data = null;
			System.gc();
			return s;
		} catch (IOException e) {
			Log.e(T, "Can't get log text from file(" + Origin.getAbsolutePath() + ")", e);
			return null;
		}
	}

	public static void clearLog() {
		if (!EnableLog) return;
		if (Origin == null) return;
		synchronized (Writing) {
			try {
				Q.close();
				Origin.delete();
				Q = new RandomAccessFile(Origin, "rw");
			} catch (IOException e) {
				Log.e(T, "Can't recreate FileOutputStream for log file(" + Origin.getAbsolutePath() + ")", e);
			}
		}
	}

	public static void closeLog(boolean keepFile) {
		if (!EnableLog) return;
		synchronized (Writing) {
			try {
				Q.close();
			} catch (IOException e) {
				throw new RuntimeException("Can't close RandomAccessFile", e);
			}
			Q = null;
			if (!keepFile) Origin.delete();
			Origin = null;
		}
	}

	public static File getLogFile() {
		if (!EnableLog) return null;
		return Origin;
	}

	private static void print(char level, String msg) {
		try {
			Q.write((level + " " + msg).getBytes());
			Q.write('\n');
		} catch (IOException e) {
			throw new RuntimeException("Can't write log", e);
		}
	}

	private static void printMsg(char level, String msg) {
		if (!EnableLog) return;
		synchronized (Writing) {
			print(level, msg);
		}
	}

	public static String getStackTraceString(Throwable t) {
		return Log.getStackTraceString(t);
	}

	private static void printErr(char level, String msg, Throwable err) {
		if (!EnableLog) return;
		synchronized (Writing) {
			print(level, msg);
			print(level, getStackTraceString(err));
		}
	}

	public static void v(String msg) {
		printMsg('V', msg);
	}

	public static void v(String msg, Throwable err) {
		printErr('V', msg, err);
	}

	public static void d(String msg) {
		printMsg('D', msg);
	}

	public static void d(String msg, Throwable err) {
		printErr('D', msg, err);
	}

	public static void i(String msg) {
		printMsg('I', msg);
	}

	public static void i(String msg, Throwable err) {
		printErr('I', msg, err);
	}

	public static void w(String msg) {
		printMsg('W', msg);
	}

	public static void w(String msg, Throwable err) {
		printErr('W', msg, err);
	}

	public static void e(String msg) {
		printMsg('E', msg);
	}

	public static void e(String msg, Throwable err) {
		printErr('E', msg, err);
	}

	public static void e(Throwable err) {
		if (!EnableLog) return;
		synchronized (Writing) {
			print('E', getStackTraceString(err));
		}
	}

	public static void wtf(String msg) {
		printMsg('F', msg);
	}

	public static void wtf(String msg, Throwable err) {
		printErr('F', msg, err);
	}

	public static void wtf(Throwable err) {
		if (!EnableLog) return;
		synchronized (Writing) {
			print('F', getStackTraceString(err));
		}
	}
}