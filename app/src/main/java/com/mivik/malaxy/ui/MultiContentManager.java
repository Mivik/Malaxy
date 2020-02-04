package com.mivik.malaxy.ui;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.HorizontalScrollView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.LinearLayoutCompat;
import com.mivik.malax.Malax;
import com.mivik.malaxy.Const;
import com.mivik.malaxy.G;
import com.mivik.malaxy.IO;
import com.mivik.medit.MEdit;
import com.mivik.medit.theme.MEditTheme;
import com.mivik.medit.theme.MEditThemeLight;
import com.mivik.mlexer.*;

import java.io.File;
import java.io.FileInputStream;

import static com.mivik.malax.BaseMalax.Cursor;

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
	private MEdit Content;
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
		Content = new MEdit(cx);
		setTheme(MEditThemeLight.getInstance());
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

	public MEdit getContent() {
		return Content;
	}

	public void setTheme(MEditTheme theme) {
		ButtonLayout.setBackgroundColor(theme.getBackgroundColor());
		Content.setTheme(theme);
		int ind = getIndex();
		for (int i = 0; i < size; i++)
			selectButton(getButton(i), i == ind);
	}

	public MEditTheme getTheme() {
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			button.setTextAlignment(TEXT_ALIGNMENT_CENTER);
		button.setAllCaps(false);
		button.setTag(data[pos]);
		button.setOnClickListener(this);
		button.setPadding(BUTTON_PADDING, 0, BUTTON_PADDING, 0);
		ButtonLayout.addView(button, pos);
		setIndex(pos);
		onTabSizeUpdated();
		if (pos == 1 && size == 2 && data[0].malax.length() == 0 && data[0].file == null)
			deleteTab(0);
	}

	public void onEditDataUpdated(int ind) {
		getButton(ind).setText(data[ind].getDisplay());
		if (ind == getIndex()) {
			data[ind].cursor = Content.getCursor();
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
		Content.setMalax(cur.malax);
		cur.applyTo(Content);
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
		public Cursor cursor;
		public int index;
		public Malax malax;
		public boolean saved;
		private File file;

		public EditData(int ind) {
			scrollX = scrollY = 0;
			cursor = new Cursor(0, 0);
			saved = false;
			file = null;
			malax = new Malax();
			index = ind;
		}

		public EditData(int ind, MEdit edit) {
			this(ind);
			loadFrom(edit);
			onFileChange();
		}

		public void setText(char[] cs) {
			if (cs == null) return;
			setText(cs, 0, cs.length);
		}

		public void setText(char[] cs, int off, int length) {
			malax.getMalax().setText(cs, off, length);
		}

		public void loadFrom(MEdit edit) {
			scrollX = edit.getScrollX();
			scrollY = edit.getScrollY();
			cursor = edit.getCursor();
			malax = edit.S;
		}

		public void applyTo(MEdit edit) {
			edit.finishScrolling();
			edit.finishSelecting();
			edit.moveCursor(cursor);
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
			MLexer lexer = malax.getLexer();
			Class<? extends MLexer> origin = lexer == null ? null : lexer.getClass();
			Class<? extends MLexer> cur = getNewLexer();
			try {
				if (origin != cur) malax.setLexer(cur.newInstance());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		private Class<? extends MLexer> getNewLexer() {
			Class<? extends MLexer> lexer = NullLexer.class;
			if (this.file == null) return lexer;
			String name = this.file.getName();
			int ind = name.lastIndexOf('.');
			if (ind == -1) return lexer;
			name = name.substring(ind + 1).toLowerCase();
			switch (name) {
				case "java":
					lexer = JavaLexer.class;
					break;
				case "js":
					lexer = JavaScriptLexer.class;
					break;
				case "c":
				case "h":
					lexer = CLexer.class;
					break;
				case "cpp":
				case "cxx":
				case "cc":
				case "c++":
				case "hpp":
				case "hxx":
					lexer = CppLexer.class;
					break;
				case "json":
					lexer = JSONLexer.class;
					break;
				case "xml":
				case "html":
					lexer = XMLLexer.class;
					break;
			}
			return lexer;
		}

		public Class<? extends MLexer> getLexer() {
			return (G._LEXER_ID == 0) ? malax.getLexer().getClass() : Const.LEXERS[G._LEXER_ID];
		}
	}
}