package com.mivik.medit.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.Menu;
import android.view.MenuItem;
import com.mivik.medit.LoadingDialog;
import com.xsjiong.vedit.R;
import com.mivik.medit.VEdit;

public class FindReplaceDialog extends AlertDialog {
	private VEdit D;
	private HintEditText EditFind, EditReplace;
	private LinearLayoutCompat Layout;
	private FindActionModeHelper _FHelper;
	private ReplaceActionModeHelper _RHelper;
	private LoadingDialog _Replacing;

	public FindReplaceDialog(VEdit edit) {
		super(edit.getContext());
		setTitle("查找替换");
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		D = edit;
		EditFind = new HintEditText(getContext());
		EditFind.setHint("查找内容");
		EditReplace = new HintEditText(getContext());
		EditReplace.setHint("替换内容");
		Layout = new LinearLayoutCompat(getContext());
		Layout.setOrientation(LinearLayoutCompat.VERTICAL);
		Layout.addView(EditFind);
		Layout.addView(EditReplace);
		_FHelper = new FindActionModeHelper(D);
		_RHelper = new ReplaceActionModeHelper(D);
		_Replacing = new LoadingDialog(D.getContext());
		_Replacing.setMessage("替换中");
		setView(Layout);
		setButton(DialogInterface.BUTTON_POSITIVE, "查找", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String s = EditFind.getText().toString();
				if (s.length() == 0) return;
				_FHelper.ss = D.find(s.toCharArray());
				if (_FHelper.ss.length == 0) {
					UI.toast(D.getContext(), "没有找到内容");
					return;
				}
				_FHelper.Q = s.toCharArray();
				final int P = D.getCursorPosition();
				int i;
				for (i = 0; i < _FHelper.ss.length; i++)
					if (_FHelper.ss[i] > P) break;
				if (i == _FHelper.ss.length) --i;
				_FHelper.ind = i;
				_FHelper.show();
			}
		});
		setButton(DialogInterface.BUTTON_NEGATIVE, "替换", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String s = EditFind.getText().toString();
				if (s.length() == 0) return;
				char[] Q = _RHelper.Q = s.toCharArray();
				final int P = D.getCursorPosition();
				int i;
				int len = D.getTextLength() - Q.length;
				F:
				{
					for (i = P; i <= len; i++)
						if (D.equal(i, Q)) {
							break F;
						}
					for (i = 0; i < P; i++)
						if (D.equal(i, Q)) {
							break F;
						}
					UI.toast(D.getContext(), "没有找到内容");
					return;
				}
				_RHelper.ind = i;
				_RHelper.T = EditReplace.getText().toString().toCharArray();
				_RHelper.show();
			}
		});
		setButton(DialogInterface.BUTTON_NEUTRAL, "全部替换", new OnClickListener() {
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
						int last = 0;
						int my = 0;
						final char[] ncs = new char[D.getTextLength()];
						final char[] S = D.getRawChars();
						for (int i = 0; i <= D.getTextLength() - Q.length; i++)
							if (D.equal(i, Q)) {
								System.arraycopy(S, last, ncs, my, i - last);
								System.arraycopy(T, 0, ncs, my += (i - last), T.length);
								my += T.length;
								i = (last = i + Q.length) - 1;
								len += delta;
								time++;
							}
						System.arraycopy(S, last, ncs, my, D.getTextLength() - last);
						final int len2 = len;
						final String msg = "已替换" + time + "处";
						UI.onUI(new Runnable() {
							@Override
							public void run() {
								D.replace(0, ncs.length, ncs, 0, len2);
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
		private VEdit Content;
		private Context cx;
		private android.support.v7.view.ActionMode _ActionMode;
		private int ind;
		private int[] ss;
		char[] Q;

		public FindActionModeHelper(VEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (_ActionMode == null) {
				((AppCompatActivity) cx).startSupportActionMode(new android.support.v7.view.ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
						Content.onStartActionMode(_ActionMode = mode);
						mode.setTitle("查找");
						mode.setSubtitle(new String(Q));
						menu.add(0, 0, 0, "后退").setIcon(UI.tintDrawable(cx, R.mipmap.icon_left, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 1, 0, "前进").setIcon(UI.tintDrawable(cx, R.mipmap.icon_right, UI.IconColor)).setShowAsActionFlags(2);
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
								if ((--ind) == -1) ind += ss.length;
								break;
							case 1:
								if ((++ind) == ss.length) ind -= ss.length;
								break;

							default:
								return false;
						}
						Content.setSelectionRange(ss[ind], ss[ind] + Q.length);
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode p1) {
						Content.onHideActionMode();
						Content.finishSelecting();
						_ActionMode = null;
					}
				});
			} else _ActionMode.setSubtitle(new String(Q));
			Content.setSelectionRange(ss[ind], ss[ind] + Q.length);
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
		private VEdit Content;
		private Context cx;
		private android.support.v7.view.ActionMode _ActionMode;
		private int ind;
		char[] T;
		char[] Q;

		public ReplaceActionModeHelper(VEdit textField) {
			Content = textField;
			cx = Content.getContext();
		}

		public void show() {
			if (!(cx instanceof AppCompatActivity)) return;
			if (_ActionMode == null) {
				((AppCompatActivity) cx).startSupportActionMode(new android.support.v7.view.ActionMode.Callback() {
					@Override
					public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
						Content.onStartActionMode(_ActionMode = mode);
						mode.setTitle("替换");
						StringBuffer buf = new StringBuffer();
						mode.setSubtitle(buf.append(Q).append(" -> ").append(T).toString());
						menu.add(0, 0, 0, "后退").setIcon(UI.tintDrawable(cx, R.mipmap.icon_left, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 1, 0, "替换").setIcon(UI.tintDrawable(cx, R.mipmap.icon_right, UI.IconColor)).setShowAsActionFlags(2);
						menu.add(0, 2, 0, "跳过").setIcon(UI.tintDrawable(cx, R.mipmap.icon_skip, UI.IconColor)).setShowAsActionFlags(2);
						return true;
					}

					@Override
					public boolean onPrepareActionMode(android.support.v7.view.ActionMode mode, Menu menu) {
						return false;
					}

					@Override
					public boolean onActionItemClicked(android.support.v7.view.ActionMode mode, MenuItem item) {
						S:
						switch (item.getItemId()) {
							case 0: {
								int i;
								for (i = ind - 1; i >= 0; i--)
									if (Content.equal(i, Q)) {
										ind = i;
										break S;
									}
								for (i = Content.getTextLength() - Q.length; i > ind; i--)
									if (Content.equal(i, Q)) {
										ind = i;
										break S;
									}
								_ActionMode.finish();
								break;
							}
							case 1:
								Content.replace(ind, ind + Q.length, T);
							case 2: {
								int i;
								int len = Content.getTextLength() - Q.length;
								for (i = ind + 1; i <= len; i++)
									if (Content.equal(i, Q)) {
										ind = i;
										break S;
									}
								for (i = 0; i < ind; i++)
									if (Content.equal(i, Q)) {
										ind = i;
										break S;
									}
								_ActionMode.finish();
								break;
							}
							default:
								return false;
						}
						Content.setSelectionRange(ind, ind + Q.length);
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode p1) {
						Content.onHideActionMode();
						Content.finishSelecting();
						_ActionMode = null;
					}
				});
			} else {
				StringBuffer buf = new StringBuffer();
				_ActionMode.setSubtitle(buf.append(Q).append(" -> ").append(T).toString());
			}
			Content.setSelectionRange(ind, ind + Q.length);
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