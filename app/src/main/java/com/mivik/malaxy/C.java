package com.mivik.malaxy;

import com.xsjiong.vlexer.*;

public interface C {
	String T = "VEdit";
	int[] REFRESH_COLORS = {0xFF2196F3, 0xFFFBC02D, 0xFFFF5722, 0xFFE91E63, 0xFF7E57C2};
	boolean LOG_TIME = false;
	Class<? extends VLexer>[] LEXERS = (Class<? extends VLexer>[]) new Class<?>[] {null, VJavaLexer.class, VJavaScriptLexer.class, VCLexer.class, VCppLexer.class, VNullLexer.class};
	String[] LEXER_NAMES = {"自动选择", "Java", "JavaScript", "C", "C++", "无"};
}
