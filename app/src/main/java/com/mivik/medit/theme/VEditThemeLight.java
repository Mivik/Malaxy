package com.mivik.medit.theme;

import com.xsjiong.vlexer.VLexer;

public class VEditThemeLight extends VEditTheme {
	private static VEditThemeLight INSTANCE;

	public static VEditThemeLight getInstance() {
		if (INSTANCE == null) INSTANCE = new VEditThemeLight();
		return INSTANCE;
	}

	private VEditThemeLight() {
		setSplitLineColor(0xFF2196F3);
		setSelectionColor(0x552196F3);
		setCursorLineColor(0xFFFF5722);
		setCursorColor(0xFF2196F3);
		setCursorGlassColor(0x442196F3);
		setBackgroundColor(0xFFFFFFFF);
		setLineNumberColor(0xFF000000);
		setSlideBarColor(0xCC2196F3);
		C[VLexer.TYPE_IDENTIFIER] = 0xFF757575;
		C[VLexer.TYPE_KEYWORD] = 0xFF3949AB;
		C[VLexer.TYPE_NUMBER] = 0xFFF44336;
		C[VLexer.TYPE_COMMENT] = 0xFF4CAF50;
		C[VLexer.TYPE_STRING] = 0xFF9C27B0;
		C[VLexer.TYPE_CHAR] = C[VLexer.TYPE_STRING];
		C[VLexer.TYPE_OPERATOR] = 0xFF00ACC1;
		C[VLexer.TYPE_BOOLEAN] = C[VLexer.TYPE_NUMBER];
		C[VLexer.TYPE_ASSIGNMENT] = C[VLexer.TYPE_OPERATOR];
		C[VLexer.TYPE_NULL] = C[VLexer.TYPE_NUMBER];
		C[VLexer.TYPE_LEFT_PARENTHESIS] = 0xFF0D47A1;
		C[VLexer.TYPE_RIGHT_PARENTHESIS] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_LEFT_SQUARE_BRACKET] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_RIGHT_SQUARE_BRACKET] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_LEFT_BRACE] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_RIGHT_BRACE] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_SEMICOLON] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_COLON] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_PERIOD] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_COMMA] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_EOF] = 0xFF000000; // ?????
		C[VLexer.UNRESOLVED_TYPE] = 0xFF000000;
		C[VLexer.TYPE_PREPROCESSOR_COMMAND] = C[VLexer.TYPE_SEMICOLON];
	}
}