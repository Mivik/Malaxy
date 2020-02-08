package com.mivik.medit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.OverScroller;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import com.mivik.malax.LineManager;
import com.mivik.malax.Malax;
import com.mivik.malax.WrappedEditable;
import com.mivik.medit.theme.MEditTheme;
import com.mivik.medit.theme.MEditThemeDark;
import com.mivik.mlexer.CursorWrapper;
import com.mivik.mlexer.MLexer;
import com.mivik.mlexer.RangeSelection;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.mivik.malax.BaseMalax.Cursor;

public class MEdit extends View implements
		SplitLineManager.UpdateListener,
		ViewTreeObserver.OnGlobalLayoutListener,
		Runnable,
		WrappedEditable.CursorListener<Cursor> {
	// --------------------
	// -----Constants------
	// --------------------

	public static final int DOUBLE_CLICK_INTERVAL = 500, BLINK_INTERVAL = 700;
	public static final int DOUBLE_CLICK_RANGE = 40;
	public static final int LINE_NUMBER_SPLIT_WIDTH = 7;
	public static final int EMPTY_CHAR_WIDTH = 10;
	public static final int SCROLL_TO_CURSOR_EXTRA = 20;
	public static final short CHAR_SPACE = 32, CHAR_TAB = 9;


	// -----------------
	// -----Fields------
	// -----------------

	protected Paint ContentPaint, LineNumberPaint;
	protected Paint ColorPaint;
	protected float YOffset;
	protected float TextHeight;
	protected int ContentHeight;
	protected Cursor C = new Cursor(0, 0);
	private Cursor DDC;
	private Cursor DDSBegin, DDSEnd;
	private boolean RS = false;
	public Malax S;
	public LineManager L;
	private int _minFling, _touchSlop;
	private float _lastX, _lastY, _stX, _stY;
	private OverScroller Scroller;
	private VelocityTracker SpeedCalc;
	private boolean isDragging = false;
	protected int TABSpaceCount = 4;
	private int _YScrollRange;
	protected float LineNumberWidth;
	private int _maxOSX = 20, _maxOSY = 20;
	protected RangeSelection<Cursor> _S = new RangeSelection<>(new Cursor(0, 0), new Cursor(0, 0));
	private float _CursorWidth = 2;
	private float _LinePaddingTop = 5, _LinePaddingBottom = 5;
	private float _ContentLeftPadding = 7;
	protected boolean _Editable = true;
	protected MInputConnection InputConnection;
	protected boolean _ShowLineNumber = true;
	private float[] _CharWidths = new float[65536];
	private InputMethodManager _IMM;
	private long LastClickTime = 0;
	private float _lastClickX, _lastClickY;
	private Cursor _ComposingStart = null;
	protected MEditTheme _Theme = MEditThemeDark.getInstance();
	protected MLexer _Lexer;
	protected Indicator _Indicator = new GlassIndicator(this);
	protected float LineHeight;
	protected byte _DraggingCursor = Indicator.TYPE_NONE;
	protected SlideBar _SlideBar = new MaterialSlideBar(this);
	private boolean _BlinkCursor;
	private boolean _CurrentlyShowCursorLine;
	private boolean _ShowCursorLine = true;
	private final byte[] _BlinkLock = new byte[0];
	private Handler _Handler = new Handler();
	private SelectListener _SelectListener;
	private ClipboardActionModeHelper _CBHelper;
	private boolean _CBEnabled = true;
	private ActionMode _ShowingActionMode;
	private final Set<WrappedEditable.EditActionListener> _EditActionListeners = new HashSet<>();
	private EventHandler H;
	private boolean _HighlightLine = true;
	private final SplitLineManager SL = new SplitLineManager(this);

	// -----------------------
	// -----Constructors------
	// -----------------------

	public MEdit(Context cx) {
		this(cx, null, 0);
	}

	public MEdit(Context cx, AttributeSet attr) {
		this(cx, attr, 0);
	}

	public MEdit(Context cx, AttributeSet attr, int style) {
		super(cx, attr, style);
		getViewTreeObserver().addOnGlobalLayoutListener(this);
		SL.setUpdateListener(this);
		setSplitLineEnabled(false);
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
		setMalax(new Malax());
		setTextSize(50);
		ContentPaint.setColor(Color.BLACK);
		ColorPaint = new Paint();
		ColorPaint.setAntiAlias(false);
		ColorPaint.setStyle(Paint.Style.FILL);
		ColorPaint.setDither(false);
		setFocusable(true);
		setFocusableInTouchMode(false);
		_IMM = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		applyTheme();
		setBlinkCursor(true);
	}

	// ------------------
	// -----Methods------
	// ------------------

	public float getLeftOfLine() {
		return (_ShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + _ContentLeftPadding;
	}

	public float getLineWidth() {
		return getWidth() - getLeftOfLine();
	}

	public void setSplitLineEnabled(boolean flag) {
		SL.setEnabled(flag);
		setScrollX(0);
		postInvalidate();
	}

	public boolean isSplitLineEnabled() {
		return SL.isEnabled();
	}

	public void setHighlightLine(boolean flag) {
		this._HighlightLine = flag;
	}

	public boolean isHighlightLine() {
		return _HighlightLine;
	}

	public void setEventHandler(EventHandler handler) {
		this.H = handler;
	}

	public EventHandler getEventHandler() {
		return this.H;
	}

	public boolean addEditActionListener(WrappedEditable.EditActionListener listener) {
		boolean ret;
		synchronized (_EditActionListeners) {
			ret = _EditActionListeners.add(listener);
		}
		S.addEditActionListener(listener);
		return ret;
	}

	public boolean removeEditActionListener(WrappedEditable.EditActionListener listener) {
		boolean ret;
		synchronized (_EditActionListeners) {
			ret = _EditActionListeners.remove(listener);
		}
		S.removeEditActionListener(listener);
		return ret;
	}

	public void clearEditActionListeners() {
		_EditActionListeners.clear();
	}

	public void setMalax(Malax malax) {
		S = malax;
		S.setCursorListener(this);
		synchronized (_EditActionListeners) {
			for (WrappedEditable.EditActionListener one : _EditActionListeners)
				S.addEditActionListener(one);
		}
		L = S.getLineManager();
		_S = new RangeSelection<>(S, 0, 0);
		_Lexer = S.getLexer();
		if (_Lexer == null) ContentPaint.setColor(_Theme.getTypeColor(MLexer.TYPE_PURE));
		moveCursor(S.getBeginCursor());
		if (InputConnection != null) InputConnection.onUpdate();
		S.setContentChangeListener(SL);
		SL.onUpdate();
		onLineChange();
		postInvalidate();
	}

	public void onStartActionMode(ActionMode mode) {
		_ShowingActionMode = mode;
	}

	public void onHideActionMode() {
		_ShowingActionMode = null;
	}

	public Cursor[] find(char[] cs) {
		final int T = S.length() - cs.length;
		ArrayList<Cursor> ret = new ArrayList<>();
		Cursor x = S.getBeginCursor();
		for (int i = 0; i <= T; i++, S.moveForward(x))
			if (equal(x, cs)) ret.add(x.clone());
		return ret.toArray(new Cursor[0]);
	}

	public boolean equal(Cursor st, char[] cmp) {
		Cursor x = st.clone();
		for (int i = 0; i < cmp.length; i++) {
			char c = S.charAt(x);
			if (cmp[i] != c) return false;
			S.moveForward(x);
		}
		return true;
	}

	public void setClipboardEnabled(boolean flag) {
		if (!(_CBEnabled = flag)) _CBHelper.hide();
	}

	public boolean isClipboardEnabled() {
		return _CBEnabled;
	}

	public boolean isEditable() {
		return _Editable;
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

	public void setIndicator(Indicator indicator) {
		_Indicator = indicator;
		_Indicator.setHeight(TextHeight);
		postInvalidate();
	}

	public Indicator getIndicator() {
		return _Indicator;
	}

	public float getLineHeight() {
		return LineHeight;
	}

	public int getContentHeight() {
		return ContentHeight;
	}

	public boolean isParsed() {
		if (_Lexer == null) return true;
		return _Lexer.isParsed();
	}

	public void parseAll() {
		if (_Lexer == null) return;
		_Lexer.parseAll();
	}

	public void setShowCursorLine(boolean flag) {
		this._ShowCursorLine = false;
	}

	public boolean isShowCursorLine() {
		return _ShowCursorLine;
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
						_CurrentlyShowCursorLine = true;
					}
				});
			}
		}
	}

	public boolean isBlinkCursor() {
		return _BlinkCursor;
	}

	public float getMaxScrollY() {
		return ContentHeight - getHeight();
	}

	public void selectAll() {
		setSelectionRange(S.getFullRangeSelection());
	}

	public boolean paste() {
		if (!_Editable) return false;
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
		if (!_Editable) return false;
		if (isRangeSelecting()) {
			ClipboardManager manager = getClipboardManager();
			manager.setPrimaryClip(ClipData.newPlainText(null, getSelectedText()));
			deleteSelecting();
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
		if (!_Editable) return;
		S.delete(getSelectionRange());
		finishSelecting();
	}

	public void setLexer(MLexer lexer) {
		S.setLexer(lexer);
		_Lexer = S.getLexer();
		if (_Lexer == null) ContentPaint.setColor(_Theme.getTypeColor(MLexer.TYPE_PURE));
		postInvalidate();
	}

	public MLexer getLexer() {
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
		loadStream(stream, Charset.defaultCharset());
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
		_Editable = editable;
		if (_Editable && _IMM != null)
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
		return L.size();
	}

	public int getLineStart(int line) {
		return L.E[line];
	}

	public int getLineEnd(int line) {
		return L.E[line + 1] - 1;
	}

	public char[] getLineChars(int line) {
		char[] ret = new char[L.E[line + 1] - L.E[line] - 1];
		System.arraycopy(S, L.E[line], ret, 0, ret.length);
		return ret;
	}

	public String getLineString(int line) {
		return new String(getLineChars(line));
	}

	public void setTextAntiAlias(boolean flag) {
		ContentPaint.setAntiAlias(flag);
		postInvalidate();
	}

	public float getTextSize() {
		return ContentPaint.getTextSize();
	}

	public void setTextSize(int unit, float size) {
		setTextSize(TypedValue.applyDimension(unit, size, getContext().getResources().getDisplayMetrics()));
	}

	public void setTextSize(float size) {
		ContentPaint.setTextSize(size);
		LineNumberPaint.setTextSize(size);
		onFontChange();
	}

	public void setTheme(MEditTheme scheme) {
		this._Theme = scheme;
		applyTheme();
		postInvalidate();
	}

	public MEditTheme getTheme() {
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

	public void setText(char[] s, int off, int length) {
		S.setText(s, off, length);
		if (_Editable && _IMM != null)
			_IMM.restartInput(this);
		requestLayout();
		postInvalidate();
	}

	public void setText(String s) {
		setText(s == null ? new char[0] : s.toCharArray());
	}

	public void setText(char[] s) {
		if (s == null) s = new char[0];
		setText(s, 0, s.length);
	}

	public RangeSelection<Cursor> getSelectionRange() {
		return _S.clone();
	}

	// return true if the start and the end of the selection has reserved
	public boolean setSelectionStart(Cursor st) {
		boolean ret = false;
		if (st.compareTo(_S.end) > 0) {
			_S.begin = _S.end;
			_S.end = st.clone();
			ret = true;
		} else _S.begin = st.clone();
		onSelectionUpdate();
		postInvalidate();
		return ret;
	}

	public Cursor getFirstVisibleCursor() {
		Cursor ret = new Cursor(0, 0);
		ret.line = getFirstVisibleLine();
		ret.column = getFirstVisibleColumn(ret.line);
		return ret;
	}

	public int getFirstVisibleLine() {
		return Math.max((int) (getScrollY() / LineHeight), 0);
	}

	public int getFirstVisibleColumn(int x) {
		int len = L.getTrimmed(x);
		char[] cs = S.getRawChars()[x];
		int sum = 0, i;
		final int sx = getScrollX();
		if (sx == 0) return 0;
		for (i = 0; i < len; i++)
			if ((sum += getCharWidth(cs[i])) >= sx) break;
		return i;
	}

	public boolean setSelectionEnd(Cursor en) {
		boolean ret = false;
		if (en.compareTo(_S.begin) < 0) {
			_S.end = _S.begin;
			_S.begin = en.clone();
			ret = true;
		} else _S.end = en.clone();
		onSelectionUpdate();
		postInvalidate();
		return ret;
	}

	public void setSelectionRange(RangeSelection<Cursor> sel) {
		RS = true;
		_S.set(sel);
		onSelectionUpdate();
		postInvalidate();
	}

	public void moveCursor(Cursor x) {
		C.set(x);
		DDC = SL.getDisplayCursor(C);
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
		return L.E[line + 1] - L.E[line] - 1;
	}

	public int getTextLength() {
		return S.length();
	}

	public void showIME() {
		if (_IMM != null)
			_IMM.showSoftInput(this, 0);
	}

	public void hideIME() {
		if (_IMM != null && _IMM.isActive(this))
			_IMM.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	public boolean isRangeSelecting() {
		return RS;
	}

	public String getText(int st, int en) {
		return S.substring(new RangeSelection<>(S, st, en));
	}

	public char[][] getRawChars() {
		return S.getLines();
	}

	public char[] getChars(int st, int en) {
		char[] ret = new char[en - st];
		System.arraycopy(S, st, ret, 0, ret.length);
		return ret;
	}

	public String getText() {
		return getText(0, S.length());
	}

	public char[] getChars() {
		return getChars(0, S.length());
	}

	public void makeLineVisible(int line) {
		float y = LineHeight * SL.getLineDisplayStart(line);
		if (getScrollY() > y) {
			finishScrolling();
			scrollTo(getScrollX(), (int) y);
			postInvalidate();
			return;
		}
		y += LineHeight - getHeight();
		if (getScrollY() < y) {
			finishScrolling();
			scrollTo(getScrollX(), (int) Math.ceil(y));
			postInvalidate();
		}
	}

	public void makeCursorFirstVisible(Cursor x) {
		char[] cs = S.getRawChars()[x.line];
		int sum = 0;
		for (int i = 0; i < x.column; i++) sum += getCharWidth(cs[i]);
		scrollTo(sum, (int) (x.line * LineHeight));
	}

	public void makeCursorVisible(Cursor x) {
		if (getHeight() == 0) return;
		makeLineVisible(x.line);
		float sum = (_ShowLineNumber ? (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH) : 0) + _ContentLeftPadding + x.column;
		if (sum - _CursorWidth / 2 < getScrollX()) {
			finishScrolling();
			scrollTo((int) (sum - _CursorWidth / 2) - SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		} else if (sum + _CursorWidth / 2 > getScrollX() + getWidth()) {
			finishScrolling();
			scrollTo((int) Math.ceil(sum + _CursorWidth / 2 - getWidth()) + SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		}
	}

	public void finishSelecting() {
		RS = false;
		onSelectionUpdate();
		postInvalidate();
	}

	public int getPosition(int line, int column) {
		return L.E[line] + column;
	}

	public boolean isEmpty() {
		return L.empty();
	}

	public Cursor getCursor() {
		return C.clone();
	}

	public int getCursorPosition() {
		return S.Cursor2Index(C);
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
		_ComposingStart = null;
		if (isRangeSelecting()) {
			S.replace(getSelectionRange(), new char[]{ch});
			finishSelecting();
		} else S.insert(C, ch);
	}

	public void commitChars(char[] cs) {
		_ComposingStart = null;
		if (isRangeSelecting()) {
			S.replace(getSelectionRange(), cs);
			finishSelecting();
		} else S.insert(C, cs);
	}

	public String getSelectedText() {
		if (!isRangeSelecting()) return null;
		return S.substring(getSelectionRange());
	}

	public void expandSelectionFrom(Cursor x) {
		if (S.length() == 0) return;
		final int len = S.length();
		if (S.eof(x)) S.moveBack(x);
		CursorWrapper<Cursor> st = new CursorWrapper<>(S, x);
		CursorWrapper<Cursor> en = new CursorWrapper<>(S, x);
		while (true) {
			if (!isSelectableChar(st.get())) {
				st.moveForward();
				break;
			}
			if (!st.moveBack()) break;
		}
		for (; en.ind < len && isSelectableChar(en.get()); en.moveForward()) ;
		if (st.cursor.compareTo(en.cursor) > 0)
			setSelectionRange(new RangeSelection<>(en.cursor, st.cursor));
		else
			setSelectionRange(new RangeSelection<>(st.cursor, en.cursor));
	}

	public Cursor getCursorByPosition(float x, float y) {
		return SL.getOriginalCursor(new Cursor(Math.max((int) (y / LineHeight), 0), (int) (x - getLeftOfLine())));
	}

	public static boolean isSelectableChar(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.' || Character.isJavaIdentifierPart(c);
	}

	public boolean redo() {
		return S.redo();
	}

	public boolean undo() {
		return S.undo();
	}


	// --------------------------
	// -----Override Methods-----
	// --------------------------

	@Override
	public void onUpdate() {
		DDC = SL.getDisplayCursor(C);
		DDSBegin = SL.getDisplayCursor(_S.begin);
		DDSEnd = SL.getDisplayCursor(_S.end);
		onLineChange();
	}

	@Override
	public void scrollTo(int x, int y) {
		if (SL.isEnabled()) x = 0;
		super.scrollTo(x, y);
	}

	@Override
	public void onGlobalLayout() {
		SL.onUpdate();
	}

	@Override
	public void onCursorMoved(Cursor cursor) {
		moveCursor(cursor);
		postInvalidate();
	}

	@Override
	public void run() {
		if (!_ShowCursorLine) return;
		synchronized (_BlinkLock) {
			if (!_BlinkCursor) return;
			_CurrentlyShowCursorLine = !_CurrentlyShowCursorLine;
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
		_DraggingCursor = Indicator.TYPE_NONE;
		_Indicator.recycle();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if ((!isRangeSelecting()) && h < oldh)
			makeCursorVisible(C);
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return ContentHeight;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		SpeedCalc.addMovement(event);
		if (_SlideBar.handleEvent(event)) {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) finishScrolling();
			return true;
		}
		if (H != null && H.handleEvent(this, event)) return true;
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				_stX = _lastX = event.getX();
				_stY = _lastY = event.getY();
				_DraggingCursor = getDraggingCursor(_stX + getScrollX(), _stY + getScrollY());
				if (_DraggingCursor != Indicator.TYPE_NONE) {
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
				if (_DraggingCursor != Indicator.TYPE_NONE) {
					Cursor nc = getCursorByPosition(x + getScrollX() - _stX + _lastX, y + getScrollY() - _stY + _lastY);
					switch (_DraggingCursor) {
						case Indicator.TYPE_NORMAL: {
							moveCursor(nc);
							return true;
						}
						case Indicator.TYPE_LEFT: {
							if (setSelectionStart(nc)) _DraggingCursor = Indicator.TYPE_RIGHT;
							makeCursorVisible(nc);
							return true;
						}
						case Indicator.TYPE_RIGHT: {
							if (setSelectionEnd(nc)) _DraggingCursor = Indicator.TYPE_LEFT;
							makeCursorVisible(nc);
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
				if (_DraggingCursor != Indicator.TYPE_NONE) {
					_DraggingCursor = Indicator.TYPE_NONE;
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
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
				return true;
			default:
				return super.onTouchEvent(event);
		}
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
		if (RS) {
			outAttrs.initialSelStart = S.Cursor2Index(_S.begin);
			outAttrs.initialSelEnd = S.Cursor2Index(_S.end);
		} else
			outAttrs.initialSelStart = outAttrs.initialSelEnd = S.Cursor2Index(C);
		if (InputConnection == null)
			InputConnection = new MInputConnection(this);
		return InputConnection;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) hideIME();
		super.setEnabled(enabled);
		if (enabled && _Editable && _IMM != null) _IMM.restartInput(this);
	}

	// 绘制函数
	@Override
	protected void onDraw(Canvas canvas) {
		// Marks
		final boolean showSelecting = isRangeSelecting();
		final boolean showCursor = (!showSelecting);
		final float bottom = getScrollY() + getHeight() + YOffset;
		final int right = getScrollX() + getWidth();
		final float xo = getLeftOfLine();
		final boolean spl = SL.isEnabled();
		final float av = getWidth() - xo;

		int line = SL.findStartDrawLine(Math.max((int) (getScrollY() / LineHeight), 0));
		float y;
		float XStart, wtmp, x;
		int i, en;
		int tot;
		if (_ShowLineNumber) {
			ColorPaint.setColor(_Theme.getSplitLineColor());
			canvas.drawRect(LineNumberWidth, getScrollY(), LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH, getScrollY() + getHeight(), ColorPaint);
		}
		int parseTot = -1;
		int parseTarget = -1;
		if (_Lexer != null) {
			parseTot = _Lexer.findPart(L.E[line]);
			parseTarget = _Lexer.DS[parseTot];
		}
		float SBeginLineEnd = -1;
		if (showCursor && _HighlightLine) {
			ColorPaint.setColor(_Theme.getSelectionColor());
			canvas.drawRect(xo - _ContentLeftPadding, LineHeight * DDC.line, right, LineHeight * (DDC.line + 1), ColorPaint);
		}
		LineDraw:
		for (; line < L.size(); line++) {
			if ((y = LineHeight * SL.getLineDisplayStart(line) + YOffset + _LinePaddingTop) >= bottom)
				break;
			if (_ShowLineNumber)
				canvas.drawText(Integer.toString(line + 1), LineNumberWidth, y, LineNumberPaint);
			final int sp = L.E[line];
			i = 0;
			en = L.getTrimmed(line);
			final char[] cs = S.getRawChars()[line];
			XStart = xo;
			if (getScrollX() > XStart && i < en)
				while ((wtmp = XStart + getCharWidth(cs[i])) < getScrollX()) {
					if (++i >= en) continue LineDraw;
					XStart = wtmp;
				}
			if (_Lexer != null) {
				if (parseTot <= _Lexer.DS[0]) {
					while (i + sp >= parseTarget && parseTot <= _Lexer.DS[0])
						parseTarget = _Lexer.DS[++parseTot];
					if (parseTot == _Lexer.DS[0] + 1) parseTarget = Integer.MAX_VALUE;
					if (parseTot != 0)
						ContentPaint.setColor(_Theme.getTypeColor(_Lexer.D[parseTot - 1]));
				}
			}
			if (spl) {
				while (XStart >= av) {
					XStart -= av;
					y += LineHeight;
				}
				tot = 0;
				int curLine = SL.getLineDisplayStart(line);
				for (x = XStart; i < en; i++) {
					if (i + sp == parseTarget) {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						ContentPaint.setColor(_Theme.getTypeColor(_Lexer.D[parseTot]));
						++parseTot;
						if (parseTot <= _Lexer.DS[0]) parseTarget = _Lexer.DS[parseTot];
					}
					if ((TMP[tot] = cs[i]) == '\t') {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						XStart += _CharWidths[CHAR_TAB];
						x += _CharWidths[CHAR_TAB];
					} else
						x += getCharWidth(TMP[tot++]);
					if (x >= getWidth()) {
						--tot;
						--i;
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						x -= getCharWidth(TMP[tot + 1]);
						ColorPaint.setColor(_Theme.getLineNumberColor());
						canvas.drawRect(x, y - YOffset - _LinePaddingTop, getWidth(), y - YOffset - _LinePaddingTop + LineHeight, ColorPaint);
						tot = 0;
						if (showSelecting) {
							final float yb = y - YOffset - _LinePaddingTop;
							ColorPaint.setColor(_Theme.getSelectionColor());
							if (curLine == DDSBegin.line) {
								if (DDSBegin.line == DDSEnd.line)
									canvas.drawRect(xo + DDSBegin.column, yb, xo + DDSEnd.column, yb + LineHeight, ColorPaint);
								else
									canvas.drawRect(xo + DDSBegin.column, yb, x, yb + LineHeight, ColorPaint);
							} else if (curLine == DDSEnd.line)
								canvas.drawRect(xo, yb, xo + DDSEnd.column, yb + LineHeight, ColorPaint);
							else if (curLine > DDSBegin.line && curLine < DDSEnd.line)
								canvas.drawRect(xo, yb, x, yb + LineHeight, ColorPaint);
						}
						XStart = x = xo;
						++curLine;
						if ((y += LineHeight) >= bottom) break LineDraw;
					}
				}
				if (showSelecting) {
					final float yb = y - YOffset - _LinePaddingTop;
					ColorPaint.setColor(_Theme.getSelectionColor());
					if (curLine == DDSBegin.line) {
						if (DDSBegin.line == DDSEnd.line)
							canvas.drawRect(xo + DDSBegin.column, yb, xo + DDSEnd.column, yb + LineHeight, ColorPaint);
						else
							canvas.drawRect(xo + DDSBegin.column, yb, x, yb + LineHeight, ColorPaint);
					} else if (curLine == DDSEnd.line)
						canvas.drawRect(xo, yb, xo + DDSEnd.column, yb + LineHeight, ColorPaint);
					else if (curLine > DDSBegin.line && curLine < DDSEnd.line)
						canvas.drawRect(xo, yb, x, yb + LineHeight, ColorPaint);
				}
				canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
			} else {
				tot = 0;
				for (x = XStart; i < en && x <= right; i++) {
					if (i + sp == parseTarget) {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						ContentPaint.setColor(_Theme.getTypeColor(_Lexer.D[parseTot]));
						++parseTot;
						if (parseTot <= _Lexer.DS[0]) parseTarget = _Lexer.DS[parseTot];
					}
					if ((TMP[tot] = cs[i]) == '\t') {
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
					if (line == _S.begin.line) SBeginLineEnd = x;
					else if (line > _S.begin.line && line < _S.end.line) {
						ColorPaint.setColor(_Theme.getSelectionColor());
						canvas.drawRect(xo, y - YOffset - _LinePaddingTop, x, y - YOffset + TextHeight + _LinePaddingBottom, ColorPaint);
					}
				}
			}
		}
		if (showCursor) {
			ColorPaint.setColor(_Theme.getCursorLineColor());
			ColorPaint.setStrokeWidth(_CursorWidth);
			final float sty = LineHeight * (DDC.line + 1);
			if (_CurrentlyShowCursorLine && _ShowCursorLine)
				canvas.drawLine(xo + DDC.column, sty - LineHeight, xo + DDC.column, sty, ColorPaint);
			_Indicator.draw(canvas, xo + DDC.column, sty, Indicator.TYPE_NORMAL);
		} else if (showSelecting) {
			final float sty = LineHeight * (DDSBegin.line + 1);
			final float eny = LineHeight * (DDSEnd.line + 1);
			if (!spl) {
				if (_S.begin.line == _S.end.line) {
					ColorPaint.setColor(_Theme.getSelectionColor());
					canvas.drawRect(xo + DDSBegin.column, sty - LineHeight, xo + DDSEnd.column, sty, ColorPaint);
				} else {
					ColorPaint.setColor(_Theme.getSelectionColor());
					if (SBeginLineEnd != -1)
						canvas.drawRect(xo + DDSBegin.column, sty - LineHeight, SBeginLineEnd, sty, ColorPaint);
					canvas.drawRect(xo, eny - LineHeight, xo + DDSEnd.column, eny, ColorPaint);
				}
			}
			_Indicator.draw(canvas, xo + DDSBegin.column, sty, Indicator.TYPE_LEFT);
			_Indicator.draw(canvas, xo + DDSEnd.column, eny, Indicator.TYPE_RIGHT);
		}
		_SlideBar.draw(canvas);
	}

	public void finishScrolling() {
		Scroller.forceFinished(true);
	}

	public void deleteSurrounding(int beforeLength, int afterLength) {
		if (isRangeSelecting())
			deleteSelecting();
		else {
			RangeSelection<Cursor> sel = new RangeSelection<>(C);
			S.moveBack(sel.begin, beforeLength);
			S.moveForward(sel.end, afterLength);
			S.delete(sel);
		}
	}

	public void clearCharWidthCache() {
		Arrays.fill(_CharWidths, 0);
		_CharWidths[CHAR_TAB] = (_CharWidths[CHAR_SPACE] = ContentPaint.measureText(" ")) * TABSpaceCount;
	}

	// -------------------------
	// -----Private Methods-----
	// -------------------------

	public char getChar(int ind) {
		return S.charAt(ind);
	}

	private void applyTheme() {
		setBackgroundColor(_Theme.getBackgroundColor());
		if (_Lexer == null) ContentPaint.setColor(_Theme.getTypeColor(MLexer.TYPE_PURE));
		_Indicator.setHeight(TextHeight);
		LineNumberPaint.setColor(_Theme.getLineNumberColor());
		_SlideBar.onSchemeChange();
	}

	private byte getDraggingCursor(float x, float y) {
		final float ori = x;
		x -= _ContentLeftPadding;
		if (_ShowLineNumber) x -= (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH);
		if (isRangeSelecting()) {
			final float sty = LineHeight * (DDSBegin.line + 1);
			final float eny = LineHeight * (DDSEnd.line + 1);
			if (_Indicator.isTouched(x - DDSBegin.column, y - sty, Indicator.TYPE_LEFT)) {
				_lastX = ori;
				_lastY = sty - LineHeight * 0.5f;
				return Indicator.TYPE_LEFT;
			}
			if (_Indicator.isTouched(x - DDSEnd.column, y - eny, Indicator.TYPE_RIGHT)) {
				_lastX = ori;
				_lastY = eny - LineHeight * 0.5f;
				return Indicator.TYPE_RIGHT;
			}
		} else {
			final float sty = LineHeight * (DDC.line + 1);
			if (_Indicator.isTouched(x - DDC.column, y - sty, Indicator.TYPE_NORMAL)) {
				_lastX = ori;
				_lastY = sty - LineHeight * 0.5f;
				return Indicator.TYPE_NORMAL;
			}
		}
		return Indicator.TYPE_NONE;
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
			if (_Editable) commitChar((char) event.getUnicodeChar(event.getMetaState()));
			return true;
		}
		if (_Editable)
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_SPACE:
					commitChar(' ');
					break;
				case KeyEvent.KEYCODE_DEL:
					if (isRangeSelecting())
						deleteSelecting();
					else
						S.delete(C);
					break;
				case KeyEvent.KEYCODE_ENTER:
					commitChar('\n');
					break;
				default:
					return false;
			}
		return true;
	}

	protected void onClick(float x, float y) {
		if (!isFocused()) requestFocusFromTouch();
		long time = System.currentTimeMillis();
		boolean dc = (time - LastClickTime <= DOUBLE_CLICK_INTERVAL) && (Math.abs(x - _lastClickX) <= DOUBLE_CLICK_RANGE) && (Math.abs(y - _lastClickY) <= DOUBLE_CLICK_RANGE);
		_lastClickX = x;
		_lastClickY = y;
		LastClickTime = time;
		Cursor nc = getCursorByPosition(x, y);
		moveCursor(nc);
		_ComposingStart = null;
		if (dc) expandSelectionFrom(nc);
		else finishSelecting();
		if (_Editable && _IMM != null) {
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
		_Indicator.setHeight(TextHeight);
		clearCharWidthCache();
		SL.onUpdate();
		onSelectionUpdate();
		onLineChange();
		requestLayout();
		postInvalidate();
	}

	protected void onLineChange() {
		ContentHeight = (int) (LineHeight * SL.getTotalCount());
		_YScrollRange = Math.max(ContentHeight - getHeight(), 0);
		if (LineNumberPaint != null)
			LineNumberWidth = LineNumberPaint.measureText("9") * ((int) Math.log10(L.size()) + 1);
	}

	private void springBack() {
		Scroller.springBack(getScrollX(), getScrollY(), 0, Integer.MAX_VALUE, 0, _YScrollRange);
	}

	protected ClipboardManager getClipboardManager() {
		return (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
	}

	protected void onSelectionUpdate() {
		if (_CBEnabled) {
			if (RS && _ShowingActionMode == null) _CBHelper.show();
			else _CBHelper.hide();
		}
		if (_SelectListener != null) {
			if (RS) _SelectListener.onSelect(_S.clone());
			else _SelectListener.onSelect(new RangeSelection<>(C));
		}
		if (!RS) {
			DDC = SL.getDisplayCursor(C);
			makeCursorVisible(C);
		} else {
			DDSBegin = SL.getDisplayCursor(_S.begin);
			DDSEnd = SL.getDisplayCursor(_S.end);
			makeCursorVisible(_S.end);
		}
		if (_Editable && _IMM != null) {
			int sst, sen;
//			CursorAnchorInfo.Builder builder = new CursorAnchorInfo.Builder().setMatrix(null);
			if (RS) {
				sst = S.Cursor2Index(_S.begin);
				sen = S.Cursor2Index(_S.end);
			} else {
				sst = sen = getCursorPosition();
				float top = LineHeight * SL.getLineDisplayStart(C.line);
				float xo = (_ShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + _ContentLeftPadding;
//				builder.setInsertionMarkerLocation(xo + _CursorHorizonOffset, top, top + YOffset, top + TextHeight, CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION);
			}
//			builder.setSelectionRange(sst, sen);
//			_IMM.updateCursorAnchorInfo(this, builder.build());
			_IMM.updateSelection(this, sst, sen, -1, -1);
//			_IMM.restartInput(this);
		}
	}

	private void setComposingText(char[] cs) {
		if (_ComposingStart == null) {
			_ComposingStart = C.clone();
			S.insert(getCursor(), cs);
		} else
			S.replace(new RangeSelection<>(_ComposingStart, C), cs);
		if (cs.length == 0)
			_ComposingStart = null;
	}

	// --------------------------
	// -----Temporary Fields-----
	// --------------------------

	// TODO 还有1024个字符都塞不满屏幕的情况！
	private char[] TMP = new char[1024];
	private char[] TMP2 = new char[1];

// -----------------------
// -----Inner Classes-----
// -----------------------

	public interface EventHandler {
		boolean handleEvent(MEdit edit, MotionEvent event);
	}

	private static class ClipboardActionModeHelper {
		private MEdit Content;
		private Context cx;
		private ActionMode _ActionMode;

		public ClipboardActionModeHelper(MEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (Content._ShowingActionMode != null) return;
			if (_ActionMode == null)
				((AppCompatActivity) cx).startSupportActionMode(new ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						_ActionMode = mode;
						mode.setTitle(android.R.string.selectTextMode);
						TypedArray array = cx.getTheme().obtainStyledAttributes(new int[]{
								android.R.attr.actionModeSelectAllDrawable,
								android.R.attr.actionModeCutDrawable,
								android.R.attr.actionModeCopyDrawable,
								android.R.attr.actionModePasteDrawable,
						});
						menu.add(0, 0, 0, cx.getString(android.R.string.selectAll))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('a')
								.setIcon(array.getDrawable(0));
						if (Content.isEditable())
							menu.add(0, 1, 0, cx.getString(android.R.string.cut))
									.setShowAsActionFlags(2)
									.setAlphabeticShortcut('x')
									.setIcon(array.getDrawable(1));
						menu.add(0, 2, 0, cx.getString(android.R.string.copy))
								.setShowAsActionFlags(2)
								.setAlphabeticShortcut('c')
								.setIcon(array.getDrawable(2));
						if (Content.isEditable())
							menu.add(0, 3, 0, cx.getString(android.R.string.paste))
									.setShowAsActionFlags(2)
									.setAlphabeticShortcut('v')
									.setIcon(array.getDrawable(3));
						array.recycle();
						return true;
					}

					@Override
					public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
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
					public void onDestroyActionMode(ActionMode mode) {
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
		void onSelect(RangeSelection<Cursor> sel);
	}

	private static class MInputConnection implements InputConnection {
		private final MEdit Q;
		private Malax S;

		public MInputConnection(MEdit parent) {
			this.Q = parent;
			this.S = parent.S;
		}

		public void onUpdate() {
			this.S = Q.S;
		}

		@Override
		public CharSequence getTextBeforeCursor(int n, int flags) {
			return S.substring(S.fromEnd(Q.C, n));
		}

		@Override
		public CharSequence getTextAfterCursor(int n, int flags) {
			return S.substring(S.fromBegin(Q.C, n));
		}

		@Override
		public CharSequence getSelectedText(int flags) {
			return Q.getSelectedText();
		}

		@Override
		public int getCursorCapsMode(int reqModes) {
			// TODO Fix Me Maybe
			return InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
		}

		@Override
		public boolean setComposingRegion(int start, int end) {
			if (start == end)
				Q._ComposingStart = null;
			else
				Q._ComposingStart = S.Index2Cursor(start);
			return true;
		}

		@Override
		public boolean setComposingText(CharSequence text, int newCursorPosition) {
			Q.setComposingText(com.mivik.malax.Editable.CharSequence2CharArray(text, 0, text.length()));
			return true;
		}

		@Override
		public boolean finishComposingText() {
			Q._ComposingStart = null;
			return true;
		}

		@Override
		public boolean commitText(CharSequence text, int newCursorPosition) {
			Q.commitChars(com.mivik.malax.Editable.CharSequence2CharArray(text, 0, text.length()));
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
				Q.moveCursor(S.Index2Cursor(start));
				return true;
			}
			Q.setSelectionRange(new RangeSelection<>(S, start, end));
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
		public boolean commitContent(@NonNull InputContentInfo inputContentInfo, int flags, Bundle opts) {
			return false;
		}
	}

	public static abstract class SlideBar {

		protected MEdit parent;

		public SlideBar(MEdit parent) {
			this.parent = parent;
		}

		public abstract void onSchemeChange();

		public abstract void draw(Canvas canvas);

		public abstract boolean handleEvent(MotionEvent event);
	}

	public static class MaterialSlideBar extends SlideBar {
		public static final int EXPAND_WIDTH = 25, COLLAPSE_WIDTH = 20;
		public static final int HEIGHT = 100;

		private boolean expand;
		private Paint mp;
		private float startY;

		public MaterialSlideBar(MEdit parent) {
			super(parent);
			expand = false;
			mp = new Paint();
			mp.setStyle(Paint.Style.FILL);
			mp.setAntiAlias(false);
		}

		@Override
		public void onSchemeChange() {
			mp.setColor(0xC4FFFFFF & parent._Theme.getSlideBarColor());
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

	public static abstract class Indicator {
		public static final byte TYPE_NONE = -1, TYPE_NORMAL = 0, TYPE_LEFT = 1, TYPE_RIGHT = 2;

		protected MEdit P;

		public Indicator(MEdit parent) {
			this.P = parent;
		}

		public abstract void setHeight(float height);

		public abstract void draw(Canvas canvas, float x, float y, byte type);

		public abstract boolean isTouched(float x, float y, byte type);

		public abstract void recycle();
	}

	public static class GlassIndicator extends Indicator {
		private float h, radius;
		private Paint mp;
		private Bitmap c0, c1, c2;

		public GlassIndicator(MEdit parent) {
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
			mp.setColor(P._Theme.getIndicatorColor());
			canvas.drawPath(path, mp);
			canvas.drawCircle(0, h - radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(0, h - radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getIndicatorGlassColor());
			canvas.drawCircle(0, h - radius, gradius, mp);

			canvas = new Canvas(c1);
			mp.setColor(P._Theme.getIndicatorColor());
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(radius, 0, radius * 2, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getIndicatorGlassColor());
			canvas.drawCircle(radius, radius, gradius, mp);

			canvas = new Canvas(c2);
			mp.setColor(P._Theme.getIndicatorColor());
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(0, 0, radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gradius, mp);
			mp.setXfermode(null);
			mp.setColor(P._Theme.getIndicatorGlassColor());
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
}