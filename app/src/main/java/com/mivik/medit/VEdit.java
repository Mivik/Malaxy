package com.mivik.medit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.OverScroller;
import com.mivik.malaxy.C;
import com.mivik.medit.theme.VEditTheme;
import com.mivik.medit.theme.VEditThemeDark;
import com.xsjiong.vlexer.VJavaLexer;
import com.xsjiong.vlexer.VLexer;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class VEdit extends View implements Runnable {
	// --------------------
	// -----Constants------
	// --------------------

	public static final int DOUBLE_CLICK_INTERVAL = 300, MERGE_ACTIONS_INTERVAL = 250, BLINK_INTERVAL = 700;
	public static final int DOUBLE_CLICK_RANGE = 40;
	public static final int LINE_NUMBER_SPLIT_WIDTH = 7;
	public static final int EXPAND_SIZE = 64;
	public static final int EMPTY_CHAR_WIDTH = 10;
	public static final int SCROLL_TO_CURSOR_EXTRA = 20;
	public static final short CHAR_SPACE = 32, CHAR_TAB = 9;
	public static final int EDIT_ACTION_STACK_SIZE = 64;
	public static final int E_EXPAND_SIZE = 256;


	// -----------------
	// -----Fields------
	// -----------------

	protected Paint ContentPaint, LineNumberPaint;
	protected Paint ColorPaint;
	protected float YOffset;
	protected float TextHeight;
	protected int ContentHeight;
	protected char[] S = new char[0];
	protected int[] E = new int[E_EXPAND_SIZE];
	private int _minFling, _touchSlop;
	private float _lastX, _lastY, _stX, _stY;
	private OverScroller Scroller;
	private VelocityTracker SpeedCalc;
	private boolean isDragging = false;
	protected int TABSpaceCount = 4;
	private int _YScrollRange;
	protected float LineNumberWidth;
	private int _maxOSX = 20, _maxOSY = 20;
	protected int _SStart = -1, _SEnd;
	private int _CursorLine = 1, _CursorColumn = 0;
	private float _CursorWidth = 2;
	private float _LinePaddingTop = 5, _LinePaddingBottom = 5;
	private float _ContentLeftPadding = 7;
	private int _TextLength;
	protected boolean Editable = true;
	protected VInputConnection InputConnection;
	protected boolean _ShowLineNumber = true;
	private float[] _CharWidths = new float[65536];
	private InputMethodManager _IMM;
	private long LastClickTime = 0;
	private float _lastClickX, _lastClickY;
	private int _ComposingStart = -1;
	protected VEditTheme _Theme = VEditThemeDark.getInstance();
	protected VLexer _Lexer = new VJavaLexer();
	protected Cursor _Cursor = new GlassCursor(this);
	private float _CursorHorizonOffset;
	private float _SStartHorizonOffset, _SEndHorizonOffset;
	protected int _SStartLine, _SEndLine;
	protected float LineHeight;
	protected byte _DraggingCursor = Cursor.TYPE_NONE;
	protected SlideBar _SlideBar;
	protected EditActionStack _EditActionStack = new EditActionStack(this, EDIT_ACTION_STACK_SIZE);
	private EditListener _EditListener = null;
	private boolean _BlinkCursor;
	private boolean _ShowCursorLine;
	private final byte[] _BlinkLock = new byte[0];
	private Handler _Handler = new Handler();
	private SelectListener _SelectListener;
	private ClipboardActionModeHelper _CBHelper;
	private boolean _CBEnabled = true;
	private ActionMode _ShowingActionMode;

	// -----------------------
	// -----Constructors------
	// -----------------------

	public VEdit(Context cx) {
		this(cx, null, 0);
	}

	public VEdit(Context cx, AttributeSet attr) {
		this(cx, attr, 0);
	}

	public VEdit(Context cx, AttributeSet attr, int style) {
		super(cx, attr, style);
		_lastClickX = _lastClickY = 0;
		setLayerType(View.LAYER_TYPE_HARDWARE, null);
		Scroller = new OverScroller(getContext());
		SpeedCalc = VelocityTracker.obtain();
		ViewConfiguration config = ViewConfiguration.get(cx);
		_minFling = config.getScaledMinimumFlingVelocity();
		_touchSlop = config.getScaledTouchSlop();
		_CBHelper = new ClipboardActionModeHelper(this);
		ContentPaint = new Paint();
		ContentPaint.setAntiAlias(true);
		ContentPaint.setDither(false);
		LineNumberPaint = new Paint();
		LineNumberPaint.setAntiAlias(true);
		LineNumberPaint.setDither(false);
		LineNumberPaint.setColor(Color.BLACK);
		LineNumberPaint.setTextAlign(Paint.Align.RIGHT);
		setTextSize(50);
		ContentPaint.setColor(Color.BLACK);
		ColorPaint = new Paint();
		ColorPaint.setAntiAlias(false);
		ColorPaint.setStyle(Paint.Style.FILL);
		ColorPaint.setDither(false);
		setFocusable(true);
		setFocusableInTouchMode(false);
		_IMM = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		E[0] = 2;
		E[1] = 0;
		E[2] = 1; // 来自我自己把全部删了之后的E信息
		_TextLength = 0;
		_SlideBar = new MaterialSlideBar(this);
		applyTheme();
		setBlinkCursor(true);
	}

	// ------------------
	// -----Methods------
	// ------------------

	public void onStartActionMode(ActionMode mode) {
		_ShowingActionMode = mode;
	}

	public void onHideActionMode() {
		_ShowingActionMode = null;
	}

	public int[] find(char[] cs) {
		final int T = _TextLength - cs.length;
		ArrayList<Integer> rret = new ArrayList<>();
		find:
		for (int i = 0, j; i <= T; i++) {
			if (!equal(i, cs)) continue find;
			rret.add(i);
		}
		int[] ret = new int[rret.size()];
		for (int i = 0; i < ret.length; i++) ret[i] = rret.get(i);
		rret = null;
		return ret;
	}

	public boolean equal(int st, char[] cmp) {
		for (int i = 0; i < cmp.length; i++) if (cmp[i] != S[st + i]) return false;
		return true;
	}

	public void setClipboardEnabled(boolean flag) {
		if (!(_CBEnabled = flag)) _CBHelper.hide();
	}

	public boolean isClipboardEnabled() {
		return _CBEnabled;
	}

	public boolean isEditable() {
		return Editable;
	}

	public void setSelectListener(SelectListener listener) {
		_SelectListener = listener;
	}

	public SelectListener getSelectListener() {
		return _SelectListener;
	}

	public void setSlideBar(SlideBar bar) {
		_SlideBar = bar;
		postInvalidate();
	}

	public SlideBar getSlideBar() {
		return _SlideBar;
	}

	public void setCursor(Cursor cursor) {
		_Cursor = cursor;
		postInvalidate();
	}

	public Cursor getCursor() {
		return _Cursor;
	}

	public float getCursorHorizonOffset() {
		return _CursorHorizonOffset;
	}

	public int getCursorLine() {
		return _CursorLine;
	}

	public int getCursorColumn() {
		return _CursorColumn;
	}

	public float getLineHeight() {
		return LineHeight;
	}

	public void setAutoParse(boolean flag) {
		_Lexer.setAutoParse(flag);
	}

	public boolean isAutoParse() {
		return _Lexer.isAutoParse();
	}

	public boolean isParsed() {
		return _Lexer.isParsed();
	}

	public void parseAll() {
		_Lexer.parseAll();
	}

	public void setBlinkCursor(boolean flag) {
		synchronized (_BlinkLock) {
			if (_BlinkCursor == flag) return;
			if (_BlinkCursor = flag)
				_Handler.post(this);
			else {
				_Handler.removeCallbacks(this);
				_Handler.post(new Runnable() {
					@Override
					public void run() {
						_ShowCursorLine = true;
					}
				});
			}
		}
	}

	public boolean isBlinkCursor() {
		return _BlinkCursor;
	}

	public void setEditListener(EditListener listener) {
		this._EditListener = listener;
	}

	public EditListener getEditListener() {
		return _EditListener;
	}

	public float getMaxScrollY() {
		return ContentHeight - getHeight();
	}

	public void selectAll() {
		setSelectionRange(0, _TextLength);
	}

	public boolean paste() {
		ClipData data = getClipboardManager().getPrimaryClip();
		if (data != null && data.getItemCount() > 0) {
			CharSequence s = data.getItemAt(0).coerceToText(getContext());
			char[] cs = new char[s.length()];
			for (int i = 0; i < cs.length; i++) cs[i] = s.charAt(i);
			commitChars(cs);
			return true;
		}
		return false;
	}

	public boolean cut() {
		if (isRangeSelecting()) {
			ClipboardManager manager = getClipboardManager();
			manager.setPrimaryClip(ClipData.newPlainText(null, getSelectedText()));
			int line = findLine(_SEnd);
			deleteChars(line, _SEnd - E[line], _SEnd - _SStart);
			finishSelecting();
			return true;
		}
		return false;
	}

	public boolean copy() {
		if (isRangeSelecting()) {
			ClipboardManager manager = getClipboardManager();
			manager.setPrimaryClip(ClipData.newPlainText(null, getSelectedText()));
			finishSelecting();
			return true;
		}
		return false;
	}

	public void deleteSelecting() {
		int line = findLine(_SEnd);
		deleteChars(line, _SEnd - E[line], _SEnd - _SStart);
		finishSelecting();
	}

	public void setLexer(VLexer lexer) {
		if (lexer == null) {
			_Lexer = null;
			return;
		}
		if (_Lexer != null && _Lexer.getClass() == lexer.getClass()) return;
		_Lexer = lexer;
		_Lexer.setText(S, S.length);
		postInvalidate();
	}

	public VLexer getLexer() {
		return _Lexer;
	}

	public void loadURL(String url) throws IOException {
		loadURL(new URL(url));
	}

	public void loadURL(URL url) throws IOException {
		loadStream(url.openStream());
	}

	public void loadFile(String filepath) throws IOException {
		loadFile(new File(filepath));
	}

	public void loadFile(File file) throws IOException {
		loadStream(new FileInputStream(file));
	}

	public void loadStream(InputStream stream) throws IOException {
		loadStream(stream, StandardCharsets.UTF_8);
	}

	public void loadStream(InputStream stream, Charset charset) throws IOException {
		byte[] buf = new byte[1024];
		int read;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((read = stream.read(buf)) != -1) out.write(buf, 0, read);
		stream.close();
		out.close();
		setText(new String(out.toByteArray(), charset));
	}

	public void setTypeface(Typeface typeface) {
		ContentPaint.setTypeface(typeface);
		LineNumberPaint.setTypeface(typeface);
		onFontChange();
	}

	public void setShowLineNumber(boolean flag) {
		_ShowLineNumber = flag;
		postInvalidate();
	}

	public boolean isShowLineNumber() {
		return _ShowLineNumber;
	}

	public void setEditable(boolean editable) {
		Editable = editable;
		if (Editable && _IMM != null)
			_IMM.restartInput(this);
		else hideIME();
		postInvalidate();
	}

	public void setContentLeftPadding(float padding) {
		_ContentLeftPadding = padding;
		postInvalidate();
	}

	public float getContentLeftPadding() {
		return _ContentLeftPadding;
	}

	public int getLineNumber() {
		return E[0] - 1;
	}

	public int getLineStart(int line) {
		return E[line];
	}

	public int getLineEnd(int line) {
		return E[line + 1] - 1;
	}

	public char[] getLineChars(int line) {
		char[] ret = new char[E[line + 1] - E[line] - 1];
		System.arraycopy(S, E[line], ret, 0, ret.length);
		return ret;
	}

	public String getLineString(int line) {
		return new String(getLineChars(line));
	}

	public void setTextAntiAlias(boolean flag) {
		ContentPaint.setAntiAlias(flag);
		postInvalidate();
	}

	// Recommend using "setScale" since "setTextSize" will clear the text width cache, which makes drawing slower
	public void setTextSize(int unit, float size) {
		setTextSize(TypedValue.applyDimension(unit, size, getContext().getResources().getDisplayMetrics()));
	}

	public void setTextSize(float size) {
		ContentPaint.setTextSize(size);
		LineNumberPaint.setTextSize(size);
		onFontChange();
	}

	public void setTheme(VEditTheme scheme) {
		this._Theme = scheme;
		applyTheme();
		postInvalidate();
	}

	public VEditTheme getTheme() {
		return _Theme;
	}

	public void setTABSpaceCount(int count) {
		TABSpaceCount = count;
		_CharWidths[CHAR_TAB] = TABSpaceCount * _CharWidths[CHAR_SPACE];
		postInvalidate();
	}

	public int getTABSpaceCount() {
		return TABSpaceCount;
	}

	public void setLinePadding(float top, float bottom) {
		_LinePaddingTop = top;
		_LinePaddingBottom = bottom;
		onFontChange();
		postInvalidate();
	}

	public float getLinePaddingTop() {
		return _LinePaddingTop;
	}

	public float getLinePaddingBottom() {
		return _LinePaddingBottom;
	}

	public void setText(char[] s, int length) {
		if (s == null) s = new char[0];
		this.S = s;
		_TextLength = length;
		if (Editable && _IMM != null)
			_IMM.restartInput(this);
		calculateEnters();
		_Lexer.setText(s);
		onLineChange();
		requestLayout();
		postInvalidate();
	}

	public void setText(String s) {
		setText(s == null ? new char[0] : s.toCharArray());
	}

	public void setText(char[] s) {
		if (s == null) s = new char[0];
		setText(s, s.length);
	}

	public int getSelectionStart() {
		return _SStart;
	}

	public int getSelectionEnd() {
		return _SEnd;
	}

	// return true if the start and the end of the selection has reserved
	public boolean setSelectionStart(int st) {
		boolean ret = false;
		if (st > _SEnd) {
			_SStart = _SEnd;
			_SEnd = st;
			ret = true;
		} else _SStart = st;
		onSelectionUpdate();
		postInvalidate();
		return ret;
	}

	public boolean setSelectionEnd(int en) {
		boolean ret = false;
		if (en < _SStart) {
			_SEnd = _SStart;
			_SStart = en;
			ret = true;
		} else _SEnd = en;
		onSelectionUpdate();
		postInvalidate();
		return ret;
	}

	public void setSelectionRange(int st, int en) {
		if (st > en) {
			int tmp = st;
			st = en;
			en = tmp;
		}
		_SStart = st;
		_SEnd = en;
		onSelectionUpdate();
		postInvalidate();
	}

	public void moveCursor(int pos) {
		if (pos > _TextLength) pos = _TextLength;
		_CursorLine = findLine(pos);
		_CursorColumn = pos - E[_CursorLine];
		onSelectionUpdate();
		postInvalidate();
	}

	public void moveCursor(int line, int column) {
		if (line > E[0] - 1) {
			line = E[0] - 1;
			column = E[E[0]] - E[E[0] - 1] - 1;
		} else if (column > E[line + 1] - E[line] - 1)
			column = E[line + 1] - E[line] - 1;
		_CursorLine = line;
		_CursorColumn = column;
		onSelectionUpdate();
		postInvalidate();
	}

	public void setMaxOverScroll(int x, int y) {
		_maxOSX = x;
		_maxOSY = y;
	}

	public int getMaxOverScrollX() {
		return _maxOSX;
	}

	public int getMaxOverScrollY() {
		return _maxOSY;
	}

	private boolean _fixScroll = true;

	public void setFixScroll(boolean flag) {
		_fixScroll = flag;
	}

	public boolean isFixScroll() {
		return _fixScroll;
	}

	private boolean _dragDirection;
	private int _flingFactor = 1000;

	public void setFlingFactor(int factor) {
		_flingFactor = factor;
	}

	public void setCursorWidth(float width) {
		_CursorWidth = width;
		postInvalidate();
	}

	public float getCursorWidth() {
		return _CursorWidth;
	}

	public int getLineLength(int line) {
		return E[line + 1] - E[line] - 1;
	}

	public int getTextLength() {
		return _TextLength;
	}

	public void showIME() {
		if (_IMM != null)
			_IMM.showSoftInput(this, 0);
	}

	public void hideIME() {
		if (_IMM != null && _IMM.isActive(this))
			_IMM.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	public void deleteChar() {
		deleteChar(_CursorLine, _CursorColumn);
	}

	public void deleteChar(int line, int column) {
		if (line == 1 && column == 0) return;
		EditAction action = new EditAction.DeleteCharAction(line, column, S[E[line] + column - 1]);
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public void _deleteChar() {
		int[] ret = _deleteChar(_CursorLine, _CursorColumn);
		_CursorLine = ret[0];
		_CursorColumn = ret[1];
		onSelectionUpdate();
	}

	public int[] _deleteChar(int line, int column) {
		if ((!Editable) || (line == 1 && column == 0)) return new int[] {line, column};
		final int pos = E[line] + column;

		if (_TextLength > pos)
			System.arraycopy(S, pos, S, pos - 1, _TextLength - pos);
		if (column == 0) {
			column = E[line] - E[line - 1] - 1;
			System.arraycopy(E, line + 1, E, line, E[0] - line);
			E[0]--;
			for (int i = line; i <= E[0]; i++) E[i]--;
			line--;
			onLineChange();
		} else {
			for (int i = line + 1; i <= E[0]; i++) E[i]--;
			column--;
		}
		_TextLength--;
		_Lexer.onTextReferenceUpdate(S, _TextLength);
		_Lexer.onDeleteChars(pos, 1);
		postInvalidate();
		return new int[] {line, column};
	}

	public boolean isRangeSelecting() {
		return _SStart != -1;
	}

	public void insertChar(char c) {
		insertChar(_CursorLine, _CursorColumn, c);
	}

	public void insertChar(int line, int column, char c) {
		EditAction action = new EditAction.InsertCharAction(line, column, c);
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public int[] _insertChar(int line, int column, char c) {
		if (!Editable) return new int[] {line, column};
		final int pos = E[line] + column;

		if (S.length <= _TextLength + 1) {
			char[] ns = new char[S.length + EXPAND_SIZE];
			System.arraycopy(S, 0, ns, 0, pos);
			if (pos != _TextLength) System.arraycopy(S, pos, ns, pos + 1, _TextLength - pos);
			S = ns;
			S[pos] = c;
			ns = null;
			// TODO Should GC Here?
			System.gc();
		} else {
			// 没办法用System.arraycopy，因为考虑到顺序，可能会覆盖
			for (int i = _TextLength; i >= pos; i--) S[i + 1] = S[i];
			S[pos] = c;
		}
		if (c == '\n') {
			// 理由同上，注意这是>不是>=
			if (E[0] + 1 == E.length) expandEArray();
			for (int i = E[0]; i > line; i--) E[i + 1] = E[i] + 1;
			E[0]++;
			E[line + 1] = pos + 1;
			line++;
			column = 0;
			onLineChange();
		} else {
			for (int i = E[0]; i > line; i--) E[i]++;
			column++;
		}
		_TextLength++;
		_Lexer.onTextReferenceUpdate(S, _TextLength);
		_Lexer.onInsertChars(pos, 1);
		postInvalidate();
		return new int[] {line, column};
	}

	public String getText(int st, int en) {
		return new String(S, st, en - st);
	}

	public char[] getRawChars() {
		return S;
	}

	public char[] getChars(int st, int en) {
		char[] ret = new char[en - st];
		System.arraycopy(S, st, ret, 0, ret.length);
		return ret;
	}

	public String getText() {
		return getText(0, _TextLength);
	}

	public char[] getChars() {
		return getChars(0, _TextLength);
	}

	public void makeLineVisible(int line) {
		float y = LineHeight * line - LineHeight;
		if (getScrollY() > y) {
			scrollTo(getScrollX(), (int) y);
			postInvalidate();
			return;
		}
		y += LineHeight - getHeight();
		if (getScrollY() < y) {
			scrollTo(getScrollX(), (int) Math.ceil(y));
			postInvalidate();
		}
	}

	public void makeCursorVisible(int line, int column) {
		int pos = E[line] + column;
		makeLineVisible(line);
		float sum = (_ShowLineNumber ? (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH) : 0) + _ContentLeftPadding;
		for (int i = E[line]; i < pos; i++)
			sum += _CharWidths[S[i]];
		if (sum - _CursorWidth / 2 < getScrollX()) {
			scrollTo((int) (sum - _CursorWidth / 2) - SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		} else if (sum + _CursorWidth / 2 > getScrollX() + getWidth()) {
			scrollTo((int) Math.ceil(sum + _CursorWidth / 2 - getWidth()) + SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		}
	}

	public void finishSelecting() {
		_SStart = -1;
		onSelectionUpdate();
		postInvalidate();
	}

	public int getPosition(int line, int column) {
		return E[line] + column;
	}

	public boolean isEmpty() {
		return E[0] == 1;
	}

	public int getCursorPosition() {
		if (isEmpty()) return 0;
		return E[_CursorLine] + _CursorColumn;
	}

	public void insertChars(char[] cs) {
		insertChars(_CursorLine, _CursorColumn, cs);
	}

	public void insertChars(int line, int column, char[] cs) {
		if (cs.length == 1) {
			insertChar(line, column, cs[0]);
			return;
		}
		EditAction action = new EditAction.InsertCharsAction(line, column, cs);
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public void _insertChars(char[] cs) {
		int[] ret = _insertChars(_CursorLine, _CursorColumn, cs);
		_CursorLine = ret[0];
		_CursorColumn = ret[1];
		calculateEnters();
		onSelectionUpdate();
	}

	public int[] _insertChars(int line, int column, char[] cs) {
		if (!Editable) return new int[] {line, column};
		if (cs.length == 1)
			return _insertChar(line, column, cs[0]);
		final int tl = cs.length;
		final int pos = E[line] + column;

		int nh = _TextLength + tl;
		if (nh > S.length) {
			char[] ns = new char[nh + EXPAND_SIZE];
			System.arraycopy(S, 0, ns, 0, pos);
			System.arraycopy(cs, 0, ns, pos, tl);
			if (pos != _TextLength) System.arraycopy(S, pos, ns, pos + tl, _TextLength - pos);
			S = ns;
			ns = null;
			System.gc();
		} else {
			for (int i = _TextLength - 1; i >= pos; i--) S[i + tl] = S[i];
			System.arraycopy(cs, 0, S, pos, tl);
		}
		_TextLength += tl;
		int tot = 0;
		int[] tmp = new int[EXPAND_SIZE];
		for (int i = 0; i < tl; i++)
			if (cs[i] == '\n') {
				if (++tot == tmp.length) {
					int[] tmp2 = new int[tmp.length + EXPAND_SIZE];
					System.arraycopy(tmp, 0, tmp2, 0, tmp.length);
					tmp = tmp2;
					tmp2 = null;
				}
				tmp[tot] = i + pos + 1;
			}
		nh = E[0] + tot + 1;
		if (nh > E.length) {
			int[] ne = new int[nh];
			System.arraycopy(E, 0, ne, 0, line + 1);
			System.arraycopy(tmp, 1, ne, line + 1, tot);
			System.arraycopy(E, line + 1, ne, line + tot + 1, E[0] - line);
			ne[0] = E[0] + tot;
			for (int i = line + tot + 1; i <= ne[0]; i++) ne[i] += tl;
			E = ne;
			ne = null;
		} else {
			for (int i = E[0]; i > line; i--) E[i + tot] = E[i] + tl;
			System.arraycopy(tmp, 1, E, line + 1, tot);
			E[0] += tot;
		}
		if (tot != 0) onLineChange();
		line += tot;
		if (tot == 0)
			column += tl;
		else
			column = pos + tl - E[line];
		_Lexer.onTextReferenceUpdate(S, _TextLength);
		_Lexer.onInsertChars(pos, cs.length);
		postInvalidate();
		return new int[] {line, column};
	}

	public void deleteChars(int count) {
		deleteChars(_CursorLine, _CursorColumn, count);
	}

	public void deleteChars(int line, int column, int count) {
		int pos = E[line] + column;
		if (count > pos) count = pos;
		EditAction action = new EditAction.DeleteCharsAction(line, column, getChars(pos - count, pos));
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public void _deleteChars(int count) {
		int[] ret = _deleteChars(_CursorLine, _CursorColumn, count);
		_CursorLine = ret[0];
		_CursorColumn = ret[1];
		onSelectionUpdate();
	}

	public int[] _deleteChars(int line, int column, int count) {
		if ((!Editable) || count == 0) return new int[] {line, column};
		if (count > _TextLength) {
			S = new char[0];
			E[0] = 2;
			E[1] = 0;
			E[2] = 1;
			_TextLength = 0;
		}
		int pos = E[line] + column;
		if (pos > _TextLength) pos = _TextLength;
		if (pos < count) count = pos;

		int tot = 0;
		for (int i = 1; i <= count; i++)
			if (S[pos - i] == '\n') tot++;
		if (_TextLength > pos)
			System.arraycopy(S, pos, S, pos - count, _TextLength - pos);
		_TextLength -= count;
		E[0] -= tot;
		for (int i = line - tot + 1; i <= E[0]; i++) E[i] = E[i + tot] - count;
		if (tot != 0) onLineChange();
		line -= tot;
		if (tot == 0)
			column -= count;
		else
			column = pos - count - E[line];
		_Lexer.onTextReferenceUpdate(S, _TextLength);
		_Lexer.onDeleteChars(pos, count);
		postInvalidate();
		return new int[] {line, column};
	}

	public void replace(int st, int en, char[] cs) {
		if (st > en) {
			int tmp = en;
			en = st;
			st = tmp;
		}
		int line = findLine(en);
		EditAction action = new EditAction.ReplaceAction(line, en - E[line], getChars(st, en), cs);
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public void replace(int st, int en, char[] cs, int cst, int cen) {
		if (st > en) {
			int tmp = en;
			en = st;
			st = tmp;
		}
		int line = findLine(en);
		char[] cn = new char[cen - cst];
		System.arraycopy(cs, cst, cn, 0, cn.length);
		EditAction action = new EditAction.ReplaceAction(line, en - E[line], getChars(st, en), cn);
		if (interceptEditAction(action)) return;
		_EditActionStack.addAction(action);
	}

	public void moveCursorRelative(int count) {
		count = Math.min(E[_CursorLine] + _CursorColumn + count, _TextLength);
		_CursorLine = findLine(count);
		_CursorColumn = count - E[_CursorLine];
		onSelectionUpdate();
	}

	public float getCharWidth(char c) {
		float ret = _CharWidths[c];
		if (ret == 0) {
			TMP2[0] = c;
			ret = ContentPaint.measureText(TMP2, 0, 1);
			if (ret < EMPTY_CHAR_WIDTH) ret = EMPTY_CHAR_WIDTH;
			_CharWidths[c] = ret;
		}
		return ret;
	}

	public void commitChar(char ch) {
		_ComposingStart = -1;
		if (isRangeSelecting()) {
			int ss = _SStart;
			finishSelecting();
			replace(ss, _SEnd, new char[] {ch});
		} else insertChar(ch);
	}

	public void commitChars(char[] cs) {
		_ComposingStart = -1;
		if (isRangeSelecting()) {
			int ss = _SStart;
			finishSelecting();
			replace(ss, _SEnd, cs);
		} else insertChars(cs);
	}

	public String getSelectedText() {
		if (!isRangeSelecting()) return null;
		return new String(S, _SStart, _SEnd - _SStart);
	}

	public void expandSelectionFrom(int pos) {
		if (pos == _TextLength) pos--;
		if (pos < 0 || pos > _TextLength) return;
		int st = pos, en = pos;
		for (; st >= 0 && isSelectableChar(S[st]); st--) ;
		for (; en < _TextLength && isSelectableChar(S[en]); en++) ;
		setSelectionRange(st + 1, en);
	}

	public int[] getCursorByPosition(float x, float y) {
		x -= _ContentLeftPadding;
		if (_ShowLineNumber) x -= (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH);
		int[] rret = new int[2];
		rret[0] = Math.min((int) Math.ceil(y / LineHeight), E[0] - 1);
		if (rret[0] < 1) rret[0] = 1;
		final int en = E[rret[0] + 1] - 1;
		int ret = E[rret[0]];
		for (float sum = -x; ret < en; ret++) {
			if ((sum += getCharWidth(S[ret])) >= 0) {
				if ((-(sum - getCharWidth(S[ret]))) > sum) // 是前面的更逼近一些
					ret++;
				break;
			}
		}
		rret[1] = ret - E[rret[0]];
		return rret;
	}

	public static boolean isSelectableChar(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.' || Character.isJavaIdentifierPart(c);
	}

	public void clearEditActions() {
		_EditActionStack.clear();
	}

	public boolean redo() {
		return _EditActionStack.redo();
	}

	public boolean undo() {
		return _EditActionStack.undo();
	}


	// --------------------------
	// -----Override Methods-----
	// --------------------------


	@Override
	public void run() {
		synchronized (_BlinkLock) {
			if (!_BlinkCursor) return;
			_ShowCursorLine = !_ShowCursorLine;
			postInvalidate();
			_Handler.postDelayed(this, BLINK_INTERVAL);
		}
	}

	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return processEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return processEvent(event);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		_DraggingCursor = Cursor.TYPE_NONE;
		_Cursor.recycle();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if ((!isRangeSelecting()) && h < oldh)
			makeCursorVisible(_CursorLine, _CursorColumn);
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return ContentHeight;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		SpeedCalc.addMovement(event);
		boolean s = super.onTouchEvent(event);
		if (_SlideBar.handleEvent(event)) return true;
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				_stX = _lastX = event.getX();
				_stY = _lastY = event.getY();
				_DraggingCursor = getDraggingCursor(_stX + getScrollX(), _stY + getScrollY());
				if (_DraggingCursor != Cursor.TYPE_NONE) {
					_stX += getScrollX();
					_stY += getScrollY();
				}
				if (!Scroller.isFinished())
					Scroller.abortAnimation();
				if (!isFocused())
					requestFocus();
				return true;
			case MotionEvent.ACTION_MOVE:
				float x = event.getX(), y = event.getY();
				if (_DraggingCursor != Cursor.TYPE_NONE) {
					int[] nc = getCursorByPosition(x + getScrollX() - _stX + _lastX, y + getScrollY() - _stY + _lastY);
					switch (_DraggingCursor) {
						case Cursor.TYPE_NORMAL: {
							moveCursor(nc[0], nc[1]);
							return true;
						}
						case Cursor.TYPE_LEFT: {
							if (setSelectionStart(E[nc[0]] + nc[1])) _DraggingCursor = Cursor.TYPE_RIGHT;
							makeCursorVisible(nc[0], nc[1]);
							return true;
						}
						case Cursor.TYPE_RIGHT: {
							if (setSelectionEnd(E[nc[0]] + nc[1])) _DraggingCursor = Cursor.TYPE_LEFT;
							makeCursorVisible(nc[0], nc[1]);
							return true;
						}
					}
				}
				if ((!isDragging) && (Math.abs(x - _stX) > _touchSlop || Math.abs(y - _stY) > _touchSlop)) {
					isDragging = true;
					if (_fixScroll)
						_dragDirection = Math.abs(x - _lastX) > Math.abs(y - _lastY);
				}
				if (isDragging) {
					int finalX = getScrollX(), finalY = getScrollY();
					if (_fixScroll) {
						if (_dragDirection) {
							finalX += (_lastX - x);
							// TODO 如果要改X边界记得这儿加上
							if (finalX < -_maxOSX) finalX = -_maxOSX;
						} else {
							finalY += (_lastY - y);
							if (finalY < -_maxOSY) finalY = -_maxOSY;
							else if (finalY > _YScrollRange + _maxOSY)
								finalY = _YScrollRange + _maxOSY;
						}
					} else {
						finalX += (_lastX - x);
						// TODO 如果要改X边界记得这儿加上
						if (finalX < -_maxOSX) finalX = -_maxOSX;
						finalY += (_lastY - y);
						if (finalY < -_maxOSY) finalY = -_maxOSY;
						else if (finalY > _YScrollRange + _maxOSY)
							finalY = _YScrollRange + _maxOSY;
					}
					scrollTo(finalX, finalY);
					postInvalidate();
				}
				_lastX = x;
				_lastY = y;
				return true;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (_DraggingCursor != Cursor.TYPE_NONE) {
					_DraggingCursor = Cursor.TYPE_NONE;
					return true;
				}
				SpeedCalc.computeCurrentVelocity(_flingFactor);
				if (!isDragging) {
					onClick(event.getX() + getScrollX(), event.getY() + getScrollY());
					performClick();
				} else {
					isDragging = false;
					int speedX = (int) SpeedCalc.getXVelocity();
					int speedY = (int) SpeedCalc.getYVelocity();
					if (Math.abs(speedX) <= _minFling) speedX = 0;
					if (Math.abs(speedY) <= _minFling) speedY = 0;
					if (_fixScroll) {
						if (_dragDirection) speedY = 0;
						else speedX = 0;
					}
					if (speedX != 0 || speedY != 0)
						Scroller.fling(getScrollX(), getScrollY(), -speedX, -speedY, -_maxOSX, Integer.MAX_VALUE, -_maxOSY, _YScrollRange + _maxOSY);
					else springBack();
					SpeedCalc.clear();
					postInvalidate();
				}
				return true;
		}
		return s;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		_YScrollRange = Math.max(ContentHeight - (bottom - top), 0);
	}

	@Override
	public void computeScroll() {
		if (Scroller.computeScrollOffset()) {
			int x = Scroller.getCurrX();
			int y = Scroller.getCurrY();
			scrollTo(x, y);
			postInvalidate();
		} else if (!isDragging && (getScrollX() < 0 || getScrollY() < 0 || getScrollY() > _YScrollRange)) { // TODO X边界还要改我
			springBack();
			postInvalidate();
		}
	}

	// 输入处理
	@Override
	public boolean onCheckIsTextEditor() {
		return true;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.imeOptions = EditorInfo.IME_NULL
				| EditorInfo.IME_FLAG_NO_ENTER_ACTION
				| EditorInfo.IME_FLAG_NO_FULLSCREEN
				| EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION;
		outAttrs.inputType = EditorInfo.TYPE_MASK_CLASS
				| EditorInfo.TYPE_CLASS_TEXT
				| EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
				| EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE;
		outAttrs.initialSelStart = _SStart;
		outAttrs.initialSelEnd = isRangeSelecting() ? -1 : _SEnd;
		if (InputConnection == null)
			InputConnection = new VInputConnection(this);
		return InputConnection;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) hideIME();
		super.setEnabled(enabled);
		if (enabled && Editable && _IMM != null) _IMM.restartInput(this);
	}

	// 绘制函数
	@Override
	protected void onDraw(Canvas canvas) {
		// Marks
		long st = 0;
		if (C.LOG_TIME) st = System.currentTimeMillis();
		final boolean showSelecting = isRangeSelecting();
		final boolean showCursor = (!showSelecting) && Editable;
		final float bottom = getScrollY() + getHeight() + YOffset;
		final int right = getScrollX() + getWidth();
		final float xo = (_ShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + _ContentLeftPadding;

		int line = Math.max((int) (getScrollY() / LineHeight) + 1, 1);
		float y = (line - 1) * LineHeight + YOffset + _LinePaddingTop;
		float XStart, wtmp, x;
		int i, en;
		int tot;
		if (_ShowLineNumber) {
			ColorPaint.setColor(_Theme.getSplitLineColor());
			canvas.drawRect(LineNumberWidth, getScrollY(), LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH, getScrollY() + getHeight(), ColorPaint);
		}
		int parseTot = _Lexer.findPart(E[line]);
		int parseTarget = _Lexer.DS[parseTot];
		float SStartLineEnd = -1;
		LineDraw:
		for (; line < E[0]; line++) {
			if (_ShowLineNumber)
				canvas.drawText(Integer.toString(line), LineNumberWidth, y, LineNumberPaint);
			if (showCursor && _CursorLine == line) {
				ColorPaint.setColor(_Theme.getSelectionColor());
				canvas.drawRect(xo - _ContentLeftPadding, y - YOffset - _LinePaddingTop, right, y + TextHeight - YOffset + _LinePaddingBottom, ColorPaint);
			}
			i = E[line];
			en = E[line + 1] - 1;
			XStart = xo;
			if (getScrollX() > XStart && i < _TextLength)
				while ((wtmp = XStart + getCharWidth(S[i])) < getScrollX()) {
					if (++i >= en) {
						if ((y += LineHeight) >= bottom) break LineDraw;
						continue LineDraw;
					}
					XStart = wtmp;
				}
			if (parseTot <= _Lexer.DS[0]) {
				while (i >= parseTarget && parseTot <= _Lexer.DS[0])
					parseTarget = _Lexer.DS[++parseTot];
				if (parseTot == _Lexer.DS[0] + 1) parseTarget = Integer.MAX_VALUE;
				if (parseTot != 0)
					ContentPaint.setColor(_Theme.getTypeColor(_Lexer.D[parseTot - 1]));
			}
			tot = 0;
			for (x = XStart; i < en && x <= right; i++) {
				if (i == parseTarget) {
					canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
					XStart = x;
					tot = 0;
					ContentPaint.setColor(_Theme.getTypeColor(_Lexer.D[parseTot]));
					++parseTot;
					if (parseTot <= _Lexer.DS[0]) parseTarget = _Lexer.DS[parseTot];
				}
				if ((TMP[tot] = S[i]) == '\t') {
					canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
					XStart = x;
					tot = 0;
					XStart += _CharWidths[CHAR_TAB];
					x += _CharWidths[CHAR_TAB];
				} else
					x += getCharWidth(TMP[tot++]);
			}
			canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
			if (showSelecting) {
				if (line == _SStartLine) SStartLineEnd = x;
				else if (line > _SStartLine && line < _SEndLine) {
					ColorPaint.setColor(_Theme.getSelectionColor());
					canvas.drawRect(xo, y - YOffset - _LinePaddingTop, x, y - YOffset + TextHeight + _LinePaddingBottom, ColorPaint);
				}
			}
			if ((y += LineHeight) >= bottom)
				break;
		}
		if (showCursor) {
			ColorPaint.setColor(_Theme.getCursorLineColor());
			ColorPaint.setStrokeWidth(_CursorWidth);
			float sty = LineHeight * _CursorLine;
			if (_ShowCursorLine)
				canvas.drawLine(xo + _CursorHorizonOffset, sty - LineHeight, xo + _CursorHorizonOffset, sty, ColorPaint);
			_Cursor.draw(canvas, xo + _CursorHorizonOffset, sty, Cursor.TYPE_NORMAL);
		} else if (showSelecting) {
			float sty = LineHeight * _SStartLine;
			if (_SStartLine == _SEndLine) {
				ColorPaint.setColor(_Theme.getSelectionColor());
				canvas.drawRect(xo + _SStartHorizonOffset, sty - LineHeight, xo + _SEndHorizonOffset, sty, ColorPaint);
			} else {
				float eny = LineHeight * _SEndLine;
				ColorPaint.setColor(_Theme.getSelectionColor());
				if (SStartLineEnd != -1)
					canvas.drawRect(xo + _SStartHorizonOffset, sty - LineHeight, SStartLineEnd, sty, ColorPaint);
				canvas.drawRect(xo, eny - LineHeight, xo + _SEndHorizonOffset, eny, ColorPaint);
			}
			_Cursor.draw(canvas, xo + _SStartHorizonOffset, sty, Cursor.TYPE_LEFT);
			_Cursor.draw(canvas, xo + _SEndHorizonOffset, LineHeight * _SEndLine, Cursor.TYPE_RIGHT);
		}
		_SlideBar.draw(canvas);
		if (C.LOG_TIME) {
			st = System.currentTimeMillis() - st;
			Log.i(C.T, "耗时：" + st);
		}
	}

	public void finishScrolling() {
		Scroller.forceFinished(true);
	}

	public void deleteSurrounding(int beforeLength, int afterLength) {
		if (isRangeSelecting())
			deleteSelecting();
		else {
			int pos = getCursorPosition() + afterLength;
			int line = findLine(pos);
			deleteChars(line, pos - E[line], afterLength + beforeLength);
		}
	}

	public int findLine(int pos) {
		int l = 1, r = E[0] - 1;
		int mid;
		while (l <= r) {
			mid = (l + r) >> 1;
			if (E[mid] <= pos)
				l = mid + 1;
			else
				r = mid - 1;
		}
		return r;
	}

	public void clearCharWidthCache() {
		Arrays.fill(_CharWidths, 0);
		_CharWidths[CHAR_TAB] = (_CharWidths[CHAR_SPACE] = ContentPaint.measureText(" ")) * TABSpaceCount;
	}

	// -------------------------
	// -----Private Methods-----
	// -------------------------

	public char getChar(int ind) {
		return S[ind];
	}

	protected boolean interceptEditAction(EditAction action) {
		if (_EditListener == null) return false;
		return _EditListener.onEdit(action);
	}

	private void applyTheme() {
		setBackgroundColor(_Theme.getBackgroundColor());
		_Cursor.setHeight(TextHeight);
		LineNumberPaint.setColor(_Theme.getLineNumberColor());
		_SlideBar.onSchemeChange();
	}

	private byte getDraggingCursor(float x, float y) {
		final float ori = x;
		x -= _ContentLeftPadding;
		if (_ShowLineNumber) x -= (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH);
		if (isRangeSelecting()) {
			if (_Cursor.isTouched(x - _SStartHorizonOffset, y - LineHeight * _SStartLine, Cursor.TYPE_LEFT)) {
				_lastX = ori;
				_lastY = LineHeight * _SStartLine - LineHeight * 0.5f;
				return Cursor.TYPE_LEFT;
			}
			if (_Cursor.isTouched(x - _SEndHorizonOffset, y - LineHeight * _SEndLine, Cursor.TYPE_RIGHT)) {
				_lastX = ori;
				_lastY = LineHeight * _SEndLine - LineHeight * 0.5f;
				return Cursor.TYPE_RIGHT;
			}
		} else if (_Cursor.isTouched(x - _CursorHorizonOffset, y - LineHeight * _CursorLine, Cursor.TYPE_NORMAL)) {
			_lastX = ori;
			_lastY = LineHeight * _CursorLine - LineHeight * 0.5f;
			return Cursor.TYPE_NORMAL;
		}
		return Cursor.TYPE_NONE;
	}

	private void calculateEnters() {
		E[E[0] = 1] = 0;
		for (int i = 0; i < _TextLength; i++) {
			if (S[i] == '\0') continue;
			if (S[i] == '\n') {
				if (++E[0] == E.length)
					expandEArray();
				E[E[0]] = i + 1;
			}
		}
		E[++E[0]] = _TextLength + 1;
	}

	protected boolean processEvent(KeyEvent event) {
		if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
		if (event.isCtrlPressed()) {
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_C:
					copy();
					break;
				case KeyEvent.KEYCODE_X:
					cut();
					break;
				case KeyEvent.KEYCODE_V:
					paste();
					break;
				case KeyEvent.KEYCODE_A:
					selectAll();
					break;
			}
			return true;
		}
		if (event.isPrintingKey()) {
			commitChar((char) event.getUnicodeChar(event.getMetaState()));
			return true;
		}
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_SPACE:
				commitChar(' ');
				break;
			case KeyEvent.KEYCODE_DEL:
				if (isRangeSelecting())
					deleteSelecting();
				else
					deleteChar();
				break;
			case KeyEvent.KEYCODE_ENTER:
				commitChar('\n');
				break;
			default:
				return false;
		}
		return true;
	}

	private void expandEArray() {
		int[] ne = new int[E.length + E_EXPAND_SIZE];
		System.arraycopy(E, 0, ne, 0, E.length);
		E = ne;
	}

	protected void onClick(float x, float y) {
		long time = System.currentTimeMillis();
		boolean dc = (time - LastClickTime <= DOUBLE_CLICK_INTERVAL) && (Math.abs(x - _lastClickX) <= DOUBLE_CLICK_RANGE) && (Math.abs(y - _lastClickY) <= DOUBLE_CLICK_RANGE);
		_lastClickX = x;
		_lastClickY = y;
		LastClickTime = time;
		int[] nc = getCursorByPosition(x, y);
		_CursorLine = nc[0];
		_CursorColumn = nc[1];
		_ComposingStart = -1;
		if (dc) expandSelectionFrom(E[nc[0]] + nc[1]);
		else finishSelecting();
		if (Editable && _IMM != null) {
			_IMM.viewClicked(this);
			_IMM.showSoftInput(this, 0);
//			_IMM.restartInput(this);
		}
		postInvalidate();
	}

	protected void onFontChange() {
		YOffset = -ContentPaint.ascent();
		TextHeight = ContentPaint.descent() + YOffset;
		LineHeight = TextHeight + _LinePaddingTop + _LinePaddingBottom;
		_Cursor.setHeight(TextHeight);
		clearCharWidthCache();
		onLineChange();
		requestLayout();
		postInvalidate();
	}

	protected void onLineChange() {
		ContentHeight = (int) (LineHeight * (E[0] - 1));
		_YScrollRange = Math.max(ContentHeight - getHeight(), 0);
		LineNumberWidth = LineNumberPaint.measureText("9") * ((int) Math.log10(E[0] - 1) + 1);
	}

	private void springBack() {
		Scroller.springBack(getScrollX(), getScrollY(), 0, Integer.MAX_VALUE, 0, _YScrollRange);
	}

	protected ClipboardManager getClipboardManager() {
		return (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
	}

	protected void onSelectionUpdate() {
		if (_SStart != _SEnd && _SStart != -1) {
			if (_CBEnabled) _CBHelper.show();
		} else {
			if (_ShowingActionMode != null) _ShowingActionMode.finish();
			else if (_CBEnabled) _CBHelper.hide();
		}
		if (_SelectListener != null) {
			if (_SStart == -1) {
				int pos = E[_CursorLine] + _CursorColumn;
				_SelectListener.onSelect(pos, pos);
			} else _SelectListener.onSelect(_SStart, _SEnd);
		}
		if (!isRangeSelecting()) {
			makeCursorVisible(_CursorLine, _CursorColumn);
			_CursorHorizonOffset = 0;
			int off = E[_CursorLine];
			int tar = off + _CursorColumn;
			for (; off < tar; off++) _CursorHorizonOffset += getCharWidth(S[off]);
		} else {
			_SStartHorizonOffset = 0;
			int off = E[_SStartLine = findLine(_SStart)];
			for (; off < _SStart; off++) _SStartHorizonOffset += getCharWidth(S[off]);
			_SEndHorizonOffset = 0;
			off = E[_SEndLine = findLine(_SEnd)];
			for (; off < _SEnd; off++) _SEndHorizonOffset += getCharWidth(S[off]);
			makeCursorVisible(_SEndLine, _SEnd - E[_SEndLine]);

		}
		if (Editable && _IMM != null) {
			int sst, sen;
			CursorAnchorInfo.Builder builder = new CursorAnchorInfo.Builder().setMatrix(null);
			if (isRangeSelecting()) {
				sst = _SStart;
				sen = _SEnd;
			} else {
				sst = sen = getCursorPosition();
				float top = TextHeight * (_CursorLine - 1);
				float xo = (_ShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + _ContentLeftPadding;
				builder.setInsertionMarkerLocation(xo + _CursorHorizonOffset, top, top + YOffset, top + TextHeight, CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION);
			}
			builder.setSelectionRange(sst, sen);
			_IMM.updateCursorAnchorInfo(this, builder.build());
			_IMM.updateSelection(this, sst, sen, -1, -1);
//			_IMM.restartInput(this);
		}
	}

	private void setComposingText(char[] cs) {
		if (_ComposingStart == -1) {
			_ComposingStart = getCursorPosition();
			insertChars(cs);
		} else
			replace(_ComposingStart, getCursorPosition(), cs);
		if (cs.length == 0)
			_ComposingStart = -1;
	}


	// --------------------------
	// -----Temporary Fields-----
	// --------------------------

	// TODO 还有64个字符都塞不满屏幕的情况！
	private char[] TMP = new char[64];
	private char[] TMP2 = new char[1];

	// -----------------------
	// -----Inner Classes-----
	// -----------------------

	private static class ClipboardActionModeHelper {
		private VEdit Content;
		private Context cx;
		private android.support.v7.view.ActionMode _ActionMode;

		public ClipboardActionModeHelper(VEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (Content._ShowingActionMode != null) return;
			if (_ActionMode == null)
				((AppCompatActivity) cx).startSupportActionMode(new android.support.v7.view.ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
						_ActionMode = mode;
						mode.setTitle(android.R.string.selectTextMode);
						TypedArray array = cx.getTheme().obtainStyledAttributes(new int[] {
								android.R.attr.actionModeSelectAllDrawable,
								android.R.attr.actionModeCutDrawable,
								android.R.attr.actionModeCopyDrawable,
								android.R.attr.actionModePasteDrawable,
						});
						menu.add(0, 0, 0, cx.getString(android.R.string.selectAll))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('a')
								.setIcon(array.getDrawable(0));
						menu.add(0, 1, 0, cx.getString(android.R.string.cut))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('x')
								.setIcon(array.getDrawable(1));
						menu.add(0, 2, 0, cx.getString(android.R.string.copy))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('c')
								.setIcon(array.getDrawable(2));
						menu.add(0, 3, 0, cx.getString(android.R.string.paste))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('v')
								.setIcon(array.getDrawable(3));
						array.recycle();
						return true;
					}

					@Override
					public boolean onPrepareActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(android.support.v7.view.ActionMode mode, MenuItem item) {
						switch (item.getItemId()) {
							case 0:
								Content.selectAll();
								break;
							case 1:
								Content.cut();
								mode.finish();
								break;
							case 2:
								Content.copy();
								mode.finish();
								break;
							case 3:
								Content.paste();
								mode.finish();
								break;
							default:
								return false;
						}
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode p1) {
						Content.finishSelecting();
						_ActionMode = null;
					}
				});
		}

		public void hide() {
			if (!(cx instanceof Activity)) return;
			if (_ActionMode != null) {
				_ActionMode.finish();
				_ActionMode = null;
			}
		}
	}

	public interface SelectListener {
		void onSelect(int st, int en);
	}

	public interface EditListener {
		boolean onEdit(EditAction action);
	}

	private static class VInputConnection implements InputConnection {
		private VEdit Q;

		public VInputConnection(VEdit parent) {
			Q = parent;
		}

		@Override
		public CharSequence getTextBeforeCursor(int n, int flags) {
			int cursor = Q.isRangeSelecting() ? Q._SStart : Q.getCursorPosition();
			int st = Math.max(cursor - n, 0);
			return new String(Q.S, st, cursor - st);
		}

		@Override
		public CharSequence getTextAfterCursor(int n, int flags) {
			int cursor = Q.isRangeSelecting() ? Q._SStart : Q.getCursorPosition();
			return new String(Q.S, cursor, Math.min(cursor + n, Q._TextLength) - cursor);
		}

		@Override
		public CharSequence getSelectedText(int flags) {
			if (!Q.isRangeSelecting()) return "";
			return new String(Q.S, Q._SStart, Q._SEnd - Q._SStart);
		}

		@Override
		public int getCursorCapsMode(int reqModes) {
			// TODO Fix Me Maybe
			return InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
		}

		@Override
		public boolean setComposingRegion(int start, int end) {
			if (start == end)
				Q._ComposingStart = -1;
			else
				Q._ComposingStart = start;
			return true;
		}

		@Override
		public boolean setComposingText(CharSequence text, int newCursorPosition) {
			char[] cs = new char[text.length()];
			for (int i = 0; i < cs.length; i++) cs[i] = text.charAt(i);
			Q.setComposingText(cs);
			return true;
		}

		@Override
		public boolean finishComposingText() {
			Q._ComposingStart = -1;
			return true;
		}

		@Override
		public boolean commitText(CharSequence text, int newCursorPosition) {
			char[] cs = new char[text.length()];
			for (int i = 0; i < cs.length; i++) cs[i] = text.charAt(i);
			Q.commitChars(cs);
			return true;
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			Q.processEvent(event);
			return true;
		}

		@Override
		public boolean setSelection(int start, int end) {
			if (start == end) {
				Q.finishSelecting();
				Q.moveCursor(start);
				return true;
			}
			Q._SStart = start;
			Q._SEnd = end;
			Q.postInvalidate();
			return true;
		}

		@Override
		public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
			// TODO Tough
			return null;
		}

		@Override
		public boolean deleteSurroundingText(int beforeLength, int afterLength) {
			Q.deleteSurrounding(beforeLength, afterLength);
			return true;
		}

		@Override
		public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
			deleteSurroundingText(beforeLength, afterLength);
			return true;
		}

		@Override
		public boolean commitCompletion(CompletionInfo text) {
			// TODO Ha?
			return false;
		}

		@Override
		public boolean commitCorrection(CorrectionInfo correctionInfo) {
			// TODO Ha?
			return false;
		}

		@Override
		public boolean performEditorAction(int editorAction) {
			// TODO Ha?
			return false;
		}

		@Override
		public boolean performContextMenuAction(int id) {
			switch (id) {
				case android.R.id.copy: {
					Q.copy();
					break;
				}
				case android.R.id.cut:
					Q.cut();
					break;
				case android.R.id.paste:
					Q.paste();
					break;
				case android.R.id.selectAll:
					Q.selectAll();
					break;
			}
			return true;
		}

		@Override
		public boolean beginBatchEdit() {
			return false;
		}

		@Override
		public boolean endBatchEdit() {
			return false;
		}

		@Override
		public boolean clearMetaKeyStates(int states) {
			return false;
		}

		@Override
		public boolean reportFullscreenMode(boolean enabled) {
			return false;
		}

		@Override
		public boolean performPrivateCommand(String action, Bundle data) {
			return false;
		}

		@Override
		public boolean requestCursorUpdates(int cursorUpdateMode) {
			return false;
		}

		@Override
		public Handler getHandler() {
			return null;
		}

		@Override
		public void closeConnection() {
			finishComposingText();
		}

		@Override
		public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
			return false;
		}
	}

	public static abstract class SlideBar {

		protected VEdit parent;

		public SlideBar(VEdit parent) {
			this.parent = parent;
		}

		abstract void onSchemeChange();

		abstract void draw(Canvas canvas);

		abstract boolean handleEvent(MotionEvent event);
	}

	public static class MaterialSlideBar extends SlideBar {
		public static final int EXPAND_WIDTH = 25, COLLAPSE_WIDTH = 20;
		public static final int HEIGHT = 100;

		private boolean expand;
		private Paint mp;
		private float startY;

		public MaterialSlideBar(VEdit parent) {
			super(parent);
			expand = false;
			mp = new Paint();
			mp.setStyle(Paint.Style.FILL);
			mp.setAntiAlias(false);
		}

		@Override
		public void onSchemeChange() {
			mp.setColor(parent._Theme.getSlideBarColor());
		}

		@Override
		public boolean handleEvent(MotionEvent event) {
			final float x = event.getX();
			final float y = event.getY();
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				expand = false;
				if ((parent.getWidth() - x) <= EXPAND_WIDTH) {
					startY = y - getBarStartY();
					expand = startY >= 0 && startY <= HEIGHT;
					return expand;
				}
				return false;
			}
			if (!expand) return false;
			switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_UP:
					float target = (y - startY) / (parent.getHeight() - HEIGHT);
					if (target < 0) target = 0;
					if (target > 1) target = 1;
					parent.scrollTo(parent.getScrollX(), (int) (target * parent.getMaxScrollY()));
					break;
			}
			return true;
		}

		private int getWidth() {
			return expand ? EXPAND_WIDTH : COLLAPSE_WIDTH;
		}

		public float getBarStartY() {
			return (float) (parent.getHeight() - HEIGHT) * parent.getScrollY() / parent.getMaxScrollY();
		}

		@Override
		public void draw(Canvas canvas) {
			final float start = getBarStartY() + +parent.getScrollY();
			int right = canvas.getWidth() + parent.getScrollX();
			canvas.drawRect(right - getWidth(), start, right, start + HEIGHT, mp);
		}
	}

	public static abstract class Cursor {
		public static final byte TYPE_NONE = -1, TYPE_NORMAL = 0, TYPE_LEFT = 1, TYPE_RIGHT = 2;

		protected VEdit P;

		public Cursor(VEdit parent) {
			this.P = parent;
		}

		public abstract void setHeight(float height);

		public abstract void draw(Canvas canvas, float x, float y, byte type);

		public abstract boolean isTouched(float x, float y, byte type);

		public abstract void recycle();
	}

	public static class GlassCursor extends Cursor {
		private float h, radius;
		private Paint mp;
		private Bitmap c0, c1, c2;

		public GlassCursor(VEdit parent) {
			super(parent);
			mp = new Paint();
			mp.setStyle(Paint.Style.FILL);
			mp.setAntiAlias(true);
		}

		@Override
		public void setHeight(float height) {
			h = height * 1.5f;
			radius = h * 0.4f;
			float yy = h - radius;
			float tmp = (float) Math.sqrt(yy * yy - radius * radius);
			float xx = tmp / yy * radius;
			yy = tmp / yy * tmp;
			float gradius = radius * 0.7f;
			Path path = new Path();
			path.moveTo(0, 0);
			path.lineTo(xx, yy);
			path.lineTo(-xx, yy);
			path.close();
			int ddd = (int) Math.ceil(radius * 2);
			c0 = Bitmap.createBitmap(ddd, (int) (h + 0.5), Bitmap.Config.ARGB_8888);
			c1 = Bitmap.createBitmap(ddd, ddd, Bitmap.Config.ARGB_8888);
			c2 = Bitmap.createBitmap(ddd, ddd, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(c0);
			canvas.translate(radius, 0);
			mp.setColor(P._Theme.getCursorColor());
			canvas.drawPath(path, mp);
			canvas.drawCircle(0, h - radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(0, h - radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getCursorGlassColor());
			canvas.drawCircle(0, h - radius, gradius, mp);

			canvas = new Canvas(c1);
			mp.setColor(P._Theme.getCursorColor());
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(radius, 0, radius * 2, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getCursorGlassColor());
			canvas.drawCircle(radius, radius, gradius, mp);

			canvas = new Canvas(c2);
			mp.setColor(P._Theme.getCursorColor());
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(0, 0, radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getCursorGlassColor());
			canvas.drawCircle(radius, radius, gradius, mp);
		}

		@Override
		public void draw(Canvas canvas, float x, float y, byte type) {
			if (isRecycled()) backRecycle();
			canvas.translate(x, y);
			switch (type) {
				case 0:
					canvas.drawBitmap(c0, -radius, 0, null);
					break;
				case 1:
					canvas.drawBitmap(c1, -radius * 2, 0, null);
					break;
				case 2:
					canvas.drawBitmap(c2, 0, 0, null);
					break;
			}
			canvas.translate(-x, -y);
		}

		@Override
		public boolean isTouched(float x, float y, byte type) {
			if (isRecycled()) backRecycle();
			Bitmap cur = null;
			switch (type) {
				case 0:
					cur = c0;
					x += radius;
					break;
				case 1:
					cur = c1;
					x += radius * 2;
					break;
				case 2:
					cur = c2;
					break;
			}
			if (cur == null) return false;
			int nx = (int) (x + 0.5);
			int ny = (int) (y + 0.5);
			if (nx < 0 || nx >= cur.getWidth() || ny < 0 || ny >= cur.getHeight()) return false;
			return cur.getPixel(nx, ny) != Color.TRANSPARENT;
		}

		private static void tryRecycle(Bitmap bitmap) {
			if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
		}

		public boolean isRecycled() {
			return c0 == null;
		}

		@Override
		public void recycle() {
			tryRecycle(c0);
			tryRecycle(c1);
			tryRecycle(c2);
			mp = null;
		}

		private void backRecycle() {
			mp = new Paint();
			mp.setStyle(Paint.Style.FILL);
			mp.setAntiAlias(true);
			setHeight(h / 1.5f);
		}
	}

	public interface EditAction {
		void redo(VEdit edit);

		void undo(VEdit edit);

		void recycle();

		class MergedAction implements EditAction {
			public static final int MERGE_BUFFER = 16;

			private EditAction[] actions;
			private int pos;

			public static MergedAction obtain(EditAction ori, EditAction ac) {
				if (ori instanceof MergedAction) {
					MergedAction ret = (MergedAction) ori;
					ret.append(ac);
					return ret;
				} else return new MergedAction(new EditAction[] {ori, ac});
			}

			public MergedAction(EditAction[] actions) {
				this.actions = actions;
				this.pos = actions.length;
			}

			public EditAction[] getActions() {
				return actions;
			}

			public void append(EditAction action) {
				if (pos == actions.length) {
					EditAction[] na = new EditAction[actions.length + MERGE_BUFFER];
					System.arraycopy(actions, 0, na, 0, actions.length);
					actions = na;
					na = null;
				}
				actions[pos++] = action;
			}

			@Override
			public void redo(VEdit edit) {
				for (int i = 0; i < pos; i++) actions[i].redo(edit);
			}

			@Override
			public void undo(VEdit edit) {
				for (int i = pos - 1; i >= 0; i--) actions[i].undo(edit);
			}

			@Override
			public void recycle() {
				actions = null;
			}
		}

		class ReplaceAction implements EditAction {
			private int line, column;
			private char[] origin;
			private char[] content;

			public ReplaceAction(int line, int column, char[] origin, char[] content) {
				this.line = line;
				this.column = column;
				this.origin = origin;
				this.content = content;
			}

			public char[] getOrigin() {
				return origin;
			}

			public char[] getContent() {
				return content;
			}

			public void setOrigin(char[] cs) {
				this.origin = cs;
			}

			public void setContent(char[] cs) {
				this.content = cs;
			}

			@Override
			public void redo(VEdit edit) {
				int[] ret = edit._deleteChars(line, column, origin.length);
				ret = edit._insertChars(ret[0], ret[1], content);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void undo(VEdit edit) {
				int column = edit.E[this.line] + this.column - origin.length + content.length;
				int line = edit.findLine(column);
				column -= edit.E[line];
				int[] ret = edit._deleteChars(line, column, content.length);
				ret = edit._insertChars(ret[0], ret[1], origin);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void recycle() {
				origin = null;
				content = null;
			}
		}

		class DeleteCharAction implements EditAction {
			private int line, column;
			private char ch;

			public DeleteCharAction(int line, int column, char c) {
				this.line = line;
				this.column = column;
				this.ch = c;
			}

			public char getChar() {
				return ch;
			}

			public void setChar(char c) {
				this.ch = c;
			}

			@Override
			public void redo(VEdit edit) {
				ch = edit.S[edit.E[line] + column - 1];
				int[] ret = edit._deleteChar(line, column);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void undo(VEdit edit) {
				int column = edit.E[this.line] + this.column - 1;
				int line = edit.findLine(column);
				int[] ret = edit._insertChar(line, column - edit.E[line], ch);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void recycle() {
			}
		}

		class DeleteCharsAction implements EditAction {
			private int line, column;
			private char[] content;

			public DeleteCharsAction(int line, int column, char[] content) {
				this.line = line;
				this.column = column;
				this.content = content;
			}

			public char[] getContent() {
				return content;
			}

			public void setContent(char[] cs) {
				this.content = cs;
			}

			@Override
			public void redo(VEdit edit) {
				int[] ret = edit._deleteChars(line, column, content.length);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void undo(VEdit edit) {
				int column = edit.E[this.line] + this.column - content.length;
				int line = edit.findLine(column);
				int[] ret = edit._insertChars(line, column - edit.E[line], content);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void recycle() {
				content = null;
			}
		}

		class InsertCharAction implements EditAction {
			private int line, column;
			private char ch;

			public InsertCharAction(int line, int column, char ch) {
				this.line = line;
				this.column = column;
				this.ch = ch;
			}

			public char getChar() {
				return ch;
			}

			public void setChar(char ch) {
				this.ch = ch;
			}

			@Override
			public void redo(VEdit edit) {
				int[] ret = edit._insertChar(line, column, ch);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void undo(VEdit edit) {
				int nColumn = edit.E[line] + column + 1;
				int nLine = edit.findLine(nColumn);
				nColumn -= edit.E[nLine];
				int[] ret = edit._deleteChar(nLine, nColumn);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void recycle() {
			}
		}

		class InsertCharsAction implements EditAction {
			private int line, column;
			private char[] content;

			public InsertCharsAction(int line, int column, char[] content) {
				this.line = line;
				this.column = column;
				this.content = content;
			}

			public char[] getContent() {
				return content;
			}

			public void setContent(char[] s) {
				content = s;
			}

			@Override
			public void redo(VEdit edit) {
				int[] ret = edit._insertChars(line, column, content);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void undo(VEdit edit) {
				int nColumn = edit.E[line] + column + content.length;
				int nLine = edit.findLine(nColumn);
				nColumn -= edit.E[nLine];
				int[] ret = edit._deleteChars(nLine, nColumn, content.length);
				edit.moveCursor(ret[0], ret[1]);
			}

			@Override
			public void recycle() {
				content = null;
			}
		}
	}

	public static class EditActionStack {
		private VEdit parent;
		private EditAction[] arr;
		private int _pos;
		private int _undoCount;
		private long LastActionTime = 0;

		public EditActionStack(VEdit parent) {
			this(parent, 64);
		}

		public EditActionStack(VEdit parent, int maxSize) {
			this.parent = parent;
			setMaxSize(maxSize);
		}

		public void setMaxSize(int size) {
			arr = new EditAction[size];
			_pos = _undoCount = 0;
		}

		public void clear() {
			_pos = _undoCount = 0;
		}

		public int getMaxSize() {
			return arr.length;
		}

		public void addAction(EditAction action) {
			long t = System.currentTimeMillis();
			long cur = t - LastActionTime;
			LastActionTime = t;
			if (cur <= MERGE_ACTIONS_INTERVAL) {
				EditAction lac = getLastAction();
				if (lac != null) {
					setLastAction(EditAction.MergedAction.obtain(lac, action));
					action.redo(parent);
					return;
				}
			}
			if (arr[_pos] != null) arr[_pos].recycle();
			arr[_pos++] = action;
			action.redo(parent);
			if (_pos == arr.length) _pos = 0;
		}

		public boolean undo() {
			if (_undoCount >= arr.length) return false;
			if (_pos == 0) _pos = arr.length;
			_pos--;
			if (arr[_pos] == null) {
				if (++_pos == arr.length) _pos = 0;
				return false;
			}
			arr[_pos].undo(parent);
			_undoCount++;
			return true;
		}

		public boolean redo() {
			if (_undoCount == 0) return false;
			if (arr[_pos] == null) return false;
			arr[_pos].redo(parent);
			if (++_pos == arr.length) _pos = 0;
			_undoCount--;
			return true;
		}

		public EditAction getLastAction() {
			if (_undoCount >= arr.length) return null;
			int pp = _pos;
			if (pp == 0) pp = arr.length;
			return arr[pp - 1];
		}

		public void setLastAction(EditAction action) {
			if (_undoCount >= arr.length) return;
			int pp = _pos;
			if (pp == 0) pp = arr.length;
			arr[pp - 1] = action;
		}
	}
}