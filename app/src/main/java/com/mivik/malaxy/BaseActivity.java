package com.mivik.malaxy;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public abstract class BaseActivity extends AppCompatActivity {
	protected static final int REQUEST_PERMISSION_CODE = 1926;
	private boolean REQUESTING_PERMISSION = false;
	private String[] PERMISSION_DESCRIPTIONS = null;
	private boolean FORCE_PERMISSION = false;

	protected synchronized void getPermissions(String[] permissions, String[] desc, boolean force) {
		if (REQUESTING_PERMISSION) return;
		REQUESTING_PERMISSION = true;
		PERMISSION_DESCRIPTIONS = desc;
		FORCE_PERMISSION = force;
		ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
	}

	public final boolean isAllPermissionsGranted(String[] permissions) {
		for (int i = 0; i < permissions.length; i++)
			if (ContextCompat.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED)
				return false;
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_PERMISSION_CODE) {
			REQUESTING_PERMISSION = false;
			for (int i = 0; i < grantResults.length; i++)
				if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("权限需求").setMessage(PERMISSION_DESCRIPTIONS[i]).setCancelable(true).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getPermissions(permissions, PERMISSION_DESCRIPTIONS, FORCE_PERMISSION);
						}
					});
					builder.setCancelable(!FORCE_PERMISSION);
					if (!FORCE_PERMISSION)
						builder.setNegativeButton("取消", null);
					break;
				}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	protected void enableBackButton() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
