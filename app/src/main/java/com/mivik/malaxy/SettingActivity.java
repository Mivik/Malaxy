package com.mivik.malaxy;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import com.mivik.malaxy.ui.SettingFragment;
import com.mivik.malaxy.ui.UI;
import com.mivik.malaxy.util.IntReference;

public class SettingActivity extends BaseActivity {
	public static final int REQUEST_CODE_CHOOSE_FONT = 233;
	public static final String CONFIG_CHANGED = "ConfigChanged";

	private SettingFragment Q;
	private Intent ResultIntent;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ResultIntent = new Intent();
		setResult(RESULT_OK, ResultIntent);
		enableBackButton();
		Q = new SettingFragment(this);
		Q.addGroup("编辑器");
		Q.addSimpleItem("设置字体", "设定编辑器使用的字体").setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(SettingActivity.this).setTitle("选择字体").setItems(new String[]{"系统字体", "预设字体", "自定义字体"}, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: {
								new AlertDialog.Builder(SettingActivity.this).setTitle("系统字体").setItems(Const.SYSTEM_FONTS, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										G.setFont(SettingActivity.this, "@" + which);
										onConfigChanged();
									}
								}).setNegativeButton("取消", null).setCancelable(true).show();
								break;
							}
							case 1: {
								new AlertDialog.Builder(SettingActivity.this).setTitle("预设字体").setItems(Const.PRESET_FONTS, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										G.setFont(SettingActivity.this, "#" + Const.PRESET_FONTS[which]);
										onConfigChanged();
									}
								}).setNegativeButton("取消", null).setCancelable(true).show();
								break;
							}
							case 2: {
								ChooseFileActivity.chooseFile(SettingActivity.this, REQUEST_CODE_CHOOSE_FONT);
								break;
							}
						}
					}
				}).setNegativeButton("取消", null).setCancelable(true).show();
			}
		});
		Q.addSimpleItem("设置字体大小", "设定编辑器的字体大小").setOnClickListener(new View.OnClickListener() {
			private AlertDialog Dialog;

			@Override
			public void onClick(View v) {
				final AppCompatEditText edit = new AppCompatEditText(SettingActivity.this);
				edit.setHint("字体大小");
				edit.setText(Float.toString(G._TEXT_SIZE));
				edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				edit.setSelection(edit.length());
				Dialog = new AlertDialog.Builder(SettingActivity.this).setTitle("设置字体大小").setView(edit).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						UI.preventDismiss(Dialog);
						String str = edit.getText().toString();
						if (str.length() == 0) {
							edit.setError("不能为空");
							return;
						}
						float size;
						try {
							size = Float.parseFloat(str);
						} catch (Throwable t) {
							edit.setError("含有非法字符");
							return;
						}
						UI.forceDismiss(Dialog);
						G.setTextSize(size);
						onConfigChanged();
					}
				}).setCancelable(true).setNegativeButton("取消", null).create();
				Dialog.show();
			}
		});
		{
			final IntReference ret = new IntReference(G._LEXER_ID);
			Q.addSimpleItem("切换高亮", "切换编辑器所使用的高亮方式").setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(SettingActivity.this).setTitle("切换高亮").setSingleChoiceItems(G.LEXER_NAMES, G._LEXER_ID, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ret.val = which;
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (ret.val == G._LEXER_ID) return;
							G.setLexerId(ret.val);
							onConfigChanged();
						}
					}).setNegativeButton("取消", null).setCancelable(true).show();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem("自动换行", "设置编辑器是否将一行自动分割为多行");
			item.setChecked(G._SPLIT_LINE);
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (item.isChecked()==G._SPLIT_LINE) return;
					G.setSplitLine(item.isChecked());
					onConfigChanged();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem("显示行号", "设定编辑器是否显示行号");
			item.setChecked(G._SHOW_LINE_NUMBER);
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (item.isChecked() == G._SHOW_LINE_NUMBER) return;
					G.setShowLineNumber(item.isChecked());
					onConfigChanged();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem("夜间主题", "设定编辑器是否使用夜间主题");
			item.setChecked(G._NIGHT_THEME);
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (item.isChecked() == G._NIGHT_THEME) return;
					G.setNightTheme(item.isChecked());
					onConfigChanged();
				}
			});
		}
		setContentView(Q.getView());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQUEST_CODE_CHOOSE_FONT && resultCode == RESULT_OK) {
			String s = data.getStringExtra(ChooseFileActivity.TAG_RESULT);
			if (s != null) {
				G.setFont(this, s);
				UI.toast(this, "已设置字体");
				onConfigChanged();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void onConfigChanged() {
		ResultIntent.putExtra(CONFIG_CHANGED, true);
	}
}