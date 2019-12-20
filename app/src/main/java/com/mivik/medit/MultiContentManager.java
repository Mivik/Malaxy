package com.mivik.medit;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.HorizontalScrollView;
import com.mivik.malaxy.C;
import com.mivik.medit.theme.VEditTheme;
import com.mivik.medit.theme.VEditThemeLight;
import com.mivik.medit.ui.UI;
import com.xsjiong.vlexer.*;

import java.io.File;
import java.io.FileInputStream;

public class MultiContentManager extends LinearLayoutCompat implements View.OnClickListener {
	public static final String UNTITLED = "untitled";

	private static final int EXPAND_SIZE = 8;
	private static int MAX_HEIGHT = -1;
	private static int BUTTON_PADDING = -1;

	private HorizontalScrollView ButtonScroller;
	private LinearLayoutCompat ButtonLayout;
	private int size;
	private EditData[] data = new EditData[EXPAND_SIZE];
	private AppCompatButton CurrentButton;
	private VEdit Content;
	private EditDataClickListener _ClickListener;

	public MultiContentManager(Context cx) {
		super(cx);
		if (MAX_HEIGHT == -1) {
			MAX_HEIGHT = UI.dp2px(30);
			BUTTON_PADDING = UI.dp2px(5);
		}
		setOrientation(LinearLayoutCompat.VERTICAL);
		ButtonLayout = new LinearLayoutCompat(cx);
		ButtonLayout.setOrientation(LinearLayoutCompat.HORIZONTAL);
		ButtonLayout.setDividerPadding(UI.dp2px(5));
		ButtonScroller = new HorizontalScrollView(cx);
		ButtonScroller.setFillViewport(true);
		ButtonScroller.addView(ButtonLayout, -1, MAX_HEIGHT);
		Content = new VEdit(cx);
		setTheme(VEditThemeLight.getInstance());
		addView(ButtonScroller, -1, MAX_HEIGHT);
		addView(Content, -1, -1);
		size = 0;
		onTabSizeUpdated();
	}

	public EditData getCurrentEditData() {
		if (CurrentButton == null) return null;
		return (EditData) CurrentButton.getTag();
	}

	public int getSize() {
		return size;
	}

	public int getIndex() {
		if (CurrentButton == null) return -1;
		return ((EditData) CurrentButton.getTag()).index;
	}

	public EditData getEditData(int index) {
		return data[index];
	}

	private void onTabSizeUpdated() {
		if (size == 0)
			addTab();
	}

	public void setEditDataClickListener(EditDataClickListener listener) {
		_ClickListener = listener;
	}

	public EditDataClickListener getEditDataClickListener() {
		return _ClickListener;
	}

	public VEdit getContent() {
		return Content;
	}

	public void setTheme(VEditTheme theme) {
		ButtonLayout.setBackgroundColor(theme.getBackgroundColor());
		Content.setTheme(theme);
		int ind = getIndex();
		for (int i = 0; i < size; i++)
			selectButton(getButton(i), i == ind);
	}

	public VEditTheme getTheme() {
		return Content.getTheme();
	}

	public void closeExist(File f) {
		for (int i = 0; i < size; i++)
			if (f.equals(data[i].file)) {
				deleteTab(i);
				return;
			}
	}

	private void ensureCapture() {
		int olen = data.length;
		if (size > olen) {
			olen += EXPAND_SIZE;
			EditData[] dst = new EditData[olen];
			System.arraycopy(data, 0, dst, 0, data.length);
			data = dst;
			dst = null;
		}
	}

	public void addTab(File f) {
		for (int i = 0; i < size; i++)
			if (f.equals(data[i].file)) {
				setIndex(i);
				return;
			}
		try {
			addTab(new String(IO.Read(new FileInputStream(f))).toCharArray(), f);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void deleteTab(int pos) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		size--;
		for (int i = pos; i < size; i++) {
			data[i] = data[i + 1];
			data[i].index = i;
		}
		data[size] = null;
		ButtonLayout.removeViewAt(pos);
		int ind = getIndex();
		if (ind == pos) {
			if (pos == size)
				setIndex(size - 1);
			else setIndex(pos);
		} else if (ind > pos) {
			pos = ind - 1;
			setIndex(pos);
		}
		onTabSizeUpdated();
	}

	public void addTab() {
		addTab(size, null);
	}

	public void addTab(char[] cs) {
		addTab(size, cs);
	}

	public void addTab(char[] cs, File file) {
		addTab(size, cs, file);
	}

	public void addTab(int pos, char[] cs) {
		addTab(pos, cs, null);
	}

	public void addTab(int pos, char[] cs, File file) {
		if (pos < 0) pos = 0;
		if (pos > size) pos = size;
		size++;
		ensureCapture();
		for (int i = size - 1; i > pos; i--)
			(data[i] = data[i - 1]).index = i;
		data[pos] = new EditData(pos);
		data[pos].setFile(file);
		if (file != null) data[pos].saved = true;
		data[pos].setText(cs);
		AppCompatButton button = new AppCompatButton(getContext());
		button.setTextColor(Content.getTheme().getLineNumberColor());
		button.setBackgroundColor(Content.getTheme().getBackgroundColor());
		button.setEllipsize(TextUtils.TruncateAt.END);
		button.setText(data[pos].getDisplay());
		button.setPadding(0, 0, 0, 0);
		button.setTextAlignment(TEXT_ALIGNMENT_CENTER);
		button.setAllCaps(false);
		button.setTag(data[pos]);
		button.setOnClickListener(this);
		button.setPadding(BUTTON_PADDING, 0, BUTTON_PADDING, 0);
		ButtonLayout.addView(button, pos);
		setIndex(pos);
		onTabSizeUpdated();
		if (pos == 1 && size == 2 && data[0].cs.length == 0 && data[0].file == null)
			deleteTab(0);
	}

	public void onEditDataUpdated(int ind) {
		getButton(ind).setText(data[ind].getDisplay());
		if (ind == getIndex()) {
			data[ind].position = Content.getCursorPosition();
			data[ind].scrollX = Content.getScrollX();
			data[ind].scrollY = Content.getScrollY();
			data[ind].applyTo(Content);
		}
	}

	@Override
	public void onClick(View v) {
		EditData data = (EditData) v.getTag();
		if (_ClickListener != null)
			_ClickListener.onEditDataClick(data);
		if (v != CurrentButton) setIndex(data.index);
	}

	public void setIndex(int pos) {
		if (size == 0) return;
		if (pos < 0) pos = 0;
		if (pos > size - 1) pos = size - 1;
		if (CurrentButton != null) {
			selectButton(CurrentButton, false);
			getCurrentEditData().loadFrom(Content);
		}
		CurrentButton = getButton(pos);
		selectButton(CurrentButton, true);
		CurrentButton.post(new Runnable() {
			@Override
			public void run() {
				int left = CurrentButton.getLeft();
				int right = CurrentButton.getRight() - ButtonScroller.getWidth();
				if (right > ButtonScroller.getScrollX())
					ButtonScroller.smoothScrollTo(right, 0);
				else if (left < ButtonScroller.getScrollX())
					ButtonScroller.smoothScrollTo(left, 0);
			}
		});
		final EditData cur = getCurrentEditData();
		cur.applyTo(Content);
		Content.setText(cur.cs, cur.length);
	}

	public void selectButton(AppCompatButton button, boolean selected) {
		if (button == null) return;
		if (selected) {
			button.setBackgroundColor(Content.getTheme().getLineNumberColor());
			button.setTextColor(Content.getTheme().getBackgroundColor());
		} else {
			button.setBackgroundColor(Content.getTheme().getBackgroundColor());
			button.setTextColor(Content.getTheme().getLineNumberColor());
		}
	}

	public AppCompatButton getButton(int ind) {
		return (AppCompatButton) ButtonLayout.getChildAt(ind);
	}

	public interface EditDataClickListener {
		void onEditDataClick(EditData data);
	}

	public static class EditData {
		public int scrollX, scrollY;
		public int position;
		public int index;
		public int length;
		public char[] cs;
		public boolean saved;
		private File file;
		private Class<? extends VLexer> lexer;

		public EditData(int ind) {
			scrollX = scrollY = position = 0;
			saved = false;
			file = null;
			lexer = VNullLexer.class;
			index = ind;
		}

		public EditData(int ind, VEdit edit) {
			this(ind);
			loadFrom(edit);
			onFileChange();
		}

		public void setText(char[] cs) {
			if (cs == null) return;
			setText(cs, cs.length);
		}

		public void setText(char[] cs, int length) {
			this.cs = cs;
			this.length = length;
		}

		public void loadFrom(VEdit edit) {
			scrollX = edit.getScrollX();
			scrollY = edit.getScrollY();
			position = edit.getCursorPosition();
			lexer = edit.getLexer().getClass();
			length = edit.getTextLength();
			cs = edit.getRawChars();
		}

		public void applyTo(VEdit edit) {
			edit.finishScrolling();
			edit.finishSelecting();
			try {
				edit.setLexer(getLexer().newInstance());
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
			edit.moveCursor(position);
			edit.setScrollX(scrollX);
			edit.setScrollY(scrollY);
		}

		public String getDisplay() {
			String ret = file == null ? UNTITLED : file.getName();
			if (saved) return ret;
			return "*" + ret;
		}

		public void setFile(File f) {
			this.file = f;
			onFileChange();
		}

		public File getFile() {
			return this.file;
		}

		private void onFileChange() {
			this.lexer = VNullLexer.class;
			if (this.file == null) return;
			String name = this.file.getName();
			int ind = name.lastIndexOf('.');
			if (ind == -1) return;
			name = name.substring(ind + 1).toLowerCase();
			switch (name) {
				case "java":
					lexer = VJavaLexer.class;
					break;
				case "js":
					lexer = VJavaScriptLexer.class;
					break;
				case "c":
				case "h":
					lexer = VCLexer.class;
					break;
				case "cpp":
				case "cxx":
				case "cc":
				case "c++":
				case "hpp":
				case "hxx":
					lexer = VCppLexer.class;
					break;
			}
		}

		public Class<? extends VLexer> getLexer() {
			return (G._LEXER_ID == 0) ? lexer : C.LEXERS[G._LEXER_ID];
		}
	}
}