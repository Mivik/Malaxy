package com.mivik.medit;

public abstract class OutputChannel {
	public void onWrite(char c) {
		onWrite(new char[]{c}, 0, 1);
	}

	public void onWrite(String str) {
		onWrite(str.toCharArray(), 0, str.length());
	}

	public void onWrite(char[] cs) {
		onWrite(cs, 0, cs.length);
	}

	public abstract void onWrite(char[] cs, int off, int len);
}