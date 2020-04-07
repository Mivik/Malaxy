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
import java.util.List;

import static com.mivik.malax.BaseMalax.Cursor;

public class MEdit extends View implements
		WordWrappingManager.UpdateListener,
		ViewTreeObserver.OnGlobalLayoutListener,
		Runnable,
		WrappedEditable.CursorListener<Cursor> {

	public static final boolean USE_COMPAT;

	static {
		boolean compat = false;
		try {
			Class.forName("androidx.appcompat.app.AppCompatActivity");
			compat = true;
		} catch (ClassNotFoundException ignored) {
		}
		USE_COMPAT = compat;
	}
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

	protected final Paint ContentPaint, LineNumberPaint, ColorPaint;
	private final ClipboardActionModeHelper mClipboardHelper;
	private final List<WrappedEditable.EditActionListener> mEditActionListeners = new ArrayList<>();
	private final WordWrappingManager mWordWrappingManager = new WordWrappingManager(this);
	private final byte[] mBlinkLock = new byte[0];

	protected float YOffset;
	protected float TextHeight;
	protected int ContentHeight;
	protected Cursor C = new Cursor(0, 0);
	private Cursor mDisplayCursor;
	private Cursor mDisplayBeginCursor, mDisplayEndCursor;
	private boolean mRangeSelecting = false;
	public Malax S;
	public LineManager L;
	private int mMinFling, mTouchSlop;
	private float mLastX, mLastY, mStartX, mStartY;
	private OverScroller Scroller;
	private VelocityTracker SpeedCalc;
	private boolean isDragging = false;
	protected int TABSpaceCount = 4;
	private int mYScrollRange;
	protected float LineNumberWidth;
	private int mMaxOverScrollX = 20, mMaxOverScrollY = 20;
	protected RangeSelection<Cursor> mSelection = new RangeSelection<>(new Cursor(0, 0), new Cursor(0, 0));
	private float mCursorWidth = 2;
	private float mLineTopPadding = 5, mLineBottomPadding = 5;
	private float mContentLeftPadding = 7;
	protected boolean mEditable = true;
	protected MInputConnection mInputConnection;
	protected boolean mShowLineNumber = true;
	private float[] mCharWidths = new float[65536];
	private InputMethodManager mInputMethodManager;
	private long LastClickTime = 0;
	private float mLastClickX, mLastClickY;
	private Cursor mComposingStart = null;
	protected MEditTheme mTheme = MEditThemeDark.getInstance();
	protected MLexer mLexer;
	protected Indicator mIndicator = new GlassIndicator(this);
	protected float LineHeight;
	protected byte mDraggingIndicator = Indicator.TYPE_NONE;
	protected SlideBar mSlideBar = new MaterialSlideBar(this);
	private boolean mBlinkCursor;
	private boolean mCurrentlyShowCursorLine;
	private boolean mShowCursorLine = true;
	private Handler mHandler = new Handler();
	private SelectListener mSelectListener;
	private boolean mClipboardHelperEnabled = true;
	private ActionMode mShowingActionMode;
	private EventHandler H;
	private boolean mHighlightLine = true;

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
		mWordWrappingManager.setUpdateListener(this);
		setWordWrappingEnabled(false);
		mLastClickX = mLastClickY = 0;
		setLayerType(View.LAYER_TYPE_HARDWARE, null);
		Scroller = new OverScroller(getContext());
		SpeedCalc = VelocityTracker.obtain();
		ViewConfiguration config = ViewConfiguration.get(cx);
		mMinFling = config.getScaledMinimumFlingVelocity();
		mTouchSlop = config.getScaledTouchSlop();
		if (USE_COMPAT) mClipboardHelper = new ClipboardActionModeHelperCompat(this);
		else mClipboardHelper = new ClipboardActionModeHelperNative(this);
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
		mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		applyTheme();
		setBlinkCursor(true);
	}

	// ------------------
	// -----Methods------
	// ------------------

	public float getLeftOfLine() {
		return (mShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + mContentLeftPadding;
	}

	public float getLineWidth() {
		return getMeasuredWidth() - getLeftOfLine();
	}

	public void setWordWrappingEnabled(boolean flag) {
		mWordWrappingManager.setEnabled(flag);
		setScrollX(0);
		postInvalidate();
	}

	public boolean isWordWrappingEnabled() {
		return mWordWrappingManager.isEnabled();
	}

	public void setHighlightLine(boolean flag) {
		this.mHighlightLine = flag;
	}

	public boolean isHighlightLine() {
		return mHighlightLine;
	}

	public void setEventHandler(EventHandler handler) {
		this.H = handler;
	}

	public EventHandler getEventHandler() {
		return this.H;
	}

	public boolean addEditActionListener(WrappedEditable.EditActionListener listener) {
		boolean ret;
		synchronized (mEditActionListeners) {
			ret = mEditActionListeners.add(listener);
		}
		S.addEditActionListener(listener);
		return ret;
	}

	public boolean removeEditActionListener(WrappedEditable.EditActionListener listener) {
		boolean ret;
		synchronized (mEditActionListeners) {
			ret = mEditActionListeners.remove(listener);
		}
		S.removeEditActionListener(listener);
		return ret;
	}

	public void clearEditActionListeners() {
		mEditActionListeners.clear();
	}

	public void setMalax(Malax malax) {
		S = malax;
		S.setCursorListener(this);
		synchronized (mEditActionListeners) {
			for (WrappedEditable.EditActionListener one : mEditActionListeners)
				S.addEditActionListener(one);
		}
		L = S.getLineManager();
		mSelection = new RangeSelection<>(S, 0, 0);
		mLexer = S.getLexer();
		if (mLexer == null) ContentPaint.setColor(mTheme.getTypeColor(MLexer.TYPE_PURE));
		moveCursor(S.getBeginCursor());
		if (mInputConnection != null) mInputConnection.onUpdate();
		S.setContentChangeListener(mWordWrappingManager);
		mWordWrappingManager.onUpdate();
		onLineChange();
		postInvalidate();
	}

	public void onStartActionMode(ActionMode mode) {
		mShowingActionMode = mode;
	}

	public void onHideActionMode() {
		mShowingActionMode = null;
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
		for (char value : cmp) {
			char c = S.charAt(x);
			if (value != c) return false;
			S.moveForward(x);
		}
		return true;
	}

	public void setClipboardHelperEnabled(boolean flag) {
		if (!(mClipboardHelperEnabled = flag)) mClipboardHelper.hide();
	}

	public boolean isClipboardHelperEnabled() {
		return mClipboardHelperEnabled;
	}

	public boolean isEditable() {
		return mEditable;
	}

	public void setSelectListener(SelectListener listener) {
		mSelectListener = listener;
	}

	public SelectListener getSelectListener() {
		return mSelectListener;
	}

	public void setSlideBar(SlideBar bar) {
		mSlideBar = bar;
		postInvalidate();
	}

	public SlideBar getSlideBar() {
		return mSlideBar;
	}

	public void setIndicator(Indicator indicator) {
		mIndicator = indicator;
		mIndicator.setHeight(TextHeight);
		postInvalidate();
	}

	public Indicator getIndicator() {
		return mIndicator;
	}

	public float getLineHeight() {
		return LineHeight;
	}

	public int getContentHeight() {
		return ContentHeight;
	}

	public boolean isParsed() {
		if (mLexer == null) return true;
		return mLexer.isParsed();
	}

	public void parseAll() {
		if (mLexer == null) return;
		mLexer.parseAll();
	}

	public void setShowCursorLine(boolean flag) {
		this.mShowCursorLine = false;
	}

	public boolean isShowCursorLine() {
		return mShowCursorLine;
	}

	public void setBlinkCursor(boolean flag) {
		synchronized (mBlinkLock) {
			if (mBlinkCursor == flag) return;
			if (mBlinkCursor = flag)
				mHandler.post(this);
			else {
				mHandler.removeCallbacks(this);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mCurrentlyShowCursorLine = true;
					}
				});
			}
		}
	}

	public boolean isBlinkCursor() {
		return mBlinkCursor;
	}

	public float getMaxScrollY() {
		return ContentHeight - getHeight();
	}

	public void selectAll() {
		setSelectionRange(S.getFullRangeSelection());
	}

	public boolean paste() {
		if (!mEditable) return false;
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
		if (!mEditable) return false;
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
		if (!mEditable) return;
		S.delete(getSelectionRange());
		finishSelecting();
	}

	public void setLexer(MLexer lexer) {
		S.setLexer(lexer);
		mLexer = S.getLexer();
		if (mLexer == null) ContentPaint.setColor(mTheme.getTypeColor(MLexer.TYPE_PURE));
		postInvalidate();
	}

	public MLexer getLexer() {
		return mLexer;
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
		mShowLineNumber = flag;
		postInvalidate();
	}

	public boolean isShowLineNumber() {
		return mShowLineNumber;
	}

	public void setEditable(boolean editable) {
		mEditable = editable;
		if (mEditable && mInputMethodManager != null)
			mInputMethodManager.restartInput(this);
		else hideIME();
		postInvalidate();
	}

	public void setContentLeftPadding(float padding) {
		mContentLeftPadding = padding;
		postInvalidate();
	}

	public float getContentLeftPadding() {
		return mContentLeftPadding;
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
		this.mTheme = scheme;
		applyTheme();
		postInvalidate();
	}

	public MEditTheme getTheme() {
		return mTheme;
	}

	public void setTABSpaceCount(int count) {
		TABSpaceCount = count;
		mCharWidths[CHAR_TAB] = TABSpaceCount * mCharWidths[CHAR_SPACE];
		postInvalidate();
	}

	public int getTABSpaceCount() {
		return TABSpaceCount;
	}

	public void setLinePadding(float top, float bottom) {
		mLineTopPadding = top;
		mLineBottomPadding = bottom;
		onFontChange();
		postInvalidate();
	}

	public float getLinePaddingTop() {
		return mLineTopPadding;
	}

	public float getLinePaddingBottom() {
		return mLineBottomPadding;
	}

	public void setText(char[] s, int off, int length) {
		S.setText(s, off, length);
		if (mEditable && mInputMethodManager != null)
			mInputMethodManager.restartInput(this);
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
		return mSelection.clone();
	}

	// return true if the start and the end of the selection has reserved
	public boolean setSelectionStart(Cursor st) {
		boolean ret = false;
		if (st.compareTo(mSelection.end) > 0) {
			mSelection.begin = mSelection.end;
			mSelection.end = st.clone();
			ret = true;
		} else mSelection.begin = st.clone();
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
		if (en.compareTo(mSelection.begin) < 0) {
			mSelection.end = mSelection.begin;
			mSelection.begin = en.clone();
			ret = true;
		} else mSelection.end = en.clone();
		onSelectionUpdate();
		postInvalidate();
		return ret;
	}

	public void setSelectionRange(RangeSelection<Cursor> sel) {
		mRangeSelecting = true;
		mSelection.set(sel);
		onSelectionUpdate();
		postInvalidate();
	}

	public void moveCursor(Cursor x) {
		C.set(x);
		mDisplayCursor = mWordWrappingManager.getDisplayCursor(C);
		onSelectionUpdate();
		postInvalidate();
	}

	public void setMaxOverScroll(int x, int y) {
		mMaxOverScrollX = x;
		mMaxOverScrollY = y;
	}

	public int getMaxOverScrollX() {
		return mMaxOverScrollX;
	}

	public int getMaxOverScrollY() {
		return mMaxOverScrollY;
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
		mCursorWidth = width;
		postInvalidate();
	}

	public float getCursorWidth() {
		return mCursorWidth;
	}

	public int getLineLength(int line) {
		return L.E[line + 1] - L.E[line] - 1;
	}

	public int getTextLength() {
		return S.length();
	}

	public void showIME() {
		if (mInputMethodManager != null)
			mInputMethodManager.showSoftInput(this, 0);
	}

	public void hideIME() {
		if (mInputMethodManager != null && mInputMethodManager.isActive(this))
			mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	public boolean isRangeSelecting() {
		return mRangeSelecting;
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
		float y = LineHeight * mWordWrappingManager.getLineDisplayStart(line);
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
		float sum = (mShowLineNumber ? (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH) : 0) + mContentLeftPadding + x.column;
		if (sum - mCursorWidth / 2 < getScrollX()) {
			finishScrolling();
			scrollTo((int) (sum - mCursorWidth / 2) - SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		} else if (sum + mCursorWidth / 2 > getScrollX() + getWidth()) {
			finishScrolling();
			scrollTo((int) Math.ceil(sum + mCursorWidth / 2 - getWidth()) + SCROLL_TO_CURSOR_EXTRA, getScrollY());
			postInvalidate();
		}
	}

	public void finishSelecting() {
		mRangeSelecting = false;
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
		float ret = mCharWidths[c];
		if (ret == 0) {
			TMP2[0] = c;
			ret = ContentPaint.measureText(TMP2, 0, 1);
			if (ret < EMPTY_CHAR_WIDTH) ret = EMPTY_CHAR_WIDTH;
			mCharWidths[c] = ret;
		}
		return ret;
	}

	public void commitChar(char ch) {
		mComposingStart = null;
		if (isRangeSelecting()) {
			S.replace(getSelectionRange(), new char[]{ch});
			finishSelecting();
		} else S.insert(C, ch);
	}

	public void commitChars(char[] cs) {
		mComposingStart = null;
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
		return mWordWrappingManager.getOriginalCursor(new Cursor(Math.max((int) (y / LineHeight), 0), (int) (x - getLeftOfLine())));
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
		mDisplayCursor = mWordWrappingManager.getDisplayCursor(C);
		mDisplayBeginCursor = mWordWrappingManager.getDisplayCursor(mSelection.begin);
		mDisplayEndCursor = mWordWrappingManager.getDisplayCursor(mSelection.end);
		onLineChange();
		post(new Runnable() {
			@Override
			public void run() {
				mWordWrappingManager.onUpdate();
			}
		});
	}

	@Override
	public void scrollTo(int x, int y) {
		if (mWordWrappingManager.isEnabled()) x = 0;
		super.scrollTo(x, y);
	}

	@Override
	public void onGlobalLayout() {
		mWordWrappingManager.onUpdate();
	}

	@Override
	public void onCursorMoved(Cursor cursor) {
		moveCursor(cursor);
		postInvalidate();
	}

	@Override
	public void run() {
		if (!mShowCursorLine) return;
		synchronized (mBlinkLock) {
			if (!mBlinkCursor) return;
			mCurrentlyShowCursorLine = !mCurrentlyShowCursorLine;
			postInvalidate();
			mHandler.postDelayed(this, BLINK_INTERVAL);
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
		mDraggingIndicator = Indicator.TYPE_NONE;
		mIndicator.recycle();
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
		if (mSlideBar.handleEvent(event)) {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) finishScrolling();
			return true;
		}
		if (H != null && H.handleEvent(this, event)) return true;
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mStartX = mLastX = event.getX();
				mStartY = mLastY = event.getY();
				mDraggingIndicator = getDraggingCursor(mStartX + getScrollX(), mStartY + getScrollY());
				if (mDraggingIndicator != Indicator.TYPE_NONE) {
					mStartX += getScrollX();
					mStartY += getScrollY();
				}
				if (!Scroller.isFinished())
					Scroller.abortAnimation();
				if (!isFocused())
					requestFocus();
				return true;
			case MotionEvent.ACTION_MOVE:
				float x = event.getX(), y = event.getY();
				if (mDraggingIndicator != Indicator.TYPE_NONE) {
					Cursor nc = getCursorByPosition(x + getScrollX() - mStartX + mLastX, y + getScrollY() - mStartY + mLastY);
					switch (mDraggingIndicator) {
						case Indicator.TYPE_NORMAL: {
							moveCursor(nc);
							return true;
						}
						case Indicator.TYPE_LEFT: {
							if (setSelectionStart(nc)) mDraggingIndicator = Indicator.TYPE_RIGHT;
							makeCursorVisible(nc);
							return true;
						}
						case Indicator.TYPE_RIGHT: {
							if (setSelectionEnd(nc)) mDraggingIndicator = Indicator.TYPE_LEFT;
							makeCursorVisible(nc);
							return true;
						}
					}
				}
				if ((!isDragging) && (Math.abs(x - mStartX) > mTouchSlop || Math.abs(y - mStartY) > mTouchSlop)) {
					isDragging = true;
					if (_fixScroll)
						_dragDirection = Math.abs(x - mLastX) > Math.abs(y - mLastY);
				}
				if (isDragging) {
					int finalX = getScrollX(), finalY = getScrollY();
					if (_fixScroll) {
						if (_dragDirection) {
							finalX += (mLastX - x);
							// TODO 如果要改X边界记得这儿加上
							if (finalX < -mMaxOverScrollX) finalX = -mMaxOverScrollX;
						} else {
							finalY += (mLastY - y);
							if (finalY < -mMaxOverScrollY) finalY = -mMaxOverScrollY;
							else if (finalY > mYScrollRange + mMaxOverScrollY)
								finalY = mYScrollRange + mMaxOverScrollY;
						}
					} else {
						finalX += (mLastX - x);
						// TODO 如果要改X边界记得这儿加上
						if (finalX < -mMaxOverScrollX) finalX = -mMaxOverScrollX;
						finalY += (mLastY - y);
						if (finalY < -mMaxOverScrollY) finalY = -mMaxOverScrollY;
						else if (finalY > mYScrollRange + mMaxOverScrollY)
							finalY = mYScrollRange + mMaxOverScrollY;
					}
					scrollTo(finalX, finalY);
					postInvalidate();
				}
				mLastX = x;
				mLastY = y;
				return true;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (mDraggingIndicator != Indicator.TYPE_NONE) {
					mDraggingIndicator = Indicator.TYPE_NONE;
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
					if (Math.abs(speedX) <= mMinFling) speedX = 0;
					if (Math.abs(speedY) <= mMinFling) speedY = 0;
					if (_fixScroll) {
						if (_dragDirection) speedY = 0;
						else speedX = 0;
					}
					if (speedX != 0 || speedY != 0)
						Scroller.fling(getScrollX(), getScrollY(), -speedX, -speedY, -mMaxOverScrollX, Integer.MAX_VALUE, -mMaxOverScrollY, mYScrollRange + mMaxOverScrollY);
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
		mYScrollRange = Math.max(ContentHeight - (bottom - top), 0);
	}

	@Override
	public void computeScroll() {
		if (Scroller.computeScrollOffset()) {
			int x = Scroller.getCurrX();
			int y = Scroller.getCurrY();
			scrollTo(x, y);
			postInvalidate();
		} else if (!isDragging && (getScrollX() < 0 || getScrollY() < 0 || getScrollY() > mYScrollRange)) { // TODO X边界还要改我
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
		if (mRangeSelecting) {
			outAttrs.initialSelStart = S.Cursor2Index(mSelection.begin);
			outAttrs.initialSelEnd = S.Cursor2Index(mSelection.end);
		} else
			outAttrs.initialSelStart = outAttrs.initialSelEnd = S.Cursor2Index(C);
		if (mInputConnection == null)
			mInputConnection = new MInputConnection(this);
		return mInputConnection;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) hideIME();
		super.setEnabled(enabled);
		if (enabled && mEditable && mInputMethodManager != null) mInputMethodManager.restartInput(this);
	}

	// 绘制函数
	@Override
	protected void onDraw(Canvas canvas) {
		final boolean showSelecting = isRangeSelecting();
		final boolean showCursor = (!showSelecting);
		final float bottom = getScrollY() + getHeight() + YOffset;
		final int right = getScrollX() + getWidth();
		final float xo = getLeftOfLine();
		final boolean spl = mWordWrappingManager.isEnabled();
		final float av = getWidth() - xo;

		int line = mWordWrappingManager.findStartDrawLine(Math.max((int) (getScrollY() / LineHeight), 0));
		float y;
		float XStart, wtmp, x;
		int i, en;
		int tot;
		if (mShowLineNumber) {
			ColorPaint.setColor(mTheme.getSplitLineColor());
			canvas.drawRect(LineNumberWidth, getScrollY(), LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH, getScrollY() + getHeight(), ColorPaint);
		}
		int parseTot = -1;
		int parseTarget = -1;
		if (mLexer != null) {
			parseTot = mLexer.findPart(L.E[line]);
			parseTarget = mLexer.DS[parseTot];
		}
		float SBeginLineEnd = -1;
		if (showCursor && mHighlightLine) {
			ColorPaint.setColor(mTheme.getSelectionColor());
			canvas.drawRect(xo - mContentLeftPadding, LineHeight * mDisplayCursor.line, right, LineHeight * (mDisplayCursor.line + 1), ColorPaint);
		}
		LineDraw:
		for (; line < L.size(); line++) {
			if ((y = LineHeight * mWordWrappingManager.getLineDisplayStart(line) + YOffset + mLineTopPadding) >= bottom)
				break;
			if (mShowLineNumber)
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
			if (mLexer != null) {
				if (parseTot <= mLexer.DS[0]) {
					while (i + sp >= parseTarget && parseTot <= mLexer.DS[0])
						parseTarget = mLexer.DS[++parseTot];
					if (parseTot == mLexer.DS[0] + 1) parseTarget = Integer.MAX_VALUE;
					if (parseTot != 0)
						ContentPaint.setColor(mTheme.getTypeColor(mLexer.D[parseTot - 1]));
				}
			}
			if (spl) {
				while (XStart >= av) {
					XStart -= av;
					y += LineHeight;
				}
				tot = 0;
				int curLine = mWordWrappingManager.getLineDisplayStart(line);
				for (x = XStart; i < en; i++) {
					if (i + sp == parseTarget) {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						ContentPaint.setColor(mTheme.getTypeColor(mLexer.D[parseTot]));
						++parseTot;
						if (parseTot <= mLexer.DS[0]) parseTarget = mLexer.DS[parseTot];
					}
					if ((TMP[tot] = cs[i]) == '\t') {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						XStart += mCharWidths[CHAR_TAB];
						x += mCharWidths[CHAR_TAB];
					} else
						x += getCharWidth(TMP[tot++]);
					if (x >= getWidth()) {
						--tot;
						--i;
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						x -= getCharWidth(TMP[tot + 1]);
						ColorPaint.setColor(mTheme.getLineNumberColor());
						canvas.drawRect(x, y - YOffset - mLineTopPadding, getWidth(), y - YOffset - mLineTopPadding + LineHeight, ColorPaint);
						tot = 0;
						if (showSelecting) {
							final float yb = y - YOffset - mLineTopPadding;
							ColorPaint.setColor(mTheme.getSelectionColor());
							if (curLine == mDisplayBeginCursor.line) {
								if (mDisplayBeginCursor.line == mDisplayEndCursor.line)
									canvas.drawRect(xo + mDisplayBeginCursor.column, yb, xo + mDisplayEndCursor.column, yb + LineHeight, ColorPaint);
								else
									canvas.drawRect(xo + mDisplayBeginCursor.column, yb, x, yb + LineHeight, ColorPaint);
							} else if (curLine == mDisplayEndCursor.line)
								canvas.drawRect(xo, yb, xo + mDisplayEndCursor.column, yb + LineHeight, ColorPaint);
							else if (curLine > mDisplayBeginCursor.line && curLine < mDisplayEndCursor.line)
								canvas.drawRect(xo, yb, x, yb + LineHeight, ColorPaint);
						}
						XStart = x = xo;
						++curLine;
						if ((y += LineHeight) >= bottom) break LineDraw;
					}
				}
				if (showSelecting) {
					final float yb = y - YOffset - mLineTopPadding;
					ColorPaint.setColor(mTheme.getSelectionColor());
					if (curLine == mDisplayBeginCursor.line) {
						if (mDisplayBeginCursor.line == mDisplayEndCursor.line)
							canvas.drawRect(xo + mDisplayBeginCursor.column, yb, xo + mDisplayEndCursor.column, yb + LineHeight, ColorPaint);
						else
							canvas.drawRect(xo + mDisplayBeginCursor.column, yb, x, yb + LineHeight, ColorPaint);
					} else if (curLine == mDisplayEndCursor.line)
						canvas.drawRect(xo, yb, xo + mDisplayEndCursor.column, yb + LineHeight, ColorPaint);
					else if (curLine > mDisplayBeginCursor.line && curLine < mDisplayEndCursor.line)
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
						ContentPaint.setColor(mTheme.getTypeColor(mLexer.D[parseTot]));
						++parseTot;
						if (parseTot <= mLexer.DS[0]) parseTarget = mLexer.DS[parseTot];
					}
					if ((TMP[tot] = cs[i]) == '\t') {
						canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
						XStart = x;
						tot = 0;
						XStart += mCharWidths[CHAR_TAB];
						x += mCharWidths[CHAR_TAB];
					} else
						x += getCharWidth(TMP[tot++]);
				}
				canvas.drawText(TMP, 0, tot, XStart, y, ContentPaint);
				if (showSelecting) {
					if (line == mSelection.begin.line) SBeginLineEnd = x;
					else if (line > mSelection.begin.line && line < mSelection.end.line) {
						ColorPaint.setColor(mTheme.getSelectionColor());
						canvas.drawRect(xo, y - YOffset - mLineTopPadding, x, y - YOffset + TextHeight + mLineBottomPadding, ColorPaint);
					}
				}
			}
		}
		if (showCursor) {
			ColorPaint.setColor(mTheme.getCursorLineColor());
			ColorPaint.setStrokeWidth(mCursorWidth);
			final float sty = LineHeight * (mDisplayCursor.line + 1);
			if (mCurrentlyShowCursorLine && mShowCursorLine)
				canvas.drawLine(xo + mDisplayCursor.column, sty - LineHeight, xo + mDisplayCursor.column, sty, ColorPaint);
			mIndicator.draw(canvas, xo + mDisplayCursor.column, sty, Indicator.TYPE_NORMAL);
		} else if (showSelecting) {
			final float sty = LineHeight * (mDisplayBeginCursor.line + 1);
			final float eny = LineHeight * (mDisplayEndCursor.line + 1);
			if (!spl) {
				if (mSelection.begin.line == mSelection.end.line) {
					ColorPaint.setColor(mTheme.getSelectionColor());
					canvas.drawRect(xo + mDisplayBeginCursor.column, sty - LineHeight, xo + mDisplayEndCursor.column, sty, ColorPaint);
				} else {
					ColorPaint.setColor(mTheme.getSelectionColor());
					if (SBeginLineEnd != -1)
						canvas.drawRect(xo + mDisplayBeginCursor.column, sty - LineHeight, SBeginLineEnd, sty, ColorPaint);
					canvas.drawRect(xo, eny - LineHeight, xo + mDisplayEndCursor.column, eny, ColorPaint);
				}
			}
			mIndicator.draw(canvas, xo + mDisplayBeginCursor.column, sty, Indicator.TYPE_LEFT);
			mIndicator.draw(canvas, xo + mDisplayEndCursor.column, eny, Indicator.TYPE_RIGHT);
		}
		mSlideBar.draw(canvas);
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
		Arrays.fill(mCharWidths, 0);
		mCharWidths[CHAR_TAB] = (mCharWidths[CHAR_SPACE] = ContentPaint.measureText(" ")) * TABSpaceCount;
	}

	// -------------------------
	// -----Private Methods-----
	// -------------------------

	public char getChar(int ind) {
		return S.charAt(ind);
	}

	private void applyTheme() {
		setBackgroundColor(mTheme.getBackgroundColor());
		if (mLexer == null) ContentPaint.setColor(mTheme.getTypeColor(MLexer.TYPE_PURE));
		mIndicator.setHeight(TextHeight);
		LineNumberPaint.setColor(mTheme.getLineNumberColor());
		mSlideBar.onSchemeChange();
	}

	private byte getDraggingCursor(float x, float y) {
		final float ori = x;
		x -= mContentLeftPadding;
		if (mShowLineNumber) x -= (LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH);
		if (isRangeSelecting()) {
			final float sty = LineHeight * (mDisplayBeginCursor.line + 1);
			final float eny = LineHeight * (mDisplayEndCursor.line + 1);
			if (mIndicator.isTouched(x - mDisplayBeginCursor.column, y - sty, Indicator.TYPE_LEFT)) {
				mLastX = ori;
				mLastY = sty - LineHeight * 0.5f;
				return Indicator.TYPE_LEFT;
			}
			if (mIndicator.isTouched(x - mDisplayEndCursor.column, y - eny, Indicator.TYPE_RIGHT)) {
				mLastX = ori;
				mLastY = eny - LineHeight * 0.5f;
				return Indicator.TYPE_RIGHT;
			}
		} else {
			final float sty = LineHeight * (mDisplayCursor.line + 1);
			if (mIndicator.isTouched(x - mDisplayCursor.column, y - sty, Indicator.TYPE_NORMAL)) {
				mLastX = ori;
				mLastY = sty - LineHeight * 0.5f;
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
			if (mEditable) commitChar((char) event.getUnicodeChar(event.getMetaState()));
			return true;
		}
		if (mEditable)
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
		boolean dc = (time - LastClickTime <= DOUBLE_CLICK_INTERVAL) && (Math.abs(x - mLastClickX) <= DOUBLE_CLICK_RANGE) && (Math.abs(y - mLastClickY) <= DOUBLE_CLICK_RANGE);
		mLastClickX = x;
		mLastClickY = y;
		LastClickTime = time;
		Cursor nc = getCursorByPosition(x, y);
		moveCursor(nc);
		mComposingStart = null;
		if (dc) expandSelectionFrom(nc);
		else finishSelecting();
		if (mEditable && mInputMethodManager != null) {
			mInputMethodManager.viewClicked(this);
			mInputMethodManager.showSoftInput(this, 0);
//			_IMM.restartInput(this);
		}
		postInvalidate();
	}

	protected void onFontChange() {
		YOffset = -ContentPaint.ascent();
		TextHeight = ContentPaint.descent() + YOffset;
		LineHeight = TextHeight + mLineTopPadding + mLineBottomPadding;
		mIndicator.setHeight(TextHeight);
		clearCharWidthCache();
		mWordWrappingManager.onUpdate();
		onSelectionUpdate();
		onLineChange();
		requestLayout();
		postInvalidate();
	}

	protected void onLineChange() {
		ContentHeight = (int) (LineHeight * mWordWrappingManager.getTotalCount());
		mYScrollRange = Math.max(ContentHeight - getHeight(), 0);
		if (LineNumberPaint != null)
			LineNumberWidth = LineNumberPaint.measureText("9") * ((int) Math.log10(L.size()) + 1);
		postInvalidate();
	}

	private void springBack() {
		Scroller.springBack(getScrollX(), getScrollY(), 0, Integer.MAX_VALUE, 0, mYScrollRange);
	}

	protected ClipboardManager getClipboardManager() {
		return (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
	}

	protected void onSelectionUpdate() {
		if (mClipboardHelperEnabled) {
			if (mRangeSelecting && mShowingActionMode == null) mClipboardHelper.show();
			else mClipboardHelper.hide();
		}
		if (mSelectListener != null) {
			if (mRangeSelecting) mSelectListener.onSelect(mSelection.clone());
			else mSelectListener.onSelect(new RangeSelection<>(C));
		}
		if (!mRangeSelecting) {
			mDisplayCursor = mWordWrappingManager.getDisplayCursor(C);
			makeCursorVisible(C);
		} else {
			mDisplayBeginCursor = mWordWrappingManager.getDisplayCursor(mSelection.begin);
			mDisplayEndCursor = mWordWrappingManager.getDisplayCursor(mSelection.end);
			makeCursorVisible(mSelection.end);
		}
		if (mEditable && mInputMethodManager != null) {
			int sst, sen;
//			CursorAnchorInfo.Builder builder = new CursorAnchorInfo.Builder().setMatrix(null);
			if (mRangeSelecting) {
				sst = S.Cursor2Index(mSelection.begin);
				sen = S.Cursor2Index(mSelection.end);
			} else {
				sst = sen = getCursorPosition();
				float top = LineHeight * mWordWrappingManager.getLineDisplayStart(C.line);
				float xo = (mShowLineNumber ? LineNumberWidth + LINE_NUMBER_SPLIT_WIDTH : 0) + mContentLeftPadding;
//				builder.setInsertionMarkerLocation(xo + _CursorHorizonOffset, top, top + YOffset, top + TextHeight, CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION);
			}
//			builder.setSelectionRange(sst, sen);
//			_IMM.updateCursorAnchorInfo(this, builder.build());
			mInputMethodManager.updateSelection(this, sst, sen, -1, -1);
//			_IMM.restartInput(this);
		}
	}

	private void setComposingText(char[] cs) {
		if (mComposingStart == null) {
			mComposingStart = C.clone();
			S.insert(getCursor(), cs);
		} else
			S.replace(new RangeSelection<>(mComposingStart, C), cs);
		if (cs.length == 0)
			mComposingStart = null;
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

	private interface ClipboardActionModeHelper {
		void show();

		void hide();
	}

	private static class ClipboardActionModeHelperNative implements ClipboardActionModeHelper {
		private MEdit Content;
		private Context cx;
		private android.view.ActionMode _ActionMode;

		public ClipboardActionModeHelperNative(MEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof Activity)) return;
			if (Content.mShowingActionMode != null) return;
			if (_ActionMode == null)
				((Activity) cx).startActionMode(new android.view.ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
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
					public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
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
					public void onDestroyActionMode(android.view.ActionMode mode) {
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

	private static class ClipboardActionModeHelperCompat implements ClipboardActionModeHelper {
		private MEdit Content;
		private Context cx;
		private androidx.appcompat.view.ActionMode _ActionMode;

		public ClipboardActionModeHelperCompat(MEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (Content.mShowingActionMode != null) return;
			if (_ActionMode == null)
				((AppCompatActivity) cx).startSupportActionMode(new androidx.appcompat.view.ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
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
					public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
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
					public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
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
				Q.mComposingStart = null;
			else
				Q.mComposingStart = S.Index2Cursor(start);
			return true;
		}

		@Override
		public boolean setComposingText(CharSequence text, int newCursorPosition) {
			Q.setComposingText(com.mivik.malax.Editable.CharSequence2CharArray(text, 0, text.length()));
			return true;
		}

		@Override
		public boolean finishComposingText() {
			Q.mComposingStart = null;
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
			mp.setColor(0xC4FFFFFF & parent.mTheme.getSlideBarColor());
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
			final float gRadius = radius * 0.7f;
			final int indicatorColor = P.mTheme.getIndicatorColor();
			final int indicatorGlassColor = indicatorColor & 0x44FFFFFF;
			final Path path = new Path();
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
			mp.setColor(indicatorColor);
			canvas.drawPath(path, mp);
			canvas.drawCircle(0, h - radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(0, h - radius, gRadius, mp);
			mp.setXfermode(null);
			mp.setColor(indicatorGlassColor);
			canvas.drawCircle(0, h - radius, gRadius, mp);

			canvas = new Canvas(c1);
			mp.setColor(indicatorColor);
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(radius, 0, radius * 2, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gRadius, mp);
			mp.setXfermode(null);
			mp.setColor(indicatorGlassColor);
			canvas.drawCircle(radius, radius, gRadius, mp);

			canvas = new Canvas(c2);
			mp.setColor(indicatorColor);
			canvas.drawCircle(radius, radius, radius, mp);
			canvas.drawRect(0, 0, radius, radius, mp);
			mp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			canvas.drawCircle(radius, radius, gRadius, mp);
			mp.setXfermode(null);
			mp.setColor(indicatorGlassColor);
			canvas.drawCircle(radius, radius, gRadius, mp);
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