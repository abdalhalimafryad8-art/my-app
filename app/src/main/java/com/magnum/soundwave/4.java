package com.magnum.soundwave;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

class MainActivityV4 extends MainActivityV3 {
	
	private void applyLiquidBlur(Dialog dialog) {
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			dialog.getWindow().setDimAmount(0.45f);
			if (Build.VERSION.SDK_INT >= 31) {
				dialog.getWindow().setBackgroundBlurRadius(60);
			}
		}
	}
	
	@Override
	protected void showSongOptionsDialog(Song song) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		applyLiquidBlur(dialog);
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		container.setLayoutParams(new ViewGroup.LayoutParams(
		(int) (310 * getResources().getDisplayMetrics().density),
		ViewGroup.LayoutParams.WRAP_CONTENT
		));
		
		TextView titleView = new TextView(this);
		titleView.setText(song.title);
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setSingleLine(true);
		titleView.setShadowLayer(4, 0, 2, 0x80000000);
		container.addView(titleView);
		
		View divider = new View(this);
		LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
		divParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
		divider.setLayoutParams(divParams);
		divider.setBackgroundColor(0x1AFFFFFF);
		container.addView(divider);
		
		String[] options = {"Play Track", "Rename File", "Delete File"};
		int[] colors = {0xFF00E676, 0xFF29B6F6, 0xFFFF5252};
		
		for (int i = 0; i < options.length; i++) {
			TextView optionView = new TextView(this);
			optionView.setText(options[i]);
			optionView.setTextColor(colors[i]);
			optionView.setTextSize(15);
			optionView.setTypeface(Typeface.DEFAULT_BOLD);
			optionView.setGravity(Gravity.CENTER);
			optionView.setShadowLayer(2, 0, 1, 0x40000000);
			
			GradientDrawable btnBg = new GradientDrawable();
			btnBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
			btnBg.setCornerRadius((int) (20 * getResources().getDisplayMetrics().density));
			btnBg.setStroke(2, isDarkMode ? 0x1AFFFFFF : 0x12000000);
			optionView.setBackground(btnBg);
			
			LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			(int) (48 * getResources().getDisplayMetrics().density)
			);
			btnParams.setMargins(0, (int) (6 * getResources().getDisplayMetrics().density), 0, (int) (6 * getResources().getDisplayMetrics().density));
			optionView.setLayoutParams(btnParams);
			
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
		applyLiquidBlur(dialog);
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		container.setLayoutParams(new ViewGroup.LayoutParams(
		(int) (310 * getResources().getDisplayMetrics().density),
		ViewGroup.LayoutParams.WRAP_CONTENT
		));
		
		TextView titleView = new TextView(this);
		titleView.setText("Rename Track");
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setShadowLayer(4, 0, 2, 0x80000000);
		container.addView(titleView);
		
		EditText input = new EditText(this);
		input.setText(song.title);
		input.setTextColor(Color.WHITE);
		input.setHintTextColor(0x40FFFFFF);
		input.setSingleLine(true);
		
		GradientDrawable inputBg = new GradientDrawable();
		inputBg.setColor(isDarkMode ? 0x0DFFFFFF : 0x0A000000);
		inputBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		inputBg.setStroke(2, isDarkMode ? 0x1AFFFFFF : 0x12000000);
		input.setBackground(inputBg);
		
		int inputPadding = (int) (12 * getResources().getDisplayMetrics().density);
		input.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
		
		LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		inputParams.setMargins(0, (int) (20 * getResources().getDisplayMetrics().density), 0, (int) (24 * getResources().getDisplayMetrics().density));
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
		btnCancel.setShadowLayer(2, 0, 1, 0x40000000);
		
		GradientDrawable cancelBg = new GradientDrawable();
		cancelBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		cancelBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnCancel.setBackground(cancelBg);
		
		LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		cancelParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
		btnCancel.setLayoutParams(cancelParams);
		btnCancel.setOnClickListener(v -> dialog.dismiss());
		
		TextView btnSave = new TextView(this);
		btnSave.setText("SAVE");
		btnSave.setTextColor(0xFF00E676);
		btnSave.setTextSize(14);
		btnSave.setTypeface(Typeface.DEFAULT_BOLD);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setShadowLayer(2, 0, 1, 0x40000000);
		
		GradientDrawable saveBg = new GradientDrawable();
		saveBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		saveBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnSave.setBackground(saveBg);
		
		LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		saveParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
		btnSave.setLayoutParams(saveParams);
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
		applyLiquidBlur(dialog);
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		container.setLayoutParams(new ViewGroup.LayoutParams(
		(int) (310 * getResources().getDisplayMetrics().density),
		ViewGroup.LayoutParams.WRAP_CONTENT
		));
		
		TextView titleView = new TextView(this);
		titleView.setText("Delete Track");
		titleView.setTextColor(0xFFFF5252);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setShadowLayer(4, 0, 2, 0x80000000);
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
		btnCancel.setShadowLayer(2, 0, 1, 0x40000000);
		
		GradientDrawable cancelBg = new GradientDrawable();
		cancelBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		cancelBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnCancel.setBackground(cancelBg);
		
		LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		cancelParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
		btnCancel.setLayoutParams(cancelParams);
		btnCancel.setOnClickListener(v -> dialog.dismiss());
		
		TextView btnDelete = new TextView(this);
		btnDelete.setText("DELETE");
		btnDelete.setTextColor(0xFFFF5252);
		btnDelete.setTextSize(14);
		btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
		btnDelete.setGravity(Gravity.CENTER);
		btnDelete.setShadowLayer(2, 0, 1, 0x40000000);
		
		GradientDrawable deleteBg = new GradientDrawable();
		deleteBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		deleteBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnDelete.setBackground(deleteBg);
		
		LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		deleteParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
		btnDelete.setLayoutParams(deleteParams);
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
}