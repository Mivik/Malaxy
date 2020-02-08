package com.mivik.medit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import com.mivik.malax.Malax;
import com.mivik.malax.WrappedEditable;
import com.mivik.medit.theme.MEditTheme;
import com.mivik.mlexer.RangeSelection;

import java.util.Arrays;

import static com.mivik.malax.BaseMalax.Cursor;

public class Malaxy extends MEdit implements WrappedEditable.EditActionListener {
	private Cursor ReadCursor;
	private InputChannel input;
	private final OutputChannel output = new MalaxyOutputChannel();
	private final byte[] InputLock = new byte[0];

	public Malaxy(Context cx) {
		this(cx, null, 0);
	}

	public Malaxy(Context cx, AttributeSet attrs) {
		this(cx, attrs, 0);
	}

	public Malaxy(Context cx, AttributeSet attrs, int defStyle) {
		super(cx, attrs, defStyle);
		setSplitLineEnabled(true);
		setShowLineNumber(false);
		setTheme(MalaxyTheme.getInstance());
		setLexer(null);
		setTypeface(Typeface.MONOSPACE);
		setIndicator(new MalaxyIndicator(this));
		setBlinkCursor(false);
		setShowCursorLine(false);
		setHighlightLine(false);
		setEventHandler(new ZoomHelper());
		addEditActionListener(this);
	}

	private void scrollToBottom() {
		scrollTo(Math.max(ContentHeight - getHeight(), 0), getScrollY());
	}

	public void setInputChannel(InputChannel input) {
		synchronized (InputLock) {
			this.input = input;
		}
	}

	@Override
	public void setMalax(Malax malax) {
		String input = null;
		if (ReadCursor != null)
			input = malax.substring(new RangeSelection<>(ReadCursor, malax.getEndCursor()));
		super.setMalax(malax);
		ReadCursor = malax.getEndCursor();
		if (input != null) malax.insert(ReadCursor, input);
	}

	@Override
	public boolean beforeAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
		if (editAction instanceof WrappedEditable.ReplaceAction) {
			WrappedEditable.ReplaceAction action = (WrappedEditable.ReplaceAction) editAction;
			if (ReadCursor.compareTo((Cursor) action.osel.begin) > 0) return true;
		} else if (editAction instanceof WrappedEditable.DeleteCharsAction) {
			WrappedEditable.DeleteCharsAction action = (WrappedEditable.DeleteCharsAction) editAction;
			if (ReadCursor.compareTo((Cursor) action.sel.begin) > 0) return true;
		} else if (editAction instanceof WrappedEditable.DeleteCharAction) {
			WrappedEditable.DeleteCharAction action = (WrappedEditable.DeleteCharAction) editAction;
			if (ReadCursor.compareTo((Cursor) action.lef) > 0) return true;
		} else if (editAction instanceof WrappedEditable.ClearAction) return true;
		else if (editAction instanceof WrappedEditable.SetTextAction) return true;
		else if (editAction instanceof WrappedEditable.InsertCharAction) {
			WrappedEditable.InsertCharAction action = (WrappedEditable.InsertCharAction) editAction;
			if (ReadCursor.compareTo((Cursor) action.lef) > 0) action.lef = S.getEndCursor();
		} else if (editAction instanceof WrappedEditable.InsertCharsAction) {
			WrappedEditable.InsertCharsAction action = (WrappedEditable.InsertCharsAction) editAction;
			if (ReadCursor.compareTo((Cursor) action.lef) > 0) action.lef = S.getEndCursor();
		}
		return false;
	}

	@Override
	public void afterAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
		if (editAction instanceof WrappedEditable.InsertCharAction) {
			WrappedEditable.InsertCharAction action = (WrappedEditable.InsertCharAction) editAction;
			if (action.ch == '\n') {
				if (input != null) {
					synchronized (InputLock) {
						if (input != null) {
							char[] data = S.substring(new RangeSelection<>(ReadCursor, S.getEndCursor())).toCharArray();
							input.onRead(data, 0, data.length);
						}
					}
				}
				ReadCursor = S.getEndCursor();
			}
		}
	}

	@Override
	public android.view.inputmethod.InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		android.view.inputmethod.InputConnection ret = super.onCreateInputConnection(outAttrs);
		outAttrs.imeOptions = EditorInfo.IME_NULL
				| EditorInfo.IME_FLAG_NO_ENTER_ACTION
				| EditorInfo.IME_FLAG_NO_FULLSCREEN
				| EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION;
		outAttrs.inputType = EditorInfo.TYPE_NULL;
		return ret;
	}

	public OutputChannel getOutputChannel() {
		return output;
	}

	private class MalaxyOutputChannel extends OutputChannel {
		@Override
		public void onWrite(final char[] cs, final int off, final int len) {
			post(new Runnable() {
				@Override
				public void run() {
					ReadCursor = S.insert(ReadCursor, cs, off, len);
					makeCursorVisible(ReadCursor);
				}
			});
		}
	}

	private static class MalaxyIndicator extends Indicator {
		private Paint paint;
		private float height;
		private float width;

		public MalaxyIndicator(Malaxy malaxy) {
			super(malaxy);
			paint = new Paint();
			paint.setAntiAlias(false);
			paint.setStyle(Paint.Style.FILL);
		}

		@Override
		public void setHeight(float height) {
			this.height = height;
			this.width = P.getCharWidth(' ');
			paint.setColor(P.getTheme().getIndicatorColor());
		}

		@Override
		public void draw(Canvas canvas, float x, float y, byte type) {
			if (type != Indicator.TYPE_NORMAL) return;
			canvas.drawRect(x, y - height, x + width, y, paint);
		}

		@Override
		public boolean isTouched(float x, float y, byte type) {
			return false;
		}

		@Override
		public void recycle() {
		}
	}

	private static class MalaxyTheme extends MEditTheme {
		private static MalaxyTheme INSTANCE;

		public static MalaxyTheme getInstance() {
			if (INSTANCE == null) INSTANCE = new MalaxyTheme();
			return INSTANCE;
		}

		private MalaxyTheme() {
			final int black = 0xFF000000;
			final int gray = 0xFF555555;
			final int white = 0xFFFFFFFF;
			final int transparent = 0;
			setBackgroundColor(black);
			setSplitLineColor(transparent);
			setSelectionColor(gray);
			setCursorLineColor(white);
			setIndicatorColor(gray & 0xAAFFFFFF);
			setIndicatorGlassColor(transparent);
			setLineNumberColor(transparent);
			setSlideBarColor(gray);
			Arrays.fill(C, white);
		}
	}

	private static class ZoomHelper implements MEdit.EventHandler {
		public static final float MIN_TEXT_SIZE = 16;

		private int cnt = 0;
		private float oldDist;
		private float textSize = 0;
		private float relativeScrollY = 0;
		private boolean ok;
		private Cursor first;

		@Override
		public boolean handleEvent(MEdit medit, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					cnt = 1;
					ok = false;
					return false;
				case MotionEvent.ACTION_UP:
					cnt = 0;
					return ok;
				case MotionEvent.ACTION_POINTER_DOWN:
					oldDist = distant(event);
					++cnt;
					if (cnt == 2) {
						ok = true;
						relativeScrollY = 0;
						first = medit.getFirstVisibleCursor();
						textSize = medit.getTextSize();
						relativeScrollY = (float) medit.getScrollY() / medit.getContentHeight();
					}
					return cnt >= 2;
				case MotionEvent.ACTION_POINTER_UP:
					--cnt;
					return ok;
				case MotionEvent.ACTION_MOVE:
					if (cnt >= 2) {
						float newDist = distant(event);
						if (newDist > oldDist + 1 || newDist < oldDist - 1) {
							textSize *= (newDist / oldDist);
							if (textSize < MIN_TEXT_SIZE) textSize = MIN_TEXT_SIZE;
							medit.setTextSize(textSize);
							medit.setScrollY((int) (medit.getContentHeight() * relativeScrollY));
							medit.makeCursorFirstVisible(first);
							oldDist = newDist;
						}
						return true;
					}
					return ok;
				default:
					return false;
			}
		}

		private static float distant(MotionEvent event) {
			float x = event.getX(0) - event.getX(1);
			float y = event.getY(0) - event.getY(1);
			return (float) Math.sqrt(x * x + y * y);
		}
	}
}