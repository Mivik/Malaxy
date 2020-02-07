package com.mivik.malaxy;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.mivik.malaxy.ui.ChooseFileFragment;
import com.mivik.malaxy.ui.DividerDrawable;
import com.mivik.malaxy.ui.UI;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;

public class ChooseFileActivity extends BaseActivity implements ChooseFileFragment.ChooseFileListener, ChooseFileFragment.DirectoryChangeListener {
	public static final String TAG_RESULT = "result";

	private static final String TAG_CHOOSE_DIR = "choose_dir";
	private static final String TAG_CREATE_FILE = "create_file";

	private ChooseFileFragment F;
	private Intent Result;
	private boolean _CREATE;
	private boolean _DIRECTORY;
	private File ChosenFile;
	private AlertDialog InputDialog, OverrideDialog;
	private AppCompatEditText NameInput;
	private Toolbar Title;
	private LinearLayoutCompat Content;
	private DrawerLayout Drawer;
	private LinearLayoutCompat DrawerContent;
	private ActionBarDrawerToggle DrawerToggle;
	private LinearLayoutCompat BookmarkLayout;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Result = new Intent();
		setResult(RESULT_OK, Result);
		Intent intent = getIntent();
		Title = new Toolbar(this);
		ViewCompat.setElevation(Title, UI.dp2px(5));
		Title.setTitleMarginStart(R.string.app_name);
		Title.setBackgroundColor(UI.ThemeColor);
		Title.setTitleTextColor(UI.AccentColor);
		Title.setTitleMarginStart(0);
		setSupportActionBar(Title);
		try {
			Field field = Toolbar.class.getDeclaredField("mTitleTextView");
			field.setAccessible(true);
			AppCompatTextView view = (AppCompatTextView) field.get(Title);
			view.setEllipsize(TextUtils.TruncateAt.START);
		} catch (Throwable t) {
		}
		Content = new LinearLayoutCompat(this);
		Content.setOrientation(LinearLayoutCompat.VERTICAL);
		Content.addView(Title);

		if (!(_CREATE = intent.getBooleanExtra(TAG_CREATE_FILE, false)))
			_DIRECTORY = intent.getBooleanExtra(TAG_CHOOSE_DIR, false);
		if (_CREATE) {
			F = new ChooseFileFragment(this, G._HOME_DIR, this, true, new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return true;
				}
			});
			NameInput = new AppCompatEditText(this);
			NameInput.setHint("文件名");
			InputDialog = new AlertDialog.Builder(this).setTitle("文件名字").setView(NameInput).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = NameInput.getText().toString();
					if (name.length() == 0) {
						NameInput.setError("文件名不能为空");
						UI.preventDismiss(InputDialog);
						return;
					}
					File ret = null;
					try {
						ret = new File(ChosenFile, name);
					} catch (Throwable t) {
						NameInput.setError("含有非法字符");
						UI.preventDismiss(InputDialog);
						return;
					}
					if (ret.exists()) {
						NameInput.setError("文件已存在");
						UI.preventDismiss(InputDialog);
						return;
					}
					try {
						ret.createNewFile();
					} catch (Throwable t) {
						NameInput.setError("创建文件失败");
						UI.preventDismiss(InputDialog);
						return;
					}
					UI.forceDismiss(InputDialog);
					returnFile(ret);
				}
			}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					UI.forceDismiss(InputDialog);
				}
			}).setCancelable(true).create();
			OverrideDialog = new AlertDialog.Builder(this).setTitle("文件已存在").setMessage("你确定要覆盖此文件吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ChosenFile.delete();
					UI.forceDismiss(OverrideDialog);
					returnFile(ChosenFile);
				}
			}).setNegativeButton("取消", null).setCancelable(true).create();
		} else
			F = new ChooseFileFragment(this, G._HOME_DIR, this, _DIRECTORY);
		F.setShowPath(false);
		F.setShowGoBackButton(false);
		F.setDirectoryChangeListener(this);
		Drawer = new DrawerLayout(this);
		Drawer.addView(F.getView(), -1, -1);

		DrawerContent = new LinearLayoutCompat(this);
		DrawerContent.setOrientation(LinearLayoutCompat.VERTICAL);
		DrawerContent.addView(newItemView("设为首页", R.mipmap.icon_home, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				G.setHomeDir(F.getCurrentDirectory());
				Drawer.closeDrawers();
			}
		}));
		DrawerContent.addView(newItemView("回到首页", R.mipmap.icon_return, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				F.setCurrentDirectory(G._HOME_DIR);
				Drawer.closeDrawers();
			}
		}));
		DrawerContent.addView(newItemView("添加书签", R.mipmap.icon_bookmark, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!G._BOOKMARKS.contains(F.getCurrentDirectory())) G._BOOKMARKS.add(F.getCurrentDirectory());
				G.onBookmarksUpdate();
				refreshBookmarks();
				Drawer.closeDrawers();
			}
		}));
		ImageView divider = new ImageView(this);
		divider.setImageDrawable(new DividerDrawable(UI.ThemeColor));
		DrawerContent.addView(divider, -1, -2);
		BookmarkLayout = new LinearLayoutCompat(this);
		BookmarkLayout.setOrientation(LinearLayoutCompat.VERTICAL);
		BookmarkLayout.setDividerDrawable(new DividerDrawable(Color.GRAY));
		DrawerContent.addView(BookmarkLayout);
		DrawerContent.setBackgroundColor(UI.AccentColor);
		DrawerContent.setClickable(true);

		DrawerLayout.LayoutParams para = new DrawerLayout.LayoutParams(-1, -1);
		para.gravity = Gravity.START;
		Drawer.addView(DrawerContent, para);
		Content.addView(Drawer, -1, -1);
		setContentView(Content);

		DrawerToggle = new ActionBarDrawerToggle(this, Drawer, Title, R.string.drawer_open, R.string.drawer_close);
		DrawerToggle.syncState();
		Drawer.addDrawerListener(DrawerToggle);
		((DrawerArrowDrawable) Title.getNavigationIcon()).setColor(UI.AccentColor);

		refreshBookmarks();
	}

	private int RippleID = -1;

	public synchronized void refreshBookmarks() {
		if (RippleID == -1) {
			TypedValue value = new TypedValue();
			getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
			RippleID = value.resourceId;
		}
		BookmarkLayout.removeAllViews();
		final int size = G._BOOKMARKS.size();
		LinearLayoutCompat.LayoutParams para = new LinearLayoutCompat.LayoutParams(-1, -2);
		para.topMargin = UI.dp2px(2);
		for (int i = 0; i < size; i++) {
			final File f = G._BOOKMARKS.get(i);
			LinearLayout layout = new LinearLayout(this);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			int p = UI.dp2px(12);
			layout.setPadding(p, p, p * 2, p);
			TextView t = new TextView(this);
			t.setGravity(Gravity.START);
			t.setTextColor(Color.BLACK);
			t.setText(f.getName());
			t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
			layout.addView(t);
			t = new TextView(this);
			t.setGravity(Gravity.START);
			t.setTextColor(Color.GRAY);
			t.setText(f.getAbsolutePath());
			t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
			layout.addView(t, para);
			layout.setClickable(true);
			TypedArray arr = getTheme().obtainStyledAttributes(RippleID, new int[]{android.R.attr.selectableItemBackground});
			ViewCompat.setBackground(layout, arr.getDrawable(0));
			arr.recycle();
			layout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					F.setCurrentDirectory(f);
					Drawer.closeDrawers();
				}
			});
			layout.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					new AlertDialog.Builder(ChooseFileActivity.this).setTitle("提示").setMessage("你确定要删除此书签吗？").setPositiveButton("删除", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (G._BOOKMARKS.remove(f)) {
								G.onBookmarksUpdate();
								refreshBookmarks();
							}
						}
					}).setNeutralButton("取消", null).setCancelable(true).show();
					return true;
				}
			});
			BookmarkLayout.addView(layout);
		}
	}

	private static int _ICON_SIZE = -1;

	private View newItemView(String title, int id, View.OnClickListener listener) {
		if (_ICON_SIZE == -1)
			_ICON_SIZE = UI.dp2px(24);
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		int p = UI.dp2px(12);
		layout.setPadding(p, p, p * 2, p);
		ImageView icon = new ImageView(this);
		icon.setImageDrawable(UI.tintDrawable(ContextCompat.getDrawable(this, id), UI.ThemeColor));
		LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(_ICON_SIZE, _ICON_SIZE);
		para.rightMargin = UI.dp2px(10);
		layout.addView(icon, para);
		TextView t = new TextView(this);
		t.setGravity(Gravity.START);
		t.setTextColor(UI.ThemeColor);
		t.setText(title);
		t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
		layout.addView(t);
		layout.setClickable(true);
		TypedArray arr = getTheme().obtainStyledAttributes(RippleID, new int[]{android.R.attr.selectableItemBackground});
		ViewCompat.setBackground(layout, arr.getDrawable(0));
		arr.recycle();
		layout.setOnClickListener(listener);
		return layout;
	}

	@Override
	public void onChange(File f) {
		setTitle(f.getPath());
	}

	@Override
	public void onChoose(File f) {
		if (_CREATE) {
			ChosenFile = f;
			if (f.isDirectory()) {
				NameInput.setError(null);
				NameInput.getText().clear();
				InputDialog.show();
				return;
			}
			OverrideDialog.show();
		} else returnFile(f);
	}

	private void returnFile(File f) {
		Result.putExtra(TAG_RESULT, f.getAbsolutePath());
		finish();
	}

	public static void chooseFile(Activity cx, int requestCode) {
		Intent intent = new Intent(cx, ChooseFileActivity.class);
		intent.putExtra(TAG_CHOOSE_DIR, false);
		cx.startActivityForResult(intent, requestCode);
	}

	public static void chooseDirectory(Activity cx, int requestCode) {
		Intent intent = new Intent(cx, ChooseFileActivity.class);
		intent.putExtra(TAG_CHOOSE_DIR, true);
		cx.startActivityForResult(intent, requestCode);
	}

	public static void createFile(Activity cx, int requestCode) {
		Intent intent = new Intent(cx, ChooseFileActivity.class);
		intent.putExtra(TAG_CREATE_FILE, true);
		cx.startActivityForResult(intent, requestCode);
	}

	@Override
	public void onBackPressed() {
		if (!F.goBack()) finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final int flag = MenuItem.SHOW_AS_ACTION_ALWAYS;
		menu.add(0, 0, 0, "关闭").setIcon(R.mipmap.icon_close).setShowAsActionFlags(flag);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
}