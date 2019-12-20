package com.mivik.medit.ui;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;

public class HintEditText extends TextInputLayout {
	public HintEditText(Context cx) {
		super(cx);
		addView(new TextInputEditText(cx));
	}

	public Editable getText() {
		return getEditText().getText();
	}

	public void setText(CharSequence cs) {
		getEditText().setText(cs);
	}
}
