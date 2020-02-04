package com.mivik.malaxy;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import androidx.core.view.ViewCompat;
import com.mivik.malax.WrappedEditable;
import com.mivik.malaxy.ui.FindReplaceDialog;
import com.mivik.malaxy.ui.MultiContentManager;
import com.mivik.malaxy.ui.UI;
import com.mivik.medit.MEdit;
import com.mivik.medit.theme.MEditThemeDark;
import com.mivik.medit.theme.MEditThemeLight;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class EditActivity extends BaseActivity implements WrappedEditable.EditActionListener, MultiContentManager.EditDataClickListener, Const {
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
		Content = ContentManager.getContent();
		Content.addEditActionListener(this);
		onSettingChanged();
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
		Content.setEventHandler(new ZoomHelper());
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
			tv.setText(SYMBOLS[i] == '\t' ? TAB : SYMBOLS[i] + "");
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

	private static final String[] _STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
	private static final String[] _STORAGE_DESC = {"我们需要存储权限", "我们需要读取权限"};

	private final boolean requestStoragePermissions() {
		if (isAllPermissionsGranted(_STORAGE_PERMISSIONS)) return false;
		getPermissions(_STORAGE_PERMISSIONS, _STORAGE_DESC, false);
		return true;
	}

	@Override
	public boolean beforeAction(WrappedEditable.EditAction editAction) {
		return false;
	}

	@Override
	public void afterAction(WrappedEditable.EditAction editAction) {
		MultiContentManager.EditData data = ContentManager.getCurrentEditData();
		if (data.saved) {
			data.saved = false;
			ContentManager.onEditDataUpdated(ContentManager.getIndex());
		}
	}

	@Override
	public void onEditDataClick(final MultiContentManager.EditData data) {
		if (data.index == ContentManager.getIndex()) {
			if (!data.saved) {
				new AlertDialog.Builder(this).setTitle("提示").setMessage("你还没有保存，确定要关闭吗？").setPositiveButton("保存并关闭", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SaveTab(data, new Runnable() {
							@Override
							public void run() {
								ContentManager.deleteTab(data.index);
							}
						});
					}
				}).setNeutralButton("关闭", new DialogInterface.OnClickListener() {
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
		if (data.getFile() == null) {
			setChooseFileListener(new ChooseFileListener() {
				@Override
				public void onChoose(File f) {
					data.setFile(f);
					SaveTab(data, action);
					ContentManager.onEditDataUpdated(data.index);
				}
			});
			ChooseFileActivity.createFile(this, REQUEST_CODE_CHOOSE_FILE);
			return;
		}
		try {
			FileOutputStream out = new FileOutputStream(data.getFile());
			out.write(Content.getText().getBytes());
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
		View v = new View(this);
		v.setBackgroundColor(Color.RED);
		SubMenu sm = menu.addSubMenu(0, 0, 0, "文件").setIcon(UI.tintDrawable(this, R.mipmap.icon_directory, UI.AccentColor));
		sm.getItem().setShowAsActionFlags(2);
		sm.add(0, 1, 0, "新建");
		sm.add(0, 2, 0, "打开");
		sm.add(0, 3, 0, "保存");
		sm.add(0, 4, 0, "另存为");
		sm = menu.addSubMenu(0, 0, 0, "编辑").setIcon(UI.tintDrawable(this, R.mipmap.icon_edit, UI.AccentColor));
		sm.getItem().setShowAsActionFlags(2);
		sm.add(0, 6, 0, "撤销");
		sm.add(0, 7, 0, "重做");
		sm.add(0, 8, 0, "查找/替换");
		menu.add(0, 5, 0, R.string.title_settings).setIcon(UI.tintDrawable(this, R.mipmap.icon_settings, UI.AccentColor)).setShowAsActionFlags(2);
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
						showLoading("加载文件中...");
						new Thread() {
							@Override
							public void run() {
								try {
									final char[] s = new String(IO.Read(new FileInputStream(f))).toCharArray();
									UI.onUI(new Runnable() {
										@Override
										public void run() {
											ContentManager.addTab(s, f);
											// 好很好！三重嵌套nmsl
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
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private ChooseFileListener _ChooseFileListener;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case REQUEST_CODE_SETTING:
					if (data.getBooleanExtra(SettingActivity.CONFIG_CHANGED, false)) onSettingChanged();
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

	private void onSettingChanged() {
		Content.setTypeface(G._FONT);
		Content.setTextSize(TypedValue.COMPLEX_UNIT_SP, G._TEXT_SIZE);
		Content.setShowLineNumber(G._SHOW_LINE_NUMBER);
		ContentManager.setTheme(G._NIGHT_THEME ? MEditThemeDark.getInstance() : MEditThemeLight.getInstance());
		ContentManager.onEditDataUpdated(ContentManager.getIndex());
	}

	private synchronized void setChooseFileListener(ChooseFileListener listener) {
		this._ChooseFileListener = listener;
	}

	@Override
	public void onBackPressed() {
		if (!ContentManager.getCurrentEditData().saved) {
			new AlertDialog.Builder(this).setTitle("提示").setMessage("你还没有保存，确定要退出吗？").setPositiveButton("保存并退出", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SaveTab(ContentManager.getCurrentEditData(), new Runnable() {
						@Override
						public void run() {
							finish();
						}
					});
				}
			}).setNeutralButton("退出", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).setCancelable(true).show();
		} else finish();
	}

	private interface ChooseFileListener {
		void onChoose(File f);
	}
}