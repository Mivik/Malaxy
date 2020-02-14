package com.mivik.malaxy;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.mivik.medit.Malaxy;

import java.io.PrintStream;
import java.util.Scanner;

public class MalaxyActivity extends BaseActivity {
	private Malaxy malaxy;
	private PrintStream out;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		malaxy = new Malaxy(this);
		setContentView(malaxy);
		new Thread() {
			@Override
			public void run() {
				main();
			}
		}.start();
	}

	public static void main() {
		Scanner scan = new Scanner(System.in);
		while (true) {
			String name = scan.nextLine();
			System.out.println("hello, " + name + "!");
		}
	}
}