package com.mivik.malaxy;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ViewCompat;
import com.mivik.malax.WrappedEditable;
import com.mivik.malaxy.ui.FindReplaceDialog;
import com.mivik.malaxy.ui.LoadingDialog;
import com.mivik.malaxy.ui.MultiContentManager;
import com.mivik.malaxy.ui.UI;
import com.mivik.malaxy.util.MutableInteger;
import com.mivik.medit.MEdit;
import com.mivik.medit.theme.MEditThemeDark;
import com.mivik.medit.theme.MEditThemeLight;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class EditActivity extends BaseActivity implements MultiContentManager.EditDataChooseListener, WrappedEditable.EditActionListener, MultiContentManager.EditDataClickListener, Const {
	public static final char[] SYMBOLS = {'\t', '{', '}', '(', ')', ';', ',', '.', '=', '\"', '<', '>', '&', '+', '-', '*', '/', '[', ']', '|', '!', '?', '\\', ':', '_'};
	public static final String TAB = "->";

	public static final int REQUEST_CODE_SETTING = 1;
	public static final int REQUEST_CODE_CHOOSE_FILE = 2;

	private LinearLayoutCompat Container;
	private MultiContentManager ContentManager;
	private MEdit Content;
	private Toolbar Title;
	private LoadingDialog Loading;
	private LinearLayoutCompat SymbolLayout;
	private FindReplaceDialog FRDialog;
	private MenuItem MenuItemEncodingReload;
	private ChooseFileListener _ChooseFileListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		Title = new Toolbar(this);
		Title.setTitle(R.string.app_name);
		ViewCompat.setElevation(Title, UI.dp2px(5));
		Title.setBackgroundColor(UI.ThemeColor);
		Title.setTitleTextColor(UI.AccentColor);
		setSupportActionBar(Title);
		Container = new LinearLayoutCompat(this);
		Container.setOrientation(LinearLayoutCompat.VERTICAL);
		Container.addView(Title);
		ContentManager = new MultiContentManager(this);
		ContentManager.setTheme(MEditThemeDark.getInstance());
		ContentManager.setEditDataClickListener(this);
		ContentManager.setEditDataChooseListener(this);
		Content = ContentManager.getContent();
		Content.addEditActionListener(this);
		onSettingsChanged();
		{
			LinearLayoutCompat.LayoutParams para = new LinearLayoutCompat.LayoutParams(-1, 0);
			para.weight = 1;
			Container.addView(ContentManager, para);
		}
		initSymbolLayout();
		Container.addView(SymbolLayout, -1, -2);
		setContentView(Container);

		Loading = new LoadingDialog(this);
		FRDialog = new FindReplaceDialog(Content);
		Content.addEditActionListener(new CodeIndentListener());
//		startActivity(new Intent(this, MalaxyActivity.class));
	}

	private void initSymbolLayout() {
		SymbolLayout = new LinearLayoutCompat(this);
		SymbolLayout.setOrientation(LinearLayoutCompat.HORIZONTAL);
		SymbolLayout.setBackgroundColor(UI.ThemeColor);
		SymbolLayout.setAlpha(0.7f);
		LinearLayoutCompat symbolContent = new LinearLayoutCompat(this);
		symbolContent.setOrientation(LinearLayoutCompat.HORIZONTAL);
		int w = UI.AccentColor;
		TextView tv;
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(UI.dp2px(38), UI.dp2px(38));
		for (int i = 0; i < SYMBOLS.length; i++) {
			tv = new TextView(this);
			tv.setTextColor(w);
			tv.setText(SYMBOLS[i] == '\t' ? TAB : Character.toString(SYMBOLS[i]));
			tv.setTextSize(20);
			tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
			tv.setGravity(Gravity.CENTER);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Content.commitChars((((TextView) v).getText().toString().equals(TAB) ? "\t" : ((TextView) v).getText().toString()).toCharArray());
				}
			});
			symbolContent.addView(tv, p);
		}
		HorizontalScrollView sc = new HorizontalScrollView(this);
		sc.addView(symbolContent);
		LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(-1, -2);
		SymbolLayout.addView(sc, para);
	}

	@Override
	protected void onPause() {
		Content.hideIME();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Content.hideIME();
		super.onDestroy();
	}

	private final boolean requestStoragePermissions() {
		final String[] permissions = getResources().getStringArray(R.array.app_permissions);
		if (isAllPermissionsGranted(permissions)) return false;
		getPermissions(permissions, getResources().getStringArray(R.array.app_permission_explanations), false);
		return true;
	}

	@Override
	public void onEditDataChose(final MultiContentManager.EditData data) {
		UI.onUI(new Runnable() {
			@Override
			public void run() {
				if (MenuItemEncodingReload != null)
					MenuItemEncodingReload.setEnabled(data.getFile() != null);
			}
		});
	}

	@Override
	public boolean beforeAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
		return false;
	}

	@Override
	public void afterAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
		MultiContentManager.EditData data = ContentManager.getCurrentEditData();
		if (data.saved) {
			data.saved = false;
			ContentManager.onEditDataUpdated(ContentManager.getIndex());
		}
	}

	@Override
	public void onEditDataClicked(final MultiContentManager.EditData data) {
		if (data.index == ContentManager.getIndex()) {
			if (!data.saved) {
				new AlertDialog.Builder(this).setTitle(R.string.dialog_note).setMessage(R.string.dialog_sure_to_close_unsaved_file).setPositiveButton(R.string.dialog_save_and_close_file, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SaveTab(data, new Runnable() {
							@Override
							public void run() {
								ContentManager.deleteTab(data.index);
							}
						});
					}
				}).setNeutralButton(R.string.dialog_close_file, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ContentManager.deleteTab(data.index);
					}
				}).setCancelable(true).show();
			} else {
				ContentManager.deleteTab(data.index);
			}
		}
	}

	private void SaveTab(final MultiContentManager.EditData data, final Runnable action) {
		if (requestStoragePermissions()) return;
		if (data.saved) {
			if (action != null) action.run();
			return;
		}
		if (data.getFile() == null) {
			setChooseFileListener(new ChooseFileListener() {
				@Override
				public void onChoose(File f) {
					data.setFile(f);
					if (MenuItemEncodingReload != null) MenuItemEncodingReload.setEnabled(true);
					SaveTab(data, action);
					ContentManager.onEditDataUpdated(data.index);
				}
			});
			ChooseFileActivity.createFile(this, REQUEST_CODE_CHOOSE_FILE);
			return;
		}
		try {
			FileOutputStream out = new FileOutputStream(data.getFile());
			out.write(Content.getText().getBytes(data.charset));
			out.close();
			data.saved = true;
			ContentManager.onEditDataUpdated(data.index);
			if (action != null) action.run();
		} catch (Throwable t) {
			UI.showError(this, t);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu sm = menu.addSubMenu(0, 0, 0, R.string.menu_file).setIcon(UI.tintDrawable(this, R.mipmap.icon_directory, UI.AccentColor));
		sm.getItem().setShowAsActionFlags(2);
		sm.add(0, 1, 0, R.string.menu_file_new);
		sm.add(0, 2, 0, R.string.menu_file_open);
		sm.add(0, 3, 0, R.string.menu_file_save);
		sm.add(0, 4, 0, R.string.menu_file_save_as);
		SubMenu encoding = sm.addSubMenu(0, 0, 0, R.string.menu_file_encoding);
		MenuItemEncodingReload = encoding.add(0, 9, 0, R.string.menu_file_encoding_reload_with_new_encoding);
		MenuItemEncodingReload.setEnabled(false);
		encoding.add(0, 10, 0, R.string.menu_file_encoding_choose_save_encoding);
		sm = menu.addSubMenu(0, 0, 0, R.string.menu_edit).setIcon(UI.tintDrawable(this, R.mipmap.icon_edit, UI.AccentColor));
		sm.getItem().setShowAsActionFlags(2);
		sm.add(0, 6, 0, R.string.menu_edit_undo);
		sm.add(0, 7, 0, R.string.menu_edit_redo);
		sm.add(0, 8, 0, R.string.menu_edit_find_or_replace);
		menu.add(0, 5, 0, R.string.menu_settings).setIcon(UI.tintDrawable(this, R.mipmap.icon_settings, UI.AccentColor)).setShowAsActionFlags(2);
		return true;
	}

	private void showLoading(final String msg) {
		UI.onUI(new Runnable() {
			@Override
			public void run() {
				Loading.setMessage(msg);
				if (!Loading.isShowing()) Loading.show();
			}
		});
	}

	private void dismissLoading() {
		UI.onUI(new Runnable() {
			@Override
			public void run() {
				Loading.dismiss();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				ContentManager.addTab();
				break;
			case 2: {
				if (requestStoragePermissions()) break;
				setChooseFileListener(new ChooseFileListener() {
					@Override
					public void onChoose(final File f) {
						int ind = ContentManager.findFile(f);
						if (ind != -1) {
							ContentManager.setIndex(ind);
							return;
						}
						ContentManager.addTab(null, null, f);
						loadFile(f, null);
					}
				});
				ChooseFileActivity.chooseFile(this, REQUEST_CODE_CHOOSE_FILE);
				break;
			}
			case 3: {
				SaveTab(ContentManager.getCurrentEditData(), null);
				break;
			}
			case 4: {
				if (requestStoragePermissions()) break;
				setChooseFileListener(new ChooseFileListener() {
					@Override
					public void onChoose(File f) {
						MultiContentManager.EditData data = ContentManager.getCurrentEditData();
						ContentManager.closeExist(f);
						data.setFile(f);
						ContentManager.onEditDataUpdated(data.index);
						SaveTab(data, null);
					}
				});
				ChooseFileActivity.createFile(this, REQUEST_CODE_CHOOSE_FILE);
				break;
			}
			case 5: {
				startActivityForResult(new Intent(this, SettingActivity.class), REQUEST_CODE_SETTING);
				break;
			}
			case 6: {
				Content.undo();
				break;
			}
			case 7: {
				Content.redo();
				break;
			}
			case 8: {
				FRDialog.show();
				break;
			}
			case 9: {
				chooseEncoding(ContentManager.getCurrentEditData().charset, new ChooseCharsetListener() {
					@Override
					public void onChoose(Charset charset) {
						if (ObjectsCompat.equals(ContentManager.getCurrentEditData().charset, charset)) return;
						loadFile(ContentManager.getCurrentEditData().getFile(), charset);
					}
				});
				break;
			}
			case 10: {
				chooseEncoding(ContentManager.getCurrentEditData().charset, new ChooseCharsetListener() {
					@Override
					public void onChoose(Charset charset) {
						MultiContentManager.EditData data = ContentManager.getCurrentEditData();
						if (ObjectsCompat.equals(charset, data.charset)) return;
						data.charset = charset;
						data.saved = false;
						ContentManager.onEditDataUpdated(data.index);
					}
				});
			}
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void loadFile(final File f, final Charset def) {
		showLoading(getString(R.string.dialog_loading_file));
		new Thread() {
			@Override
			public void run() {
				try {
					byte[] data = IO.Read(new FileInputStream(f));
					final Charset charset = def == null ? IO.guessCharset(data) : def;
					final CharBuffer buf = charset.decode(ByteBuffer.wrap(data));
					UI.onUI(new Runnable() {
						@Override
						public void run() {
							MultiContentManager.EditData data = ContentManager.getCurrentEditData();
							data.charset = charset;
							data.setText(buf.array(), 0, buf.length());
							// 好很好！三重嵌套
							new Thread() {
								@Override
								public void run() {
									Content.parseAll();
									dismissLoading();
								}
							}.start();
						}
					});
				} catch (Throwable t) {
					dismissLoading();
					UI.postShowError(EditActivity.this, t);
				}
			}
		}.start();
	}

	private void chooseEncoding(Charset cur, final ChooseCharsetListener listener) {
		final Map<String, Charset> all = Charset.availableCharsets();
		Set<String> keySet = all.keySet();
		final String[] sorted = new String[keySet.size() + 1];
		int i = 0;
		for (String one : keySet) sorted[++i] = one;
		Arrays.sort(sorted, 1, sorted.length);
		sorted[0] = getString(R.string.dialog_encoding_auto);
		int ind = Arrays.binarySearch(sorted, cur.name());
		final MutableInteger chosen = new MutableInteger(ind);
		new AlertDialog.Builder(this).setSingleChoiceItems(sorted, ind, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				chosen.val = which;
			}
		}).setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.onChoose(chosen.val == 0 ? null : all.get(sorted[chosen.val]));
			}
		}).setNegativeButton(R.string.dialog_cancel, null).setCancelable(true).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (resultCode == RESULT_OK && data != null) {
			switch (requestCode) {
				case REQUEST_CODE_SETTING:
					if (data.getBooleanExtra(SettingActivity.CONFIG_CHANGED, false)) onSettingsChanged();
					break;
				case REQUEST_CODE_CHOOSE_FILE:
					String s = data.getStringExtra(ChooseFileActivity.TAG_RESULT);
					if (s == null) break;
					if (_ChooseFileListener != null) _ChooseFileListener.onChoose(new File(s));
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void onSettingsChanged() {
		Content.setTypeface(G._FONT);
		Content.setTextSize(TypedValue.COMPLEX_UNIT_SP, G._TEXT_SIZE);
		Content.setShowLineNumber(G._SHOW_LINE_NUMBER);
		Content.setWordWrappingEnabled(G._SPLIT_LINE);
		Content.setEventHandler(G._TWO_FINGER_SCALING ? new ZoomHelper() : null);
		ContentManager.setTheme(G._NIGHT_THEME ? MEditThemeDark.getInstance() : MEditThemeLight.getInstance());
		ContentManager.onEditDataUpdated(ContentManager.getIndex());
	}

	private synchronized void setChooseFileListener(ChooseFileListener listener) {
		this._ChooseFileListener = listener;
	}

	@Override
	public void onBackPressed() {
		boolean unsaved = false;
		for (int i = 0; i < ContentManager.getSize(); i++)
			if (!ContentManager.getEditData(i).saved) {
				unsaved = true;
				break;
			}
		if (unsaved) {
			new AlertDialog.Builder(this).setTitle(R.string.dialog_note).setMessage(R.string.dialog_sure_to_exit_with_file_unsaved).setPositiveButton(R.string.dialog_save_and_close_file, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new SaveAllRunnable().run();
				}
			}).setNeutralButton(R.string.dialog_exit, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).setCancelable(true).show();
		} else finish();
	}

	private interface ChooseCharsetListener {
		void onChoose(Charset charset);
	}

	private interface ChooseFileListener {
		void onChoose(File f);
	}

	private class SaveAllRunnable implements Runnable {
		private int ind;

		public SaveAllRunnable() {
			this.ind = 0;
		}

		public SaveAllRunnable(int ind) {
			this.ind = ind;
		}

		@Override
		public void run() {
			Runnable next = ind == ContentManager.getSize() - 1 ? null : new SaveAllRunnable(ind + 1);
			SaveTab(ContentManager.getCurrentEditData(), next);
		}
	}
}