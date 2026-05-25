package com.magnum.soundwave;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class MainActivityV6 extends MainActivityV5 {
	
	protected MediaPlayer dialogPlayer;
	protected Handler previewHandler = new Handler();
	protected Runnable previewRunnable;
	protected boolean isPreviewPlaying = false;
	protected float startCutMs = 0;
	protected float endCutMs = 0;
	
	static class RangeSeekBar extends View {
		private float min = 0;
		private float max = 100;
		private float selectedMin = 0;
		private float selectedMax = 100;
		private boolean isDraggingLeft = false;
		private boolean isDraggingRight = false;
		private OnRangeChangeListener listener;
		private int thumbRadius = 36;
		private int activeColor = 0xFFFF4081;
		private int inactiveColor = 0x33FFFFFF;
		
		public interface OnRangeChangeListener {
			void onRangeChanged(float minVal, float maxVal);
		}
		
		public RangeSeekBar(Context context) {
			super(context);
			thumbRadius = (int) (16 * context.getResources().getDisplayMetrics().density);
		}
		
		public void setRange(float min, float max) {
			this.min = min;
			this.max = max;
			this.selectedMin = min;
			this.selectedMax = max;
			invalidate();
		}
		
		public void setOnRangeChangeListener(OnRangeChangeListener listener) {
			this.listener = listener;
		}
		
		public float getSelectedMin() { return selectedMin; }
		public float getSelectedMax() { return selectedMax; }
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			float w = getWidth();
			float h = getHeight();
			float cy = h / 2f;
			float padding = thumbRadius;
			float trackLen = w - 2 * padding;
			
			float x1 = padding + ((selectedMin - min) / (max - min)) * trackLen;
			float x2 = padding + ((selectedMax - min) / (max - min)) * trackLen;
			
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setStrokeWidth((int) (6 * getResources().getDisplayMetrics().density));
			paint.setStrokeCap(Paint.Cap.ROUND);
			
			paint.setColor(inactiveColor);
			canvas.drawLine(padding, cy, w - padding, cy, paint);
			
			paint.setColor(activeColor);
			canvas.drawLine(x1, cy, x2, cy, paint);
			
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.WHITE);
			canvas.drawCircle(x1, cy, thumbRadius, paint);
			canvas.drawCircle(x2, cy, thumbRadius, paint);
			
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
			paint.setColor(activeColor);
			canvas.drawCircle(x1, cy, thumbRadius, paint);
			canvas.drawCircle(x2, cy, thumbRadius, paint);
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float w = getWidth();
			float padding = thumbRadius;
			float trackLen = w - 2 * padding;
			
			float x1 = padding + ((selectedMin - min) / (max - min)) * trackLen;
			float x2 = padding + ((selectedMax - min) / (max - min)) * trackLen;
			
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				float d1 = Math.abs(x - x1);
				float d2 = Math.abs(x - x2);
				if (d1 < d2 && d1 < thumbRadius * 2) {
					isDraggingLeft = true;
					} else if (d2 < d1 && d2 < thumbRadius * 2) {
					isDraggingRight = true;
				}
				break;
				case MotionEvent.ACTION_MOVE:
				float val = min + ((x - padding) / trackLen) * (max - min);
				val = Math.max(min, Math.min(max, val));
				if (isDraggingLeft) {
					if (val < selectedMax - (max - min) * 0.05f) {
						selectedMin = val;
					}
					} else if (isDraggingRight) {
					if (val > selectedMin + (max - min) * 0.05f) {
						selectedMax = val;
					}
				}
				invalidate();
				if (listener != null) {
					listener.onRangeChanged(selectedMin, selectedMax);
				}
				break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
				isDraggingLeft = false;
				isDraggingRight = false;
				break;
			}
			return true;
		}
	}
	
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
	
	private void forceDialogSize(Dialog dialog) {
		if (dialog.getWindow() != null) {
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(dialog.getWindow().getAttributes());
			lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
			lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
			dialog.getWindow().setAttributes(lp);
		}
	}
	
	private void saveTrimmedSong(long id, String title, String artist, String path, long duration) {
		SharedPreferences prefs = getSharedPreferences("virtual_media_prefs", MODE_PRIVATE);
		String currentList = prefs.getString("trimmed_list", "");
		String newEntry = id + "|" + title + "|" + artist + "|" + path + "|" + duration;
		if (currentList.isEmpty()) {
			currentList = newEntry;
			} else {
			currentList += ";;;" + newEntry;
		}
		prefs.edit().putString("trimmed_list", currentList).apply();
	}
	
	private void cutAudioFile(Song song, long startMs, long endMs) {
		Dialog progressDialog = createCustomProgressDialog("Trimming audio... Please wait.");
		progressDialog.show();
		forceDialogSize(progressDialog);
		
		new Thread(() -> {
			boolean success = false;
			String outPath = null;
			long trimmedDuration = endMs - startMs;
			try {
				File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
				if (dir != null && !dir.exists()) {
					dir.mkdirs();
				}
				String name = song.title + "_trimmed_" + System.currentTimeMillis() + ".mp3";
				outPath = new File(dir, name).getAbsolutePath();
				
				trimAudio(song.path, outPath, startMs, endMs);
				success = true;
				} catch (Exception e) {
				success = false;
			}
			
			final boolean finalSuccess = success;
			final String finalPath = outPath;
			runOnUiThread(() -> {
				progressDialog.dismiss();
				if (finalSuccess) {
					long newId = -System.currentTimeMillis();
					saveTrimmedSong(newId, song.title + " (Trimmed)", song.artist, finalPath, trimmedDuration);
					loadMusic();
					Toast.makeText(MainActivityV6.this, "Audio trimmed & saved successfully!", Toast.LENGTH_LONG).show();
					} else {
					Toast.makeText(MainActivityV6.this, "Failed to trim audio.", Toast.LENGTH_SHORT).show();
				}
			});
		}).start();
	}
	
	private void trimAudio(String inputPath, String outputPath, long startMs, long endMs) throws Exception {
		MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(inputPath);
		
		int trackCount = extractor.getTrackCount();
		int audioTrackIndex = -1;
		MediaFormat format = null;
		for (int i = 0; i < trackCount; i++) {
			format = extractor.getTrackFormat(i);
			String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith("audio/")) {
				audioTrackIndex = i;
				break;
			}
		}
		
		if (audioTrackIndex >= 0) {
			extractor.selectTrack(audioTrackIndex);
			MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			int writeTrackIndex = muxer.addTrack(format);
			muxer.start();
			
			extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
			ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			
			while (true) {
				bufferInfo.offset = 0;
				bufferInfo.size = extractor.readSampleData(buffer, 0);
				if (bufferInfo.size < 0) {
					bufferInfo.size = 0;
					break;
				}
				
				long sampleTime = extractor.getSampleTime();
				if (sampleTime > endMs * 1000) {
					break;
				}
				
				bufferInfo.presentationTimeUs = sampleTime;
				bufferInfo.flags = extractor.getSampleFlags();
				muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo);
				extractor.advance();
			}
			
			muxer.stop();
			muxer.release();
		}
		extractor.release();
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
		
		String[] options = {"Play Track", "Rename File", "Delete File", "Edit Track"};
		int[] colors = {0xFF00E676, 0xFF29B6F6, 0xFFFF5252, 0xFFFFB74D};
		
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
				else if (index == 3) showEditTrackDialog(song);
			});
			container.addView(optionView);
		}
		
		dialog.setContentView(container);
		dialog.show();
		forceDialogSize(dialog);
	}
	
	private void showEditTrackDialog(Song song) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		applyLiquidBlur(dialog);
		
		try {
			dialogPlayer = new MediaPlayer();
			dialogPlayer.setDataSource(song.path);
			dialogPlayer.prepare();
			} catch (Exception e) {
			Toast.makeText(this, "Failed to load audio preview", Toast.LENGTH_SHORT).show();
			return;
		}
		
		startCutMs = 0;
		endCutMs = song.duration;
		isPreviewPlaying = false;
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundResource(R.drawable.ic_o);
		int padding = (int) (24 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		
		TextView titleView = new TextView(this);
		titleView.setText("Edit Track");
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setShadowLayer(4, 0, 2, 0x80000000);
		container.addView(titleView);
		
		TextView durView = new TextView(this);
		durView.setText("Length: " + formatDuration(song.duration));
		durView.setTextColor(0xFF8A94A6);
		durView.setTextSize(13);
		durView.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams durParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		durParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, (int) (12 * getResources().getDisplayMetrics().density));
		durView.setLayoutParams(durParams);
		container.addView(durView);
		
		TextView rangeLabel = new TextView(this);
		rangeLabel.setText("Range: 00:00 - " + formatDuration(song.duration));
		rangeLabel.setTextColor(Color.WHITE);
		rangeLabel.setTextSize(14);
		rangeLabel.setGravity(Gravity.CENTER);
		container.addView(rangeLabel);
		
		RangeSeekBar rangeSeekBar = new RangeSeekBar(this);
		rangeSeekBar.setRange(0, song.duration);
		LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		(int) (56 * getResources().getDisplayMetrics().density)
		);
		seekParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
		rangeSeekBar.setLayoutParams(seekParams);
		container.addView(rangeSeekBar);
		
		ImageView btnPlayPreview = new ImageView(this);
		btnPlayPreview.setImageResource(R.drawable.ic_all);
		btnPlayPreview.setImageLevel(0);
		
		GradientDrawable playBg = new GradientDrawable();
		playBg.setColor(isDarkMode ? 0x1AFFFFFF : 0x0D000000);
		playBg.setShape(GradientDrawable.OVAL);
		playBg.setStroke(2, isDarkMode ? 0x26FFFFFF : 0x12000000);
		btnPlayPreview.setBackground(playBg);
		
		int playSize = (int) (56 * getResources().getDisplayMetrics().density);
		int playPad = (int) (14 * getResources().getDisplayMetrics().density);
		btnPlayPreview.setPadding(playPad, playPad, playPad, playPad);
		LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(playSize, playSize);
		playParams.gravity = Gravity.CENTER;
		playParams.setMargins(0, 0, 0, (int) (24 * getResources().getDisplayMetrics().density));
		btnPlayPreview.setLayoutParams(playParams);
		container.addView(btnPlayPreview);
		
		rangeSeekBar.setOnRangeChangeListener((minVal, maxVal) -> {
			startCutMs = minVal;
			endCutMs = maxVal;
			rangeLabel.setText("Range: " + formatDuration((long) startCutMs) + " - " + formatDuration((long) endCutMs));
			
			if (dialogPlayer != null && isPreviewPlaying) {
				dialogPlayer.seekTo((int) startCutMs);
			}
		});
		
		previewRunnable = new Runnable() {
			@Override
			public void run() {
				if (dialogPlayer != null && isPreviewPlaying) {
					try {
						int pos = dialogPlayer.getCurrentPosition();
						if (pos >= (int) endCutMs || pos < (int) startCutMs) {
							dialogPlayer.seekTo((int) startCutMs);
						}
					} catch (Exception ignored) {}
					previewHandler.postDelayed(this, 100);
				}
			}
		};
		
		btnPlayPreview.setOnClickListener(v -> {
			if (dialogPlayer == null) return;
			if (isPreviewPlaying) {
				dialogPlayer.pause();
				isPreviewPlaying = false;
				btnPlayPreview.setImageLevel(0);
				previewHandler.removeCallbacks(previewRunnable);
				} else {
				dialogPlayer.seekTo((int) startCutMs);
				dialogPlayer.start();
				isPreviewPlaying = true;
				btnPlayPreview.setImageLevel(1);
				previewHandler.post(previewRunnable);
			}
		});
		
		LinearLayout btnContainer = new LinearLayout(this);
		btnContainer.setOrientation(LinearLayout.HORIZONTAL);
		btnContainer.setWeightSum(2);
		
		TextView btnCancel = new TextView(this);
		btnCancel.setText("CANCEL");
		btnCancel.setTextColor(0xFF8A94A6);
		btnCancel.setTextSize(14);
		btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
		btnCancel.setGravity(Gravity.CENTER);
		
		GradientDrawable cancelBg = new GradientDrawable();
		cancelBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		cancelBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnCancel.setBackground(cancelBg);
		
		LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		cancelParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
		btnCancel.setLayoutParams(cancelParams);
		btnCancel.setOnClickListener(v -> dialog.dismiss());
		
		TextView btnCut = new TextView(this);
		btnCut.setText("CUT & SAVE");
		btnCut.setTextColor(0xFFFFB74D);
		btnCut.setTextSize(14);
		btnCut.setTypeface(Typeface.DEFAULT_BOLD);
		btnCut.setGravity(Gravity.CENTER);
		
		GradientDrawable cutBg = new GradientDrawable();
		cutBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		cutBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnCut.setBackground(cutBg);
		
		LinearLayout.LayoutParams cutParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		cutParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
		btnCut.setLayoutParams(cutParams);
		btnCut.setOnClickListener(v -> {
			dialog.dismiss();
			cutAudioFile(song, (long) startCutMs, (long) endCutMs);
		});
		
		btnContainer.addView(btnCancel);
		btnContainer.addView(btnCut);
		container.addView(btnContainer);
		
		dialog.setContentView(container);
		
		dialog.setOnDismissListener(dialogInterface -> {
			isPreviewPlaying = false;
			previewHandler.removeCallbacks(previewRunnable);
			if (dialogPlayer != null) {
				try {
					dialogPlayer.stop();
					dialogPlayer.release();
				} catch (Exception ignored) {}
				dialogPlayer = null;
			}
		});
		
		dialog.show();
		forceDialogSize(dialog);
	}
}