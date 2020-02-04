package com.mivik.medit.theme;

import com.mivik.mlexer.MLexer;

public class MEditThemeDark extends MEditTheme {
	private static MEditThemeDark INSTANCE;

	public static MEditThemeDark getInstance() {
		if (INSTANCE == null) INSTANCE = new MEditThemeDark();
		return INSTANCE;
	}

	private MEditThemeDark() {
		final int orange = 0xFFF9AE58;
		final int orange2 = 0xFFEE932B;
		final int white = 0xFFFFFFFF;
		final int red2 = 0xFFF97B58;
		final int blue2 = 0xFF4E5A65;
		final int blue3 = 0xFF343D46;
		final int blue5 = 0xFF5FB4B4;
		final int blue6 = 0xFFA6ACB9;
		final int white3 = 0xFFD8DEE9;
		final int pink = 0xFFC695C6;
		final int green = 0xFF99C794;
		setSplitLineColor(white3);
		setSelectionColor(0x55FFFFFF & white3);
		setCursorLineColor(orange);
//		setCursorColor(0xFFE53935);
		setIndicatorColor(orange2);
		setIndicatorGlassColor(0x44FFFFFF & getIndicatorColor());
		setBackgroundColor(blue3);
		setLineNumberColor(0xFF868E98);
		setSlideBarColor(0xFF5F666D);
		C[MLexer.TYPE_IDENTIFIER] = white;
		C[MLexer.TYPE_KEYWORD] = pink;
		C[MLexer.TYPE_NUMBER] = orange;
		C[MLexer.TYPE_COMMENT] = blue6;
		C[MLexer.TYPE_STRING] = green;
		C[MLexer.TYPE_CHAR] = C[MLexer.TYPE_STRING];
		C[MLexer.TYPE_OPERATOR] = red2;
		C[MLexer.TYPE_BOOLEAN] = C[MLexer.TYPE_NUMBER];
		C[MLexer.TYPE_ASSIGNMENT] = C[MLexer.TYPE_OPERATOR];
		C[MLexer.TYPE_NULL] = pink;
		C[MLexer.TYPE_PARENTHESIS] = white;
		C[MLexer.TYPE_SQUARE_BRACKET] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_BRACE] = C[MLexer.TYPE_PARENTHESIS];
		C[MLexer.TYPE_SEMICOLON] = blue6;
		C[MLexer.TYPE_COLON] = C[MLexer.TYPE_SEMICOLON];
		C[MLexer.TYPE_PERIOD] = C[MLexer.TYPE_SEMICOLON];
		C[MLexer.TYPE_COMMA] = C[MLexer.TYPE_SEMICOLON];
		C[MLexer.TYPE_PREPROCESSOR_COMMAND] = blue5;
		C[MLexer.TYPE_PURE] = white;
		C[MLexer.TYPE_TAG_START] = C[MLexer.TYPE_IDENTIFIER];
		C[MLexer.TYPE_TAG_END] = C[MLexer.TYPE_IDENTIFIER];
		C[MLexer.TYPE_CDATA] = C[MLexer.TYPE_PREPROCESSOR_COMMAND];
	}
}