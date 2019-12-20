package com.mivik.medit.theme;

import com.xsjiong.vlexer.VLexer;

public class VEditThemeDark extends VEditTheme {
	private static VEditThemeDark INSTANCE;

	public static VEditThemeDark getInstance() {
		if (INSTANCE == null) INSTANCE = new VEditThemeDark();
		return INSTANCE;
	}

	private VEditThemeDark() {
		setSplitLineColor(0xFF0288D1);
		setSelectionColor(0x550288D1);
		setCursorLineColor(0xFFE8EAF6);
		setCursorColor(0xFFE53935);
		setCursorGlassColor(0x44E53935);
		setBackgroundColor(0xFF37474F);
		setLineNumberColor(0xFFFFFFFF);
		setSlideBarColor(0xCC2196F3);
		C[VLexer.TYPE_IDENTIFIER] = 0xFF00BCD4;
		C[VLexer.TYPE_KEYWORD] = 0xFF5C6BC0;
		C[VLexer.TYPE_NUMBER] = 0xFFE91E63;
		C[VLexer.TYPE_COMMENT] = 0xFF4CAF50;
		C[VLexer.TYPE_STRING] = 0xFFFF5722;
		C[VLexer.TYPE_CHAR] = C[VLexer.TYPE_STRING];
		C[VLexer.TYPE_OPERATOR] = 0xFF00ACC1;
		C[VLexer.TYPE_BOOLEAN] = C[VLexer.TYPE_NUMBER];
		C[VLexer.TYPE_ASSIGNMENT] = C[VLexer.TYPE_OPERATOR];
		C[VLexer.TYPE_NULL] = C[VLexer.TYPE_NUMBER];
		C[VLexer.TYPE_LEFT_PARENTHESIS] = 0xFF9C27B0;
		C[VLexer.TYPE_RIGHT_PARENTHESIS] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_LEFT_SQUARE_BRACKET] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_RIGHT_SQUARE_BRACKET] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_LEFT_BRACE] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_RIGHT_BRACE] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_SEMICOLON] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_COLON] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_PERIOD] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_COMMA] = C[VLexer.TYPE_LEFT_PARENTHESIS];
		C[VLexer.TYPE_EOF] = 0xFFFFFFFF; // ?????
		C[VLexer.UNRESOLVED_TYPE] = 0xFFFFFFFF;
		C[VLexer.TYPE_PREPROCESSOR_COMMAND] = C[VLexer.TYPE_SEMICOLON];
	}
}