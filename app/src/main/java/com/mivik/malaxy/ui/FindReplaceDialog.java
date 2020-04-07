package com.mivik.malaxy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.LinearLayoutCompat;
import com.mivik.malaxy.R;
import com.mivik.medit.MEdit;
import com.mivik.mlexer.CursorWrapper;
import com.mivik.mlexer.RangeSelection;

import static com.mivik.malax.BaseMalax.Cursor;

public class FindReplaceDialog extends AlertDialog {
	private MEdit D;
	private HintEditText EditFind, EditReplace;
	private LinearLayoutCompat Layout;
	private FindActionModeHelper _FHelper;
	private ReplaceActionModeHelper _RHelper;
	private LoadingDialog _Replacing;

	public FindReplaceDialog(MEdit edit) {
		super(edit.getContext());
		setTitle(R.string.title_find_or_replace);
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		D = edit;
		EditFind = new HintEditText(getContext());
		EditFind.setHint(getContext().getString(R.string.dialog_find_content));
		EditReplace = new HintEditText(getContext());
		EditReplace.setHint(getContext().getString(R.string.dialog_replace_content));
		Layout = new LinearLayoutCompat(getContext());
		Layout.setOrientation(LinearLayoutCompat.VERTICAL);
		Layout.addView(EditFind);
		Layout.addView(EditReplace);
		_FHelper = new FindActionModeHelper(D);
		_RHelper = new ReplaceActionModeHelper(D);
		_Replacing = new LoadingDialog(D.getContext());
		_Replacing.setMessage(R.string.dialog_replace_replacing);
		setView(Layout);
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.dialog_find), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String s = EditFind.getText().toString();
				if (s.length() == 0) return;
				_FHelper.ss = D.find(s.toCharArray());
				if (_FHelper.ss.length == 0) {
					UI.toast(D.getContext(), getContext().getString(R.string.dialog_find_or_replace_text_not_found));
					return;
				}
				_FHelper.Q = s.toCharArray();
				final Cursor P = D.getCursor();
				int i;
				for (i = 0; i < _FHelper.ss.length; i++)
					if (_FHelper.ss[i].compareTo(P) > 0) break;
				if (i == _FHelper.ss.length) --i;
				_FHelper.ind = i;
				_FHelper.show();
			}
		});
		setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(R.string.dialog_replace), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String s = EditFind.getText().toString();
				if (s.length() == 0) return;
				char[] Q = _RHelper.Q = s.toCharArray();
				final Cursor P = D.getCursor();
				final int pos = D.S.Cursor2Index(P);
				int len = D.getTextLength() - Q.length;
				CursorWrapper<Cursor> i;
				F:
				{
					i = new CursorWrapper<>(D.S, P);
					for (; i.ind <= len; i.moveForward())
						if (D.equal(i.cursor, Q)) {
							break F;
						}
					for (i.set(D.S.getBeginCursor()); i.ind < pos; i.moveForward())
						if (D.equal(i.cursor, Q)) {
							break F;
						}
					UI.toast(D.getContext(), getContext().getString(R.string.dialog_find_or_replace_text_not_found));
					return;
				}
				_RHelper.ind = i.cursor.clone();
				_RHelper.T = EditReplace.getText().toString().toCharArray();
				_RHelper.show();
			}
		});
		setButton(DialogInterface.BUTTON_NEUTRAL, getContext().getString(R.string.dialog_replace_replace_all), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				_Replacing.show();
				new Thread() {
					@Override
					public void run() {
						String s = EditFind.getText().toString();
						if (s.length() == 0) return;
						char[] Q = s.toCharArray();
						char[] T = EditReplace.getText().toString().toCharArray();
						int len = D.getTextLength();
						final int delta = T.length - Q.length;
						int time = 0;
						Cursor last = D.S.getBeginCursor();
						final StringBuilder ncs = new StringBuilder(D.getTextLength());
						final int tar = D.getTextLength() - Q.length;
						for (CursorWrapper<Cursor> i = new CursorWrapper<>(D.S, D.S.getBeginCursor());
							 i.ind <= tar;
							 i.moveForward())
							if (D.equal(i.cursor, Q)) {
								StringBuilder builder = D.S.subStringBuilder(new RangeSelection<>(last, i.cursor));
								ncs.append(builder);
								ncs.append(T);
								last = i.cursor.clone();
								D.S.moveForward(last, Q.length);
								i.set(last);
								i.moveBack();
								len += delta;
								time++;
							}
						ncs.append(D.S.subStringBuilder(new RangeSelection<>(last, D.S.getEndCursor())));
						final String msg = String.format(getContext().getString(R.string.dialog_replace_replaced), time);
						UI.onUI(new Runnable() {
							@Override
							public void run() {
								D.setText(ncs.toString());
								UI.toast(D.getContext(), msg);
								_Replacing.dismiss();
							}
						});
					}
				}.start();
			}
		});
	}

	@Override
	public void show() {
		EditFind.getText().clear();
		EditReplace.getText().clear();
		super.show();
	}

	private static class FindActionModeHelper {
		private MEdit Content;
		private Context cx;
		private ActionMode _ActionMode;
		private int ind;
		private Cursor[] ss;
		char[] Q;

		public FindActionModeHelper(MEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (_ActionMode == null) {
				((AppCompatActivity) cx).startSupportActionMode(new ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						Content.onStartActionMode(_ActionMode = mode);
						mode.setTitle(R.string.dialog_find);
						mode.setSubtitle(new String(Q));
						menu.add(0, 0, 0, R.string.dialog_find_back).setIcon(UI.tintDrawable(cx, R.mipmap.icon_left, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 1, 0, R.string.dialog_find_forward).setIcon(UI.tintDrawable(cx, R.mipmap.icon_right, UI.IconColor)).setShowAsActionFlags(2);
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
								if ((--ind) == -1) ind += ss.length;
								break;
							case 1:
								if ((++ind) == ss.length) ind -= ss.length;
								break;
							default:
								return false;
						}
						Content.setSelectionRange(Content.S.fromBegin(ss[ind], Q.length));
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode p1) {
						Content.finishSelecting();
						Content.onHideActionMode();
						_ActionMode = null;
					}
				});
			} else _ActionMode.setSubtitle(new String(Q));
			Content.setSelectionRange(Content.S.fromBegin(ss[ind], Q.length));
		}

		public void hide() {
			if (!(cx instanceof Activity)) return;
			if (_ActionMode != null) {
				_ActionMode.finish();
				_ActionMode = null;
			}
		}
	}

	private static class ReplaceActionModeHelper {
		private MEdit Content;
		private Context cx;
		private ActionMode _ActionMode;
		private Cursor ind;
		char[] T;
		char[] Q;

		public ReplaceActionModeHelper(MEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (_ActionMode == null) {
				((AppCompatActivity) cx).startSupportActionMode(new ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						Content.onStartActionMode(_ActionMode = mode);
						mode.setTitle(R.string.dialog_replace);
						mode.setSubtitle(getStateString());
						menu.add(0, 0, 0, R.string.dialog_replace_back).setIcon(UI.tintDrawable(cx, R.mipmap.icon_left, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 1, 0, R.string.dialog_replace_replace).setIcon(UI.tintDrawable(cx, R.mipmap.icon_right, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 2, 0, R.string.dialog_replace_skip).setIcon(UI.tintDrawable(cx, R.mipmap.icon_skip, UI.IconColor)).setShowAsActionFlags(2);
						return true;
					}

					@Override
					public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
						S:
						switch (item.getItemId()) {
							case 0: {
								CursorWrapper<Cursor> i = new CursorWrapper<>(Content.S, ind);
								for (; i.moveBack(); )
									if (Content.equal(i.cursor, Q)) {
										ind = i.cursor.clone();
										break S;
									}
								i.move(Content.getTextLength() - Q.length);
								for (; i.cursor.compareTo(ind) > 0; i.moveBack())
									if (Content.equal(i.cursor, Q)) {
										ind = i.cursor.clone();
										break S;
									}
								_ActionMode.finish();
								return true;
							}
							case 1:
								Content.S.replace(Content.S.fromBegin(ind, Q.length), T);
							case 2: {
								final int len = Content.getTextLength() - Q.length;
								CursorWrapper<Cursor> i = new CursorWrapper<>(Content.S, ind);
								i.moveForward();
								for (; i.ind <= len; i.moveForward())
									if (Content.equal(i.cursor, Q)) {
										ind = i.cursor.clone();
										break S;
									}
								for (i.set(Content.S.getBeginCursor()); i.cursor.compareTo(ind) < 0; i.moveForward())
									if (Content.equal(i.cursor, Q)) {
										ind = i.cursor.clone();
										break S;
									}
								_ActionMode.finish();
								return true;
							}
							default:
								return false;
						}
						Content.setSelectionRange(Content.S.fromBegin(ind, Q.length));
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode mode) {
						Content.finishSelecting();
						Content.onHideActionMode();
						_ActionMode = null;
					}
				});
			} else _ActionMode.setSubtitle(getStateString());
			Content.setSelectionRange(Content.S.fromBegin(ind, Q.length));
		}

		private String getStateString() {
			return String.format(cx.getString(R.string.dialog_replace_state_text), new String(Q), new String(T));
		}

		public void hide() {
			if (!(cx instanceof Activity)) return;
			if (_ActionMode != null) {
				_ActionMode.finish();
				_ActionMode = null;
			}
		}
	}
}