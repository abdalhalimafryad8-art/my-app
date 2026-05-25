package com.magnum.soundwave;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivityV3 extends MainActivityV2 {
	
	private Song pendingSongForRename;
	private String pendingNewName;
	
	@Override
	protected void showSongOptionsDialog(Song song) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		container.setLayoutParams(new ViewGroup.LayoutParams(
		(int) (300 * getResources().getDisplayMetrics().density),
		ViewGroup.LayoutParams.WRAP_CONTENT
		));
		
		TextView titleView = new TextView(this);
		titleView.setText(song.title);
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setSingleLine(true);
		container.addView(titleView);
		
		View divider = new View(this);
		LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
		divParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
		divider.setLayoutParams(divParams);
		divider.setBackgroundColor(0x33FFFFFF);
		container.addView(divider);
		
		String[] options = {"Play Track", "Rename File", "Delete File"};
		int[] colors = {0xFF00E676, 0xFF29B6F6, 0xFFFF5252};
		
		for (int i = 0; i < options.length; i++) {
			TextView optionView = new TextView(this);
			optionView.setText(options[i]);
			optionView.setTextColor(colors[i]);
			optionView.setTextSize(16);
			optionView.setTypeface(Typeface.DEFAULT_BOLD);
			optionView.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, (int) (12 * getResources().getDisplayMetrics().density));
			optionView.setGravity(Gravity.CENTER);
			
			final int index = i;
			optionView.setOnClickListener(v -> {
				dialog.dismiss();
				if (index == 0) playSong(song);
				else if (index == 1) showRenameDialog(song);
				else if (index == 2) showDeleteConfirmDialog(song);
			});
			container.addView(optionView);
		}
		
		dialog.setContentView(container);
		dialog.show();
	}
	
	@Override
	protected void showRenameDialog(Song song) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		
		TextView titleView = new TextView(this);
		titleView.setText("Rename Track");
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		container.addView(titleView);
		
		EditText input = new EditText(this);
		input.setText(song.title);
		input.setTextColor(Color.WHITE);
		input.setHintTextColor(0x88FFFFFF);
		input.setSingleLine(true);
		LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		inputParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (24 * getResources().getDisplayMetrics().density));
		input.setLayoutParams(inputParams);
		container.addView(input);
		
		LinearLayout btnContainer = new LinearLayout(this);
		btnContainer.setOrientation(LinearLayout.HORIZONTAL);
		btnContainer.setWeightSum(2);
		
		TextView btnCancel = new TextView(this);
		btnCancel.setText("CANCEL");
		btnCancel.setTextColor(0xFF8A94A6);
		btnCancel.setTextSize(14);
		btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
		btnCancel.setGravity(Gravity.CENTER);
		btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		btnCancel.setOnClickListener(v -> dialog.dismiss());
		
		TextView btnSave = new TextView(this);
		btnSave.setText("SAVE");
		btnSave.setTextColor(0xFF00E676);
		btnSave.setTextSize(14);
		btnSave.setTypeface(Typeface.DEFAULT_BOLD);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		btnSave.setOnClickListener(v -> {
			String newName = input.getText().toString().trim();
			if (!newName.isEmpty()) {
				dialog.dismiss();
				renameMediaFile(song, newName);
			}
		});
		
		btnContainer.addView(btnCancel);
		btnContainer.addView(btnSave);
		container.addView(btnContainer);
		
		dialog.setContentView(container);
		dialog.show();
	}
	
	@Override
	protected void showDeleteConfirmDialog(Song song) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		
		TextView titleView = new TextView(this);
		titleView.setText("Delete Track");
		titleView.setTextColor(0xFFFF5252);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		container.addView(titleView);
		
		TextView msgView = new TextView(this);
		msgView.setText("Are you sure you want to permanently delete this audio file?");
		msgView.setTextColor(0xFFE2E8F0);
		msgView.setTextSize(14);
		msgView.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		msgParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (24 * getResources().getDisplayMetrics().density));
		msgView.setLayoutParams(msgParams);
		container.addView(msgView);
		
		LinearLayout btnContainer = new LinearLayout(this);
		btnContainer.setOrientation(LinearLayout.HORIZONTAL);
		btnContainer.setWeightSum(2);
		
		TextView btnCancel = new TextView(this);
		btnCancel.setText("CANCEL");
		btnCancel.setTextColor(0xFF8A94A6);
		btnCancel.setTextSize(14);
		btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
		btnCancel.setGravity(Gravity.CENTER);
		btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		btnCancel.setOnClickListener(v -> dialog.dismiss());
		
		TextView btnDelete = new TextView(this);
		btnDelete.setText("DELETE");
		btnDelete.setTextColor(0xFFFF5252);
		btnDelete.setTextSize(14);
		btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
		btnDelete.setGravity(Gravity.CENTER);
		btnDelete.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		btnDelete.setOnClickListener(v -> {
			dialog.dismiss();
			deleteMediaFile(song);
		});
		
		btnContainer.addView(btnCancel);
		btnContainer.addView(btnDelete);
		container.addView(btnContainer);
		
		dialog.setContentView(container);
		dialog.show();
	}
	
	@Override
	protected void deleteMediaFile(Song song) {
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id);
		try {
			int deleted = getContentResolver().delete(uri, null, null);
			if (deleted > 0) {
				loadMusic();
				Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
				} else {
				fallbackDelete(song);
			}
			} catch (SecurityException e) {
			if (Build.VERSION.SDK_INT >= 30) {
				try {
					List<Uri> uris = new ArrayList<>();
					uris.add(uri);
					PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), uris);
					startIntentSenderForResult(pi.getIntentSender(), 102, null, 0, 0, 0, null);
					} catch (Exception ex) {
					fallbackDelete(song);
				}
				} else {
				fallbackDelete(song);
			}
		}
	}
	
	private void fallbackDelete(Song song) {
		try {
			File file = new File(song.path);
			if (file.exists() && file.delete()) {
				getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.DATA + "=?", new String[]{song.path});
				loadMusic();
				Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "Permission denied by Android System", Toast.LENGTH_LONG).show();
			}
			} catch (Exception e) {
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void renameMediaFile(Song song, String newName) {
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id);
		try {
			ContentValues values = new ContentValues();
			values.put(MediaStore.Audio.Media.TITLE, newName);
			String extension = song.path.substring(song.path.lastIndexOf("."));
			values.put(MediaStore.Audio.Media.DISPLAY_NAME, newName + extension);
			
			int updated = getContentResolver().update(uri, values, null, null);
			if (updated > 0) {
				loadMusic();
				Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
				} else {
				fallbackRename(song, newName);
			}
			} catch (SecurityException e) {
			if (Build.VERSION.SDK_INT >= 30) {
				try {
					pendingSongForRename = song;
					pendingNewName = newName;
					List<Uri> uris = new ArrayList<>();
					uris.add(uri);
					PendingIntent pi = MediaStore.createWriteRequest(getContentResolver(), uris);
					startIntentSenderForResult(pi.getIntentSender(), 103, null, 0, 0, 0, null);
					} catch (Exception ex) {
					fallbackRename(song, newName);
				}
				} else {
				fallbackRename(song, newName);
			}
		}
	}
	
	private void fallbackRename(Song song, String newName) {
		try {
			File file = new File(song.path);
			String extension = file.getName().substring(file.getName().lastIndexOf("."));
			File newFile = new File(file.getParent(), newName + extension);
			
			if (file.renameTo(newFile)) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Audio.Media.DATA, newFile.getAbsolutePath());
				values.put(MediaStore.Audio.Media.TITLE, newName);
				values.put(MediaStore.Audio.Media.DISPLAY_NAME, newFile.getName());
				getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Media._ID + "=?", new String[]{String.valueOf(song.id)});
				loadMusic();
				Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "Permission denied by Android System", Toast.LENGTH_LONG).show();
			}
			} catch (Exception e) {
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 102 && resultCode == RESULT_OK) {
			loadMusic();
			Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
			} else if (requestCode == 103 && resultCode == RESULT_OK) {
			if (pendingSongForRename != null && pendingNewName != null) {
				renameMediaFile(pendingSongForRename, pendingNewName);
				pendingSongForRename = null;
				pendingNewName = null;
			}
		}
	}
}