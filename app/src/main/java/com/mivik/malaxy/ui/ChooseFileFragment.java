package com.mivik.malaxy.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

public class ChooseFileFragment extends MFragment implements AdapterView.OnItemClickListener, FileFilter {
	private static Bitmap FolderBitmap, FileBitmap;
	public static BitmapDrawable FolderDrawable, FileDrawable;
	public static int IconSize, RightMargin;
	private SwipeRefreshLayout Refresh;

	public static void checkBitmap() {
		if (FolderBitmap != null) return;
		byte[] data = android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAABHNCSVQICAgIfAhkiAAAAo1JREFU\neJzt2rFy2zAURNEr1dE/4c/5N1KvuE8KJ4ooAoxF2wMKe8/MK2w3pBdaY94YJEmSJEmSJEmSJEmS\n5grw68WmfMPvIdZE/0CfnelbfhOBCv3DtAU6esVPvy3wRQr9Q7QFOnrlT78t8EmF/uHZAh2N8Om3\nBTYq9A/NWZ/SyO5LTDt4QWd9pmZ6n1R28HLOx6ZUE/ykaQcv5nxspkaGm5UdvJTz3JRKjptNO3gh\n57mZqkluUHbwMs62KYs0N5h28CLOtpkqeUqSJEmSpFAH4Aqcej+IurgegUvvp1A3lyNw7v0U6uZs\nA2SzAcLZAOFsgHA2QDgbINz5APwAfvZ+EnVxOgJveAASXYG3458vvAfkuQD8PQDeA/Kc4d8BsAHy\n2ADhbIBwNkA4GyCcDRDuDO//EgZuAxOduFsEuQ3McuU989ufAPAekOSW9f0B8B6Q45a1DZDJBghn\nA4SzAcLZAOFsgHC3rA9333QbmONEZRHkNjDDbQsI8wMA3gMSzDJ+PADeA8Y3y9gGyGMDhLMBwtkA\n4WyAcDZAuFnGh4cfug0c320LCMsGcBs4ttkWEJYHALwHjGyRbe0AeA8Y1yJbGyCLDRDOBghnA4Sz\nAcLZAOEW2T5uAsFt4MhmW0CoN4DbwDEttoBQPwDgPWBE1UxbB8B7wHiqmdoAOWyAcDZAOBsgnA0Q\nzgYIV820tgkEt4EjWmwBod0AbgPHUt0CQvsAgPeAkTSzXDsA3gPG0czSBshgA4SzAcLZAOFsgHA2\nQLhmlq1NILgNHEl1CwjrDeA2cAzNLSCsHwDwHjCC1Qz/dwC8B7y+1QxtgPHZAOFsgHA2QDgbIJwN\nEG41w7VNILgNHEFzCyhJkiRJkrL8Bo92etvl8h4FAAAAAElFTkSuQmCC\n", android.util.Base64.DEFAULT);
		FolderBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		data = android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAABHNCSVQICAgIfAhkiAAAAwJJREFU\neJzt3UFOVEEURuGjMTAy7kPBjbgxRxqI4siNiLoJxbHRHQAjA8GBmhAiXdD14Fb1f76kpkVb9+R1\n7Bo8kCRJkiRJqrML7AFHwClwUbxaXvb9c/XPNnAAnFM/9NsEcIERdNsGPlM/7HUDMIJO76gfdG8A\nRrCmXcZ77K8bgBGsYY/6IS8ZgBHc0hH1Q146ACO4hRPqh3wXARjBDVUP+C4DMIIbqB7wXQdgBA3V\nA76PAIxgheoB31cARnCN6gHfZwBG8B/VA77vAIzgiuoBVwRgBJdUD7gqgI2N4Op9fkv1gCsD2KgI\nrrvPb6kecHUAGxHBqvv8luoBjxDA9BGsus9vqR7wKAFMG0HrPr+lesAjBTBlBK37/JbqAY8WwHQR\ntO7zW6oHPGIAU0XQus9vqR7wqAFME8HIBzh7AFNEMPoBzh7A8BHMcICzBzB0BLMc4OwBDBvBTAc4\newBDRjDbAc4ewHARzHiAswcwVASzHuDsASwSwaPeDcSD6g/Q42H1B1AtAwhnAOEMIJwBhDOAcAYQ\nzt8B2m7yY1Clrt8hfAKEM4BwBhDOAMIZQDgDCGcA4QwgnAGEM4BwBhDOAMIZQDgDCGcA4QwgnAGE\nM4BwBhDOAMIZQDgDCGcA4QwgnAGEM4BwBhDOAMIZQDgDCGcA4QwgnAGEM4BwBhDOAMIZQDgDCGcA\n4QwgnAGEM4BwBhDOAMIZQDgDCGcA4Qxgbse9GxjA3H70bmAAc/vQu8ESrz1tvVKl9TdGfyXLqM6B\n58C3nk18AszrgM7hg0+AWX0EXgC/ejfyCTCXc2CfhYYPvjZuBqfAd+AQeA8cLbn5CAEs8TWkNfkV\nEM4AwhlAOAMIZwDhDCCcAYRb4v/gx8DjBfZJdAw8qfwASzwBfi6wR6ru+/xeSwRwuMAeqbrv80ew\nA5zx51bPdfN1Bjxd47yH9Jb6A51t7a910oPaAj5Rf6izrMO/Z7ZRtoA3+HWwap0Be2zg8C/bAV4D\nX4ET6g+9ep0AX4BXwLOOc5UkSZIkSeryG7ei65jwyWVaAAAAAElFTkSuQmCC\n", android.util.Base64.DEFAULT);
		FileBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		FolderDrawable = new BitmapDrawable(FolderBitmap);
		UI.tintDrawable(FolderDrawable, UI.ThemeColor);
		FileDrawable = new BitmapDrawable(FileBitmap);
		UI.tintDrawable(FileDrawable, UI.ThemeColor);
		IconSize = UI.dp2px(24);
		RightMargin = UI.dp2px(8);
		System.gc();
	}

	public void setLastText(CharSequence cs) {
		goBack.setText(cs);
	}

	public void setChooseDirText(CharSequence cs) {
		ok.setText(cs);
	}

	public CharSequence getLastText() {
		return goBack.getText();
	}

	public CharSequence getChooseDirText() {
		return ok.getText();
	}

	private ListView list;
	private AppCompatButton ok, goBack;
	private File dir;
	private LinearLayoutCompat layout, buttonLayout;
	private boolean chooseDir;
	private FileFilter filter;
	private ChooseFileListener listener;
	private DirectoryChangeListener dirChangeListener;

	public void setDirectoryChangeListener(DirectoryChangeListener listener) {
		dirChangeListener = listener;
		listener.onChange(dir);
	}

	public void setShowPath(boolean flag) {
		path.setVisibility(flag ? View.VISIBLE : View.GONE);
	}

	public boolean isShowingPath() {
		return path.getVisibility() == View.VISIBLE;
	}

	public DirectoryChangeListener getDirectoryChangeListener() {
		return dirChangeListener;
	}

	@Override
	public boolean accept(File pathname) {
		return ((!chooseDir) && pathname.isFile()) || pathname.isDirectory();
	}

	public ChooseFileFragment(Context cx, File f, ChooseFileListener listener) {
		this(cx, f, listener, false);
	}

	public ChooseFileFragment(Context cx, File f, ChooseFileListener listener, boolean chooseDir) {
		this(cx, f, listener, chooseDir, null);
	}

	public ChooseFileFragment(Context cx, File f, ChooseFileListener lis, boolean chooseDir, FileFilter filter) {
		super(cx);
		checkBitmap();
		this.chooseDir = chooseDir;
		this.dir = f;
		this.filter = filter == null ? this : filter;
		this.listener = lis;
		this.Refresh = new SwipeRefreshLayout(cx);
		Refresh.setColorSchemeColors(UI.ThemeColor);
		Refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				setCurrentDirectory(getCurrentDirectory());
			}
		});
		list = new ListView(getContext());
		layout = new LinearLayoutCompat(getContext());
		layout.setOrientation(LinearLayoutCompat.VERTICAL);
		path = new TextView(getContext());
		path.setGravity(Gravity.CENTER);
		layout.addView(path, -1, -2);
		LinearLayoutCompat.LayoutParams p = getDivideParams(true);
		Refresh.addView(list, -1, -1);
		layout.addView(Refresh, p);
		buttonLayout = new LinearLayoutCompat(getContext());
		buttonLayout.setOrientation(LinearLayoutCompat.HORIZONTAL);
		layout.addView(buttonLayout, -1, -2);
		goBack = new AppCompatButton(cx);
		goBack.setText("上一层");
		goBack.setBackgroundDrawable(null);
		goBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goBack();
			}
		});
		buttonLayout.addView(goBack, getDivideParams(false));
		if (chooseDir) {
			ok = new AppCompatButton(cx);
			ok.setText("选择文件夹");
			ok.setBackgroundDrawable(null);
			ok.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null)
						listener.onChoose(dir);
					onDettach();
				}
			});
			buttonLayout.addView(ok, getDivideParams(false));
		}
		adapter = new FileAdapter(getContext(), ds);
		list.setAdapter(adapter);
		if (dirChangeListener != null) dirChangeListener.onChange(dir);
		update();
		list.setOnItemClickListener(this);
		Refresh.post(new Runnable() {
			@Override
			public void run() {
				Refresh.setRefreshing(false);
				path.setText(dir.getPath());
			}
		});
		list.setDivider(new DividerDrawable(UI.ThemeColor));
	}

	public boolean goBack() {
		if (dir.getParentFile() == null) return false;
		dir = dir.getParentFile();
		if (dirChangeListener != null) dirChangeListener.onChange(dir);
		update();
		return true;
	}

	public void setChooseDir(boolean chooseDir) {
		this.chooseDir = chooseDir;
	}

	public boolean isChooseDir() {
		return chooseDir;
	}

	public File getCurrentDirectory() {
		return dir;
	}

	public void setCurrentDirectory(File f) {
		dir = f;
		if (dirChangeListener != null) dirChangeListener.onChange(f);
		update();
	}

	public void setShowGoBackButton(boolean flag) {
		goBack.setVisibility(flag ? View.VISIBLE : View.GONE);
	}

	public boolean isShowingGoBackButton() {
		return goBack.getVisibility() == View.VISIBLE;
	}

	private void update() {
		if (list == null) return;
		Refresh.setRefreshing(true);
		ds.clear();
		new Thread() {
			@Override
			public void run() {
				File[] fs = dir.listFiles(filter);
				if (fs == null) fs = new File[]{};
				int dq = 0;
				for (File one : fs) if (one.isDirectory()) dq++;
				String[] tmpp = new String[dq];
				String[] tmp = new String[fs.length - dq];
				int p1 = 0, p2 = 0;
				for (int i = 0; i < fs.length; i++)
					if (fs[i].isDirectory()) tmpp[p1++] = fs[i].getName();
					else tmp[p2++] = fs[i].getName();
				Arrays.sort(tmpp);
				Arrays.sort(tmp);
				for (int i = 0; i < tmpp.length; i++) fs[i] = new File(dir, tmpp[i]);
				for (int i = 0; i < tmp.length; i++) fs[dq + i] = new File(dir, tmp[i]);
				final File[] ffs = fs;
				list.post(new Runnable() {
					@Override
					public void run() {
						ds.clear();
						for (File one : ffs) ds.add(one);
						path.setText(dir.getPath());
						adapter.notifyDataSetChanged();
						Refresh.setRefreshing(false);
					}
				});
			}
		}.start();
	}

	FileAdapter adapter;

	@Override
	public View getView() {
		return layout;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		File f = ds.get(pos);
		if (f.isDirectory()) {
			dir = f;
			if (dirChangeListener != null) dirChangeListener.onChange(dir);
			update();
		} else {
			if (listener != null)
				listener.onChoose(f);
			onDettach();
		}
	}

	TextView path;

	static LinearLayoutCompat.LayoutParams getDivideParams(boolean vertical) {
		LinearLayoutCompat.LayoutParams p = new LinearLayoutCompat.LayoutParams(-1, -1);
		if (vertical) p.height = 0;
		else p.width = 0;
		p.weight = 1f;
		return p;
	}

	ArrayList<File> ds = new ArrayList<>();

	static class FileAdapter extends ArrayAdapter<File> {
		static int pa = -1;

		public FileAdapter(Context cx, ArrayList<File> data) {
			super(cx, android.R.layout.simple_list_item_1, data);
			if (pa == -1)
				pa = UI.dp2px(15);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			File f = getItem(position);
			TextView t = new TextView(getContext());
			t.setText(f.getName());
			t.setGravity(Gravity.CENTER);
			t.setTextColor(Color.BLACK);
			ImageView iv = new ImageView(getContext());
			iv.setImageDrawable(f.isDirectory() ? FolderDrawable : FileDrawable);
			LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(IconSize, IconSize);
			para.rightMargin = RightMargin;
			iv.setLayoutParams(para);
			LinearLayout layout = new LinearLayout(getContext());
			layout.setPadding(pa, pa, pa, pa);
			layout.addView(iv);
			layout.addView(t);
			return layout;
		}
	}

	@Override
	public Object getTag() {
		return "VFileChooser";
	}

	public interface ChooseFileListener {
		void onChoose(File f);
	}

	public interface DirectoryChangeListener {
		void onChange(File f);
	}
}
