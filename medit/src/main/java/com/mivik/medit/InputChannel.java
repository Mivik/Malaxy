package com.mivik.medit;

public abstract class InputChannel {
	public void onRead(char c) {
		onRead(new char[]{c}, 0, 1);
	}

	public void onRead(String str) {
		onRead(str.toCharArray(), 0, str.length());
	}

	public void onRead(char[] cs) {
		onRead(cs, 0, cs.length);
	}

	public abstract void onRead(char[] cs, int off, int len);
}