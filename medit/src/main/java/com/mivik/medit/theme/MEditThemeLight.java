package com.mivik.medit.theme;

import com.mivik.mlexer.MLexer;

public class MEditThemeLight extends MEditTheme {
	private static MEditThemeLight INSTANCE;

	public static MEditThemeLight getInstance() {
		if (INSTANCE == null) INSTANCE = new MEditThemeLight();
		return INSTANCE;
	}

	private MEditThemeLight() {
		setSplitLineColor(0xFF2196F3);
		setSelectionColor(0x552196F3);
		setCursorLineColor(0xFFFF5722);
		setIndicatorColor(0xFF2196F3);
		setIndicatorGlassColor(0x442196F3);
		setBackgroundColor(0xFFFFFFFF);
		setLineNumberColor(0xFF000000);
		setSlideBarColor(0xCC2196F3);
		C[MLexer.TYPE_IDENTIFIER] = 0xFF757575;
		C[MLexer.TYPE_KEYWORD] = 0xFF3949AB;
		C[MLexer.TYPE_NUMBER] = 0xFFF44336;
		C[MLexer.TYPE_COMMENT] = 0xFF4CAF50;
		C[MLexer.TYPE_STRING] = 0xFF9C27B0;
		C[MLexer.TYPE_CHAR] = C[MLexer.TYPE_STRING];
		C[MLexer.TYPE_OPERATOR] = 0xFF00ACC1;
		C[MLexer.TYPE_BOOLEAN] = C[MLexer.TYPE_NUMBER];
		C[MLexer.TYPE_ASSIGNMENT] = C[MLexer.TYPE_OPERATOR];
		C[MLexer.TYPE_NULL] = C[MLexer.TYPE_NUMBER];
		C[MLexer.TYPE_PARENTHESIS] = 0xFF0D47A1;
		C[MLexer.TYPE_SQUARE_BRACKET] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_BRACE] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_SEMICOLON] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_COLON] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_PERIOD] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_COMMA] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_PREPROCESSOR_COMMAND] = C[MLexer.TYPE_SEMICOLON];
		C[MLexer.TYPE_PURE] = 0xFF000000;
		C[MLexer.TYPE_CONTENT] = C[MLexer.TYPE_PURE];
		C[MLexer.TYPE_TAG_START] = C[MLexer.TYPE_KEYWORD];
		C[MLexer.TYPE_TAG_END] = C[MLexer.TYPE_KEYWORD];
		C[MLexer.TYPE_CONTENT_START] = C[MLexer.TYPE_TAG_START];
		C[MLexer.TYPE_CDATA] = C[MLexer.TYPE_PREPROCESSOR_COMMAND];
	}
}