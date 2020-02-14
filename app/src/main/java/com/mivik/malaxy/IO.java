package com.mivik.malaxy;

import android.util.Log;
import com.ibm.icu.text.CharsetDetector;

import java.io.*;
import java.nio.charset.Charset;

public class IO {
	public static int BUFFER_SIZE = 1024;

	public static Charset guessCharset(byte[] data) {
		CharsetDetector detector = new CharsetDetector();
		detector.setText(data);
		final String charsetName = detector.detect().getName();
		Log.i(Const.T, "Detected charset: " + charsetName);
		Charset charset = Charset.defaultCharset();
		try {
			charset = Charset.forName(charsetName);
		} catch (Throwable t) {
			Log.e(Const.T, "Cannot find charset with name: " + charsetName);
		}
		return charset;
	}

	public static byte[] Read(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Copy(in, out);
		out.close();
		return out.toByteArray();
	}

	public static boolean SafeDelete(File f) {
		if (f == null || (!f.exists())) return false;
		if (f.isFile()) return f.delete();
		File[] fs = f.listFiles();
		boolean ret = true;
		for (int i = 0; i < fs.length; i++) ret &= SafeDelete(fs[i]);
		return ret;
	}

	public static void Copy(InputStream in, OutputStream out) throws IOException {
		int read;
		byte[] buf = new byte[BUFFER_SIZE];
		while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
		in.close();
	}
}