package com.mivik.medit;

import java.io.*;

public class IO {
	public static int BUFFER_SIZE = 1024;

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
