package com.mivik.malaxy;

import com.mivik.mlexer.*;

public interface Const {
	String T = "MEdit";
	int[] REFRESH_COLORS = {0xFF2196F3, 0xFFFBC02D, 0xFFFF5722, 0xFFE91E63, 0xFF7E57C2};
	boolean LOG_TIME = false;
	Class<? extends MLexer>[] LEXERS = (Class<? extends MLexer>[]) new Class<?>[]{null, JavaLexer.class, JavaScriptLexer.class, CLexer.class, CppLexer.class, JSONLexer.class, XMLLexer.class, NullLexer.class};
	String[] LEXER_NAMES = {"自动选择", "Java", "JavaScript", "C", "C++", "JSON", "XML", "无"};
	String[] SYSTEM_FONTS = {"默认", "等宽", "粗体"};
	String[] PRESET_FONTS = {"FiraCode", "JetBrainsMono"};
}