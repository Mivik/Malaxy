package com.mivik.malaxy;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.view.View;
import com.mivik.medit.G;
import com.mivik.medit.ui.SettingFragment;
import com.mivik.medit.ui.UI;
import com.mivik.medit.util.IntReference;

public class SettingActivity extends BaseActivity {
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
		Q.addSimpleItem("设置字体大小", "设定编辑器的字体大小").setOnClickListener(new View.OnClickListener() {
			private AlertDialog Dialog;

			@Override
			public void onClick(View v) {
				final AppCompatEditText edit = new AppCompatEditText(SettingActivity.this);
				edit.setHint("字体大小");
				edit.setText(Integer.toString(G._TEXT_SIZE));
				edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				edit.setSelection(edit.getText().length());
				Dialog = new AlertDialog.Builder(SettingActivity.this).setTitle("设置字体大小").setView(edit).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						UI.preventDismiss(Dialog);
						String str = edit.getText().toString();
						if (str.length() == 0) {
							edit.setError("不能为空");
							return;
						}
						int size;
						try {
							size = Integer.parseInt(str);
						} catch (Throwable t) {
							edit.setError("含有非法字符");
							return;
						}
						UI.forceDismiss(Dialog);
						if (size == G._TEXT_SIZE) return;
						G.setTextSize(size);
						onConfigChanged();
					}
				}).setCancelable(true).setNegativeButton("取消", null).create();
				Dialog.show();
			}
		});
		final IntReference __CHOSE_ITEM = new IntReference(G._LEXER_ID);
		Q.addSimpleItem("切换高亮", "切换编辑器所使用的高亮方式").setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(SettingActivity.this).setTitle("切换高亮").setSingleChoiceItems(G.LEXER_NAMES, G._LEXER_ID, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						__CHOSE_ITEM.val = which;
					}
				}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (__CHOSE_ITEM.val == G._LEXER_ID) return;
						G.setLexerId(__CHOSE_ITEM.val);
						onConfigChanged();
					}
				}).setNegativeButton("取消", null).setCancelable(true).show();
			}
		});
		final SettingFragment.CheckBoxItem __SHOW_LINE_NUMBER = Q.addCheckBoxItem("显示行号", "设定编辑器是否显示行号");
		__SHOW_LINE_NUMBER.setChecked(G._SHOW_LINE_NUMBER);
		__SHOW_LINE_NUMBER.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (__SHOW_LINE_NUMBER.isChecked() == G._SHOW_LINE_NUMBER) return;
				G.setShowLineNumber(__SHOW_LINE_NUMBER.isChecked());
				onConfigChanged();
			}
		});
		final SettingFragment.CheckBoxItem __NIGHT_THEME = Q.addCheckBoxItem("夜间主题", "设定编辑器是否使用夜间主题");
		__NIGHT_THEME.setChecked(G._NIGHT_THEME);
		__NIGHT_THEME.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (__NIGHT_THEME.isChecked() == G._NIGHT_THEME) return;
				G.setNightTheme(__NIGHT_THEME.isChecked());
				onConfigChanged();
			}
		});
		setContentView(Q.getView());
	}

	private void onConfigChanged() {
		ResultIntent.putExtra(CONFIG_CHANGED, true);
	}
}
