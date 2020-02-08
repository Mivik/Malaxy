package com.mivik.medit;

import com.mivik.malax.BaseMalax;
import com.mivik.malax.LineManager;

import static com.mivik.malax.BaseMalax.Cursor;

public class SplitLineManager implements BaseMalax.ContentChangeListener {
	private MEdit edit;
	private int[] S = new int[16];
	private boolean enabled = true;
	private UpdateListener listener;

	public SplitLineManager(MEdit edit) {
		this.edit = edit;
	}

	public void setUpdateListener(UpdateListener listener) {
		this.listener = listener;
	}

	public UpdateListener getUpdateListener() {
		return listener;
	}

	public void setEnabled(boolean flag) {
		if (this.enabled==flag) return;
		if (this.enabled = flag) onUpdate();
	}

	public boolean isEnabled() {
		return enabled;
	}

	// The "column" of returned value is x offset!!
	public Cursor getDisplayCursor(Cursor x) {
		if (!enabled) {
			final BaseMalax malax = edit.S.getMalax();
			final char[] cs = malax.getRawChars()[x.line];
			float cur = 0;
			for (int i = 0; i < x.column; i++) cur += edit.getCharWidth(cs[i]);
			return new Cursor(x.line, (int) cur);
		}
		if (edit.S == null) return null;
		final BaseMalax malax = edit.S.getMalax();
		final char[] cs = malax.getRawChars()[x.line];
		final float width = edit.getLineWidth();
		float cur = 0;
		int off = 0;
		for (int i = 0; i < x.column; i++) {
			cur += edit.getCharWidth(cs[i]);
			if (cur >= width) {
				cur = 0;
				++off;
				--i;
			}
		}
		return new Cursor(getLineDisplayStart(x.line) + off, (int) cur);
	}

	public Cursor getOriginalCursor(Cursor x) {
		if (!enabled) {
			final BaseMalax malax = edit.S.getMalax();
			final int len = malax.getLineManager().getTrimmed(x.line);
			final char[] cs = malax.getRawChars()[x.line];
			int i = 0;
			for (float sum = -x.column; i < len; i++) {
				if ((sum += edit.getCharWidth(cs[i])) >= 0) {
					if ((-(sum - edit.getCharWidth(cs[i]))) > sum) // 是前面的更逼近一些
						i++;
					break;
				}
			}
			return new Cursor(x.line, i);
		}
		final int line = findStartDrawLine(x.line);
		final BaseMalax malax = edit.S.getMalax();
		final int len = malax.getLineManager().getTrimmed(line);
		final char[] cs = malax.getRawChars()[line];
		final float width = edit.getLineWidth();
		if (len == 0) return new Cursor(line, 0);
		int off = x.line - getLineDisplayStart(line);
		float cur = 0;
		int i = 0;
		if (off > 0) {
			for (; ; i++) {
				cur += edit.getCharWidth(cs[i]);
				if (cur >= width) {
					cur = 0;
					if (--off == 0) break;
					--i;
				}
			}
		}
		for (float sum = -x.column; i < len; i++) {
			if ((sum += edit.getCharWidth(cs[i])) >= 0) {
				if ((-(sum - edit.getCharWidth(cs[i]))) > sum) // 是前面的更逼近一些
					i++;
				break;
			}
		}
		return new Cursor(line, i);
	}

	public int getTotalCount() {
		final int size = edit.S.getLineManager().size();
		if (size == 0) return 0;
		if (!enabled) return size;
		return S[size - 1];
	}

	public int findStartDrawLine(int display) {
		if (!enabled) return display;
		int l = 0, r = edit.S.getLineManager().size() - 1, mid, ans = 0;
		while (l <= r) {
			mid = (l + r) >> 1;
			if (display >= getLineDisplayStart(mid)) {
				l = mid + 1;
				ans = mid;
			} else r = mid - 1;
		}
		return ans;
	}

	public void onUpdate() {
		if (!enabled) return;
		final LineManager line = edit.S.getLineManager();
		final int tot = line.size();
		if (tot == 0) return;
		if (tot > S.length) {
			int[] dst = new int[tot];
			System.arraycopy(S, 0, dst, 0, S.length);
			S = dst;
		}
		S[0] = calcLineDisplayCount(0);
		for (int i = 1; i < tot; i++)
			S[i] = calcLineDisplayCount(i) + S[i - 1];
		if (listener != null) listener.onUpdate();
	}

	public int getLineDisplayCount(int x) {
		if (!enabled) return 1;
		if (x > 0) return S[x] - S[x - 1];
		return S[x];
	}

	public int getLineDisplayStart(int x) {
		if (!enabled) return x;
		if (x > 0) return S[x - 1];
		return 0;
	}

	public int getLineDisplayEnd(int x) {
		if (!enabled) return x;
		return S[x] - 1;
	}

	private int calcLineDisplayCount(int line) {
		final BaseMalax malax = edit.S.getMalax();
		final int len = malax.getLineManager().getTrimmed(line);
		final char[] cs = malax.getRawChars()[line];
		final float width = edit.getLineWidth();
		if (width <= 0) return 0;
		float cur = 0;
		int ret = 1;
		for (int i = 0; i < len; i++) {
			cur += edit.getCharWidth(cs[i]);
			if (cur >= width) {
				cur = 0;
				++ret;
				--i;
			}
		}
		return ret;
	}

	@Override
	public void onExpand(int st, int en) {
		if (!enabled) return;
		final LineManager line = edit.S.getLineManager();
		final int tot = line.size();
		int[] dst;
		if (tot > S.length) {
			dst = new int[tot];
			System.arraycopy(S, 0, dst, 0, st);
		} else dst = S;
		final int del = en - st + 1;
		for (int i = tot - 1; i > en; i--) dst[i] = S[i - del];
		int base = getLineDisplayStart(st);
		for (int i = st; i <= en; i++)
			dst[i] = base += calcLineDisplayCount(i);
		base -= getLineDisplayStart(st);
		for (int i = en; i < tot; i++) dst[i] += base;
		S = dst;
		if (listener != null) listener.onUpdate();
	}

	@Override
	public void onMerge(int st, int en) {
		if (!enabled) return;
		final LineManager line = edit.S.getLineManager();
		final int tot = line.size();
		final int del = en - st;
		final int len = S[en] - getLineDisplayStart(st);
		for (int i = st; i < tot; i++) S[i] = S[i + del] - len;
		if (listener != null) listener.onUpdate();
	}

	@Override
	public void onLineUpdated(int x) {
		if (!enabled) return;
		final int tot = edit.S.getLineManager().size();
		final int del = calcLineDisplayCount(x) - getLineDisplayCount(x);
		if (del == 0) return;
		for (; x < tot; x++) S[x] += del;
		if (listener != null) listener.onUpdate();
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('[');
		final LineManager line = edit.S.getLineManager();
		final int tot = line.size();
		for (int i = 0; i < tot; i++) {
			ret.append(getLineDisplayCount(i));
			if (i != tot - 1) ret.append(' ');
		}
		ret.append(']');
		return ret.toString();
	}

	public interface UpdateListener {
		void onUpdate();
	}
}