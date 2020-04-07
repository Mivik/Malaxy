package com.mivik.malaxy;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import com.mivik.malaxy.ui.SettingFragment;
import com.mivik.malaxy.ui.UI;
import com.mivik.malaxy.util.MutableInteger;

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
		Q.addGroup(getString(R.string.settings_editor));
		Q.addSimpleItem(getString(R.string.settings_editor_font), getString(R.string.settings_editor_font_description)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String[] types = getResources().getStringArray(R.array.settings_editor_font_types);
				new AlertDialog.Builder(SettingActivity.this).setTitle(R.string.settings_editor_choose_font).setItems(types, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: {
								new AlertDialog.Builder(SettingActivity.this).setTitle(types[0]).setItems(Const.SYSTEM_FONTS, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										G.setFont(SettingActivity.this, "@" + which);
										onConfigChanged();
									}
								}).setNegativeButton(R.string.dialog_cancel, null).setCancelable(true).show();
								break;
							}
							case 1: {
								new AlertDialog.Builder(SettingActivity.this).setTitle(types[1]).setItems(Const.PRESET_FONTS, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										G.setFont(SettingActivity.this, "#" + Const.PRESET_FONTS[which]);
										onConfigChanged();
									}
								}).setNegativeButton(R.string.dialog_cancel, null).setCancelable(true).show();
								break;
							}
							case 2: {
								ChooseFileActivity.chooseFile(SettingActivity.this, REQUEST_CODE_CHOOSE_FONT);
								break;
							}
						}
					}
				}).setNegativeButton(R.string.dialog_cancel, null).setCancelable(true).show();
			}
		});
		Q.addSimpleItem(getString(R.string.settings_editor_font_size), getString(R.string.settings_editor_font_size_description)).setOnClickListener(new View.OnClickListener() {
			private AlertDialog Dialog;

			@SuppressLint("SetTextI18n")
			@Override
			public void onClick(View v) {
				final AppCompatEditText edit = new AppCompatEditText(SettingActivity.this);
				edit.setHint(R.string.settings_editor_font_size_size);
				edit.setText(Float.toString(G._TEXT_SIZE));
				edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				edit.setSelection(edit.length());
				Dialog = new AlertDialog.Builder(SettingActivity.this).setTitle(R.string.settings_editor_font_size).setView(edit).setPositiveButton(getString(R.string.dialog_confirm), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						UI.preventDismiss(Dialog);
						Editable editable = edit.getText();
						String str = editable == null ? null : editable.toString();
						if (str == null || str.length() == 0) {
							edit.setError(getString(R.string.dialog_text_cannot_be_empty));
							return;
						}
						float size;
						try {
							size = Float.parseFloat(str);
						} catch (Throwable t) {
							edit.setError(getString(R.string.dialog_text_contains_illegal_characters));
							return;
						}
						UI.forceDismiss(Dialog);
						G.setTextSize(size);
						onConfigChanged();
					}
				}).setCancelable(true).setNegativeButton(R.string.dialog_cancel, null).create();
				Dialog.show();
			}
		});
		{
			final MutableInteger ret = new MutableInteger(G._LEXER_ID);
			Q.addSimpleItem(getString(R.string.settings_editor_highlight), getString(R.string.settings_editor_highlight_description)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(SettingActivity.this).setTitle(R.string.settings_editor_highlight).setSingleChoiceItems(G.LEXER_NAMES, G._LEXER_ID, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ret.val = which;
						}
					}).setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (ret.val == G._LEXER_ID) return;
							G.setLexerId(ret.val);
							onConfigChanged();
						}
					}).setNegativeButton(R.string.dialog_cancel, null).setCancelable(true).show();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem(getString(R.string.settings_editor_two_finger_scaling), getString(R.string.settings_editor_two_finger_scaling_description));
			item.setChecked(G._TWO_FINGER_SCALING);
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (item.isChecked() == G._TWO_FINGER_SCALING) return;
					G.setTwoFingerScaling(item.isChecked());
					onConfigChanged();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem(getString(R.string.settings_editor_word_wrapping), getString(R.string.settings_editor_word_wrapping_description));
			item.setChecked(G._SPLIT_LINE);
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (item.isChecked() == G._SPLIT_LINE) return;
					G.setSplitLine(item.isChecked());
					onConfigChanged();
				}
			});
		}
		{
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem(getString(R.string.settings_editor_show_linenumber), getString(R.string.settings_editor_show_linenumber_description));
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
			final SettingFragment.CheckBoxItem item = Q.addCheckBoxItem(getString(R.string.settings_editor_dark_mode), getString(R.string.settings_editor_dark_mode_description));
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
				UI.toast(this, getString(R.string.settings_editor_font_set));
				onConfigChanged();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void onConfigChanged() {
		ResultIntent.putExtra(CONFIG_CHANGED, true);
	}
}