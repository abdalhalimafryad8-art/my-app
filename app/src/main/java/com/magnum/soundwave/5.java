package com.magnum.soundwave;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MainActivityV5 extends MainActivityV4 {
	
	protected boolean isSelectionMode = false;
	protected Set<Long> selectedSongIds = new HashSet<>();
	protected ImageView deleteFab;
	protected ProgressBar miniProgressBar;
	protected TextView miniTimeView;
	
	private final int[] avatarColors = {
		0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1,
		0xFF3949AB, 0xFF1E88E5, 0xFF00897B, 0xFF43A047,
		0xFFFF8F00, 0xFFF4511E, 0xFF6D4C41, 0xFF546E7A
	};
	
	@Override
	protected void initViews() {
		super.initViews();
		
		ListView musicListView = findViewById(R.id.music_list_view);
		musicAdapter = new MusicAdapterV3();
		musicListView.setAdapter(musicAdapter);
		
		musicListView.setOnItemLongClickListener((parent, view, position, id) -> {
			toggleSelectionMode(position);
			return true;
		});
		
		setupProgrammaticViews();
	}
	
	@Override
	protected void setupListeners() {
		super.setupListeners();
		
		ListView musicListView = findViewById(R.id.music_list_view);
		musicListView.setOnItemClickListener((parent, view, position, id) -> {
			if (isSelectionMode) {
				toggleSelection(songsList.get(position).id);
				} else {
				playSong(songsList.get(position));
			}
		});
	}
	
	private void setupProgrammaticViews() {
		ViewGroup root = findViewById(R.id.main_root_layout);
		if (root == null) return;
		
		deleteFab = new ImageView(this);
		deleteFab.setImageResource(R.drawable.ic_all);
		deleteFab.setImageLevel(3);
		
		GradientDrawable fabBg = new GradientDrawable();
		fabBg.setColor(0xFFFF5252);
		fabBg.setShape(GradientDrawable.OVAL);
		fabBg.setStroke(4, Color.WHITE);
		deleteFab.setBackground(fabBg);
		
		int fabSize = (int) (60 * getResources().getDisplayMetrics().density);
		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		deleteFab.setPadding(padding, padding, padding, padding);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(fabSize, fabSize);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.setMargins(0, 0, (int) (24 * getResources().getDisplayMetrics().density), (int) (104 * getResources().getDisplayMetrics().density));
		
		deleteFab.setLayoutParams(params);
		deleteFab.setVisibility(View.GONE);
		deleteFab.setOnClickListener(v -> deleteSelectedSongs());
		
		root.addView(deleteFab);
		
		setupMiniPlayerProgress();
	}
	
	private void setupMiniPlayerProgress() {
		ViewGroup miniPlayer = findViewById(R.id.mini_player_bar);
		if (miniPlayer == null) return;
		
		miniProgressBar = new ProgressBar(this, null, android.R.style.Widget_ProgressBar_Horizontal);
		RelativeLayout.LayoutParams progressParams = new RelativeLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		(int) (4 * getResources().getDisplayMetrics().density)
		);
		progressParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		miniProgressBar.setLayoutParams(progressParams);
		
		GradientDrawable pgBg = new GradientDrawable();
		pgBg.setColor(0x1AFFFFFF);
		GradientDrawable pgProgress = new GradientDrawable();
		pgProgress.setColor(0xFFFF4081);
		ClipDrawable clip = new ClipDrawable(pgProgress, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		
		LayerDrawable layer = new LayerDrawable(new android.graphics.drawable.Drawable[]{pgBg, clip});
		layer.setId(0, android.R.id.background);
		layer.setId(1, android.R.id.progress);
		miniProgressBar.setProgressDrawable(layer);
		miniPlayer.addView(miniProgressBar);
		
		LinearLayout textLayout = null;
		for (int i = 0; i < miniPlayer.getChildCount(); i++) {
			View child = miniPlayer.getChildAt(i);
			if (child instanceof LinearLayout) {
				textLayout = (LinearLayout) child;
				break;
			}
		}
		
		if (textLayout != null) {
			miniTimeView = new TextView(this);
			miniTimeView.setText("00:00 / 00:00");
			miniTimeView.setTextSize(11);
			miniTimeView.setTextColor(isDarkMode ? 0xFF8A94A6 : 0xFF757575);
			
			LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
			);
			textParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
			miniTimeView.setLayoutParams(textParams);
			textLayout.addView(miniTimeView);
		}
	}
	
	@Override
	protected void startSeekBarUpdate() {
		if (seekRunnable == null) {
			seekRunnable = new Runnable() {
				@Override
				public void run() {
					if (mediaPlayer != null && isPlaying) {
						try {
							int currentPos = mediaPlayer.getCurrentPosition();
							int totalPos = mediaPlayer.getDuration();
							
							SeekBar sb = findViewById(R.id.player_seekbar);
							if (sb != null) {
								sb.setProgress(currentPos);
							}
							TextView timeCur = findViewById(R.id.player_time_current);
							if (timeCur != null) {
								timeCur.setText(formatDuration(currentPos));
							}
							
							if (miniProgressBar != null) {
								miniProgressBar.setMax(totalPos);
								miniProgressBar.setProgress(currentPos);
							}
							if (miniTimeView != null) {
								miniTimeView.setText(formatDuration(currentPos) + " / " + formatDuration(totalPos));
							}
						} catch (Exception ignored) {}
					}
					seekHandler.postDelayed(this, 1000);
				}
			};
		}
		seekHandler.postDelayed(seekRunnable, 1000);
	}
	
	private void toggleSelectionMode(int startPosition) {
		isSelectionMode = true;
		selectedSongIds.clear();
		selectedSongIds.add(songsList.get(startPosition).id);
		deleteFab.setVisibility(View.VISIBLE);
		musicAdapter.notifyDataSetChanged();
		Toast.makeText(this, "Batch Selection Active", Toast.LENGTH_SHORT).show();
	}
	
	private void toggleSelection(long songId) {
		if (selectedSongIds.contains(songId)) {
			selectedSongIds.remove(songId);
			} else {
			selectedSongIds.add(songId);
		}
		if (selectedSongIds.isEmpty()) {
			exitSelectionMode();
			} else {
			musicAdapter.notifyDataSetChanged();
		}
	}
	
	private void exitSelectionMode() {
		isSelectionMode = false;
		selectedSongIds.clear();
		deleteFab.setVisibility(View.GONE);
		musicAdapter.notifyDataSetChanged();
	}
	
	private void deleteSelectedSongs() {
		SharedPreferences prefs = getSharedPreferences("virtual_media_prefs", MODE_PRIVATE);
		Set<String> deletedIds = new HashSet<>(prefs.getStringSet("deleted_ids", new HashSet<>()));
		for (Long id : selectedSongIds) {
			deletedIds.add(String.valueOf(id));
		}
		prefs.edit().putStringSet("deleted_ids", deletedIds).apply();
		exitSelectionMode();
		loadMusic();
		Toast.makeText(this, "Selected tracks deleted", Toast.LENGTH_SHORT).show();
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
	
	@Override
	protected void loadMusic() {
		songsList.clear();
		ContentResolver contentResolver = getContentResolver();
		Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
		String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
		Cursor cursor = contentResolver.query(musicUri, null, selection, null, sortOrder);
		
		SharedPreferences prefs = getSharedPreferences("virtual_media_prefs", MODE_PRIVATE);
		Set<String> deletedIds = prefs.getStringSet("deleted_ids", new HashSet<>());
		
		if (cursor != null && cursor.moveToFirst()) {
			int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
			int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
			int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
			do {
				long id = cursor.getLong(idCol);
				if (deletedIds.contains(String.valueOf(id))) {
					continue;
				}
				String title = cursor.getString(titleCol);
				String renamed = prefs.getString("rename_" + id, null);
				if (renamed != null) {
					title = renamed;
				}
				String artist = cursor.getString(artistCol);
				String data = cursor.getString(dataCol);
				long duration = cursor.getLong(durationCol);
				songsList.add(new Song(id, title, artist, data, duration));
			} while (cursor.moveToNext());
			cursor.close();
		}
		
		String trimmedList = prefs.getString("trimmed_list", "");
		if (!trimmedList.isEmpty()) {
			String[] entries = trimmedList.split(";;;");
			for (String entry : entries) {
				String[] parts = entry.split("\\|");
				if (parts.length == 5) {
					long id = Long.parseLong(parts[0]);
					if (deletedIds.contains(String.valueOf(id))) {
						continue;
					}
					String title = parts[1];
					String renamed = prefs.getString("rename_" + id, null);
					if (renamed != null) {
						title = renamed;
					}
					String artist = parts[2];
					String path = parts[3];
					long duration = Long.parseLong(parts[4]);
					songsList.add(new Song(id, title, artist, path, duration));
				}
			}
		}
		
		if (songsList.isEmpty()) {
			findViewById(R.id.empty_state_text).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.empty_state_text)).setText("No audio tracks found.");
			} else {
			findViewById(R.id.empty_state_text).setVisibility(View.GONE);
		}
		if (musicAdapter != null) musicAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void renameMediaFile(Song song, String newName) {
		SharedPreferences prefs = getSharedPreferences("virtual_media_prefs", MODE_PRIVATE);
		prefs.edit().putString("rename_" + song.id, newName).apply();
		loadMusic();
		Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void deleteMediaFile(Song song) {
		SharedPreferences prefs = getSharedPreferences("virtual_media_prefs", MODE_PRIVATE);
		Set<String> deletedIds = new HashSet<>(prefs.getStringSet("deleted_ids", new HashSet<>()));
		deletedIds.add(String.valueOf(song.id));
		prefs.edit().putStringSet("deleted_ids", deletedIds).apply();
		loadMusic();
		Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
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
	}
	
	private void showEditTrackDialog(Song song) {
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
		titleView.setText("Edit Track");
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(18);
		titleView.setTypeface(Typeface.DEFAULT_BOLD);
		titleView.setGravity(Gravity.CENTER);
		titleView.setShadowLayer(4, 0, 2, 0x80000000);
		container.addView(titleView);
		
		TextView durView = new TextView(this);
		durView.setText("Total Length: " + formatDuration(song.duration));
		durView.setTextColor(0xFF8A94A6);
		durView.setTextSize(13);
		durView.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams durParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		durParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
		durView.setLayoutParams(durParams);
		container.addView(durView);
		
		TextView startLabel = new TextView(this);
		startLabel.setText("Start Cut: 00:00");
		startLabel.setTextColor(Color.WHITE);
		startLabel.setTextSize(14);
		container.addView(startLabel);
		
		SeekBar startSeekBar = new SeekBar(this);
		startSeekBar.setMax((int) song.duration);
		startSeekBar.setProgress(0);
		startSeekBar.getProgressDrawable().setColorFilter(0xFFFFB74D, PorterDuff.Mode.SRC_IN);
		startSeekBar.getThumb().setColorFilter(0xFFFFB74D, PorterDuff.Mode.SRC_IN);
		LinearLayout.LayoutParams seekParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		seekParams1.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
		startSeekBar.setLayoutParams(seekParams1);
		container.addView(startSeekBar);
		
		TextView endLabel = new TextView(this);
		endLabel.setText("End Cut: " + formatDuration(song.duration));
		endLabel.setTextColor(Color.WHITE);
		endLabel.setTextSize(14);
		container.addView(endLabel);
		
		SeekBar endSeekBar = new SeekBar(this);
		endSeekBar.setMax((int) song.duration);
		endSeekBar.setProgress((int) song.duration);
		endSeekBar.getProgressDrawable().setColorFilter(0xFFFFB74D, PorterDuff.Mode.SRC_IN);
		endSeekBar.getThumb().setColorFilter(0xFFFFB74D, PorterDuff.Mode.SRC_IN);
		LinearLayout.LayoutParams seekParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		seekParams2.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (24 * getResources().getDisplayMetrics().density));
		endSeekBar.setLayoutParams(seekParams2);
		container.addView(endSeekBar);
		
		startSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress > endSeekBar.getProgress()) {
					endSeekBar.setProgress(progress);
				}
				startLabel.setText("Start Cut: " + formatDuration(progress));
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		endSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress < startSeekBar.getProgress()) {
					startSeekBar.setProgress(progress);
				}
				endLabel.setText("End Cut: " + formatDuration(progress));
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
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
		btnCancel.setShadowLayer(2, 0, 1, 0x40000000);
		
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
		btnCut.setShadowLayer(2, 0, 1, 0x40000000);
		
		GradientDrawable cutBg = new GradientDrawable();
		cutBg.setColor(isDarkMode ? 0x14FFFFFF : 0x0D000000);
		cutBg.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
		btnCut.setBackground(cutBg);
		
		LinearLayout.LayoutParams cutParams = new LinearLayout.LayoutParams(0, (int) (40 * getResources().getDisplayMetrics().density), 1);
		cutParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
		btnCut.setLayoutParams(cutParams);
		btnCut.setOnClickListener(v -> {
			dialog.dismiss();
			cutAudioFile(song, startSeekBar.getProgress(), endSeekBar.getProgress());
		});
		
		btnContainer.addView(btnCancel);
		btnContainer.addView(btnCut);
		container.addView(btnContainer);
		
		dialog.setContentView(container);
		dialog.show();
	}
	
	private void cutAudioFile(Song song, long startMs, long endMs) {
		Dialog progressDialog = createCustomProgressDialog("Trimming audio... Please wait.");
		progressDialog.show();
		
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
					Toast.makeText(MainActivityV5.this, "Audio trimmed & saved successfully!", Toast.LENGTH_LONG).show();
					} else {
					Toast.makeText(MainActivityV5.this, "Failed to trim audio.", Toast.LENGTH_SHORT).show();
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
	public void onBackPressed() {
		if (isSelectionMode) {
			exitSelectionMode();
			} else {
			super.onBackPressed();
		}
	}
	
	private Bitmap createNumberBitmap(int number, int color) {
		int size = (int) (64 * getResources().getDisplayMetrics().density);
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
		
		paint.setColor(Color.WHITE);
		paint.setTextSize(size / 2.2f);
		paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		paint.setTextAlign(Paint.Align.CENTER);
		
		float yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);
		canvas.drawText(String.valueOf(number), size / 2f, yPos, paint);
		
		return bitmap;
	}
	
	private class MusicAdapterV3 extends MusicAdapter {
		public MusicAdapterV3() {
			super(songsList);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			TextView title = view.findViewById(R.id.item_title);
			ImageView icon = view.findViewById(R.id.item_icon);
			Song song = songsList.get(position);
			
			title.setText((position + 1) + " - " + song.title);
			
			int colorIndex = position % avatarColors.length;
			Bitmap numberBitmap = createNumberBitmap(position + 1, avatarColors[colorIndex]);
			icon.setImageBitmap(numberBitmap);
			icon.setBackgroundColor(Color.TRANSPARENT);
			icon.setPadding(0, 0, 0, 0);
			icon.clearColorFilter();
			
			View container = view.findViewById(R.id.item_container_layout);
			if (isSelectionMode && selectedSongIds.contains(song.id)) {
				GradientDrawable cardBg = new GradientDrawable();
				cardBg.setColor(isDarkMode ? 0x33FF4081 : 0xFFFFF0F5);
				cardBg.setCornerRadius(28);
				cardBg.setStroke(4, 0xFFFF4081);
				container.setBackground(cardBg);
				} else {
				GradientDrawable cardBg = new GradientDrawable();
				cardBg.setColor(isDarkMode ? 0xFF1A1D29 : 0xFFFFFFFF);
				cardBg.setCornerRadius(28);
				cardBg.setStroke(2, isDarkMode ? 0xFF2A2E3D : 0xFFE2E8F0);
				container.setBackground(cardBg);
			}
			return view;
		}
	}
}