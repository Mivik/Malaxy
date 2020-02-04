package com.mivik.malaxy.ui;

import android.content.Context;
import android.text.Editable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
