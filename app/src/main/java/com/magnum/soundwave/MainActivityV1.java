package com.magnum.soundwave;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivityV1 extends Activity {
	
	protected static class Song {
		long id;
		String title;
		String artist;
		String path;
		long duration;
		
		public Song(long id, String title, String artist, String path, long duration) {
			this.id = id;
			this.title = title;
			this.artist = artist;
			this.path = path;
			this.duration = duration;
		}
	}
	
	protected static class Video {
		long id;
		String title;
		String path;
		long duration;
		
		public Video(long id, String title, String path, long duration) {
			this.id = id;
			this.title = title;
			this.path = path;
			this.duration = duration;
		}
	}
	
	protected MediaPlayer mediaPlayer;
	protected List<Song> songsList = new ArrayList<>();
	protected List<Video> videosList = new ArrayList<>();
	protected MusicAdapter musicAdapter;
	protected VideoAdapter videoAdapter;
	protected int currentSongIndex = -1;
	protected boolean isPlaying = false;
	protected boolean isDarkMode = true;
	protected SharedPreferences sharedPreferences;
	protected Handler seekHandler = new Handler();
	protected Runnable seekRunnable;
	protected RotateAnimation rotateAnimation;
	protected LruCache<Long, Bitmap> thumbnailCache = new LruCache<>(120);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			runOnUiThread(() -> showCrashScreen(throwable));
		});
		
		setContentView(R.layout.activity_main);
		
		sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
		isDarkMode = sharedPreferences.getBoolean("dark_mode", true);
		
		mediaPlayer = new MediaPlayer();
		
		initViews();
		setupListeners();
		
		if (checkPermissions()) {
			loadMusic();
			loadVideos();
			} else {
			requestPermissions();
		}
		
		applyTheme(findViewById(R.id.main_root_layout));
	}
	
	protected void initViews() {
		TextView toolbarTitle = findViewById(R.id.toolbar_title);
		toolbarTitle.setText("SoundWave");
		
		ImageView btnConvertVideo = findViewById(R.id.btn_convert_video);
		btnConvertVideo.setImageResource(R.drawable.ic_all);
		btnConvertVideo.setImageLevel(6);
		
		ImageView btnToggleTheme = findViewById(R.id.btn_toggle_theme);
		btnToggleTheme.setImageResource(R.drawable.ic_all);
		btnToggleTheme.setImageLevel(8);
		
		ListView musicListView = findViewById(R.id.music_list_view);
		musicAdapter = new MusicAdapter(songsList);
		musicListView.setAdapter(musicAdapter);
		
		ListView videoListView = findViewById(R.id.video_list_view);
		videoAdapter = new VideoAdapter(videosList);
		videoListView.setAdapter(videoAdapter);
		
		ImageView miniIcon = findViewById(R.id.mini_icon);
		miniIcon.setImageResource(R.drawable.ic_all);
		miniIcon.setImageLevel(5);
		
		ImageView miniPlayPause = findViewById(R.id.mini_play_pause);
		miniPlayPause.setImageResource(R.drawable.ic_all);
		miniPlayPause.setImageLevel(0);
		
		ImageView playerBack = findViewById(R.id.player_btn_back);
		playerBack.setImageResource(R.drawable.ic_all);
		playerBack.setImageLevel(7);
		
		ImageView playerArt = findViewById(R.id.player_album_art);
		playerArt.setImageResource(R.drawable.ic_all);
		playerArt.setImageLevel(5);
		
		ImageView playerPrev = findViewById(R.id.player_btn_prev);
		playerPrev.setImageResource(R.drawable.ic_all);
		playerPrev.setImageLevel(9);
		
		ImageView playerPlayPause = findViewById(R.id.player_btn_play_pause);
		playerPlayPause.setImageResource(R.drawable.ic_all);
		playerPlayPause.setImageLevel(0);
		
		ImageView playerNext = findViewById(R.id.player_btn_next);
		playerNext.setImageResource(R.drawable.ic_all);
		playerNext.setImageLevel(10);
		
		ImageView converterBack = findViewById(R.id.converter_btn_back);
		converterBack.setImageResource(R.drawable.ic_all);
		converterBack.setImageLevel(7);
		
		TextView converterHeaderTitle = findViewById(R.id.converter_header_title);
		converterHeaderTitle.setText("Video Converter");
	}
	
	protected void setupListeners() {
		findViewById(R.id.btn_toggle_theme).setOnClickListener(v -> {
			isDarkMode = !isDarkMode;
			sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply();
			applyTheme(findViewById(R.id.main_root_layout));
		});
		
		findViewById(R.id.btn_convert_video).setOnClickListener(v -> {
			showOverlay(findViewById(R.id.video_converter_overlay));
		});
		
		findViewById(R.id.converter_btn_back).setOnClickListener(v -> {
			hideOverlay(findViewById(R.id.video_converter_overlay));
		});
		
		findViewById(R.id.player_btn_back).setOnClickListener(v -> {
			hideOverlay(findViewById(R.id.full_player_overlay));
		});
		
		findViewById(R.id.mini_player_bar).setOnClickListener(v -> {
			showOverlay(findViewById(R.id.full_player_overlay));
		});
		
		((ListView) findViewById(R.id.music_list_view)).setOnItemClickListener((parent, view, position, id) -> {
			playSong(songsList.get(position));
		});
		
		((ListView) findViewById(R.id.video_list_view)).setOnItemClickListener((parent, view, position, id) -> {
			showConvertConfirmDialog(videosList.get(position));
		});
		
		findViewById(R.id.mini_play_pause).setOnClickListener(v -> togglePlayPause());
		findViewById(R.id.player_btn_play_pause).setOnClickListener(v -> togglePlayPause());
		
		findViewById(R.id.player_btn_next).setOnClickListener(v -> playNextSong());
		findViewById(R.id.player_btn_prev).setOnClickListener(v -> playPrevSong());
		
		SeekBar playerSeekBar = findViewById(R.id.player_seekbar);
		playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && mediaPlayer != null) {
					mediaPlayer.seekTo(progress);
					TextView timeCur = findViewById(R.id.player_time_current);
					timeCur.setText(formatDuration(progress));
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}
	
	@Override
	public void onBackPressed() {
		View playerOverlay = findViewById(R.id.full_player_overlay);
		View converterOverlay = findViewById(R.id.video_converter_overlay);
		
		if (playerOverlay != null && playerOverlay.getVisibility() == View.VISIBLE) {
			hideOverlay(playerOverlay);
			} else if (converterOverlay != null && converterOverlay.getVisibility() == View.VISIBLE) {
			hideOverlay(converterOverlay);
			} else {
			super.onBackPressed();
		}
	}
	
	protected boolean checkPermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			return checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == android.content.pm.PackageManager.PERMISSION_GRANTED &&
			checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == android.content.pm.PackageManager.PERMISSION_GRANTED;
			} else {
			return checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == android.content.pm.PackageManager.PERMISSION_GRANTED &&
			checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == android.content.pm.PackageManager.PERMISSION_GRANTED;
		}
	}
	
	protected void requestPermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			requestPermissions(new String[]{
				"android.permission.READ_MEDIA_AUDIO",
				"android.permission.READ_MEDIA_VIDEO"
			}, 101);
			} else {
			requestPermissions(new String[]{
				"android.permission.READ_EXTERNAL_STORAGE",
				"android.permission.WRITE_EXTERNAL_STORAGE"
			}, 101);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 101) {
			boolean granted = true;
			for (int res : grantResults) {
				if (res != android.content.pm.PackageManager.PERMISSION_GRANTED) {
					granted = false;
					break;
				}
			}
			if (granted) {
				loadMusic();
				loadVideos();
				} else {
				Toast.makeText(this, "Permissions are required", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	protected void loadMusic() {
		songsList.clear();
		ContentResolver contentResolver = getContentResolver();
		Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
		String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
		Cursor cursor = contentResolver.query(musicUri, null, selection, null, sortOrder);
		if (cursor != null && cursor.moveToFirst()) {
			int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
			int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
			int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
			do {
				long id = cursor.getLong(idCol);
				String title = cursor.getString(titleCol);
				String artist = cursor.getString(artistCol);
				String data = cursor.getString(dataCol);
				long duration = cursor.getLong(durationCol);
				songsList.add(new Song(id, title, artist, data, duration));
			} while (cursor.moveToNext());
			cursor.close();
		}
		if (songsList.isEmpty()) {
			findViewById(R.id.empty_state_text).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.empty_state_text)).setText("No audio tracks found.");
			} else {
			findViewById(R.id.empty_state_text).setVisibility(View.GONE);
		}
		musicAdapter.notifyDataSetChanged();
	}
	
	protected void loadVideos() {
		videosList.clear();
		ContentResolver contentResolver = getContentResolver();
		Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		String sortOrder = MediaStore.Video.Media.TITLE + " ASC";
		Cursor cursor = contentResolver.query(videoUri, null, null, null, sortOrder);
		if (cursor != null && cursor.moveToFirst()) {
			int titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
			int dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
			int durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
			int idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID);
			do {
				long id = cursor.getLong(idCol);
				String title = cursor.getString(titleCol);
				String data = cursor.getString(dataCol);
				long duration = cursor.getLong(durationCol);
				videosList.add(new Video(id, title, data, duration));
			} while (cursor.moveToNext());
			cursor.close();
		}
		if (videosList.isEmpty()) {
			findViewById(R.id.converter_empty_text).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.converter_empty_text)).setText("No video files found.");
			} else {
			findViewById(R.id.converter_empty_text).setVisibility(View.GONE);
		}
		videoAdapter.notifyDataSetChanged();
	}
	
	protected void playSong(Song song) {
		try {
			currentSongIndex = songsList.indexOf(song);
			mediaPlayer.reset();
			mediaPlayer.setDataSource(song.path);
			mediaPlayer.prepare();
			mediaPlayer.start();
			isPlaying = true;
			
			updatePlayerUI(song);
			
			mediaPlayer.setOnCompletionListener(mp -> playNextSong());
			} catch (Exception e) {
			Toast.makeText(this, "Error playing audio file", Toast.LENGTH_SHORT).show();
		}
	}
	
	protected void playNextSong() {
		if (songsList.isEmpty()) return;
		currentSongIndex = (currentSongIndex + 1) % songsList.size();
		playSong(songsList.get(currentSongIndex));
	}
	
	protected void playPrevSong() {
		if (songsList.isEmpty()) return;
		currentSongIndex = (currentSongIndex - 1 + songsList.size()) % songsList.size();
		playSong(songsList.get(currentSongIndex));
	}
	
	protected void togglePlayPause() {
		if (mediaPlayer == null || songsList.isEmpty()) return;
		ImageView miniPlayPause = findViewById(R.id.mini_play_pause);
		ImageView playerPlayPause = findViewById(R.id.player_btn_play_pause);
		
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPlaying = false;
			miniPlayPause.setImageLevel(0);
			playerPlayPause.setImageLevel(0);
			stopAlbumArtRotation();
			} else {
			mediaPlayer.start();
			isPlaying = true;
			miniPlayPause.setImageLevel(1);
			playerPlayPause.setImageLevel(1);
			startAlbumArtRotation();
		}
	}
	
	protected void updatePlayerUI(Song song) {
		findViewById(R.id.mini_player_bar).setVisibility(View.VISIBLE);
		
		TextView miniTitle = findViewById(R.id.mini_title);
		TextView miniArtist = findViewById(R.id.mini_artist);
		miniTitle.setText(song.title);
		miniArtist.setText(song.artist);
		
		ImageView miniPlayPause = findViewById(R.id.mini_play_pause);
		miniPlayPause.setImageLevel(1);
		
		TextView playerTitle = findViewById(R.id.player_song_title);
		TextView playerArtist = findViewById(R.id.player_song_artist);
		playerTitle.setText(song.title);
		playerArtist.setText(song.artist);
		
		ImageView playerPlayPause = findViewById(R.id.player_btn_play_pause);
		playerPlayPause.setImageLevel(1);
		
		SeekBar playerSeekBar = findViewById(R.id.player_seekbar);
		playerSeekBar.setMax((int) song.duration);
		playerSeekBar.setProgress(0);
		
		TextView timeTotal = findViewById(R.id.player_time_total);
		timeTotal.setText(formatDuration(song.duration));
		
		startSeekBarUpdate();
		startAlbumArtRotation();
	}
	
	protected void startAlbumArtRotation() {
		ImageView albumArt = findViewById(R.id.player_album_art);
		if (albumArt != null) {
			if (rotateAnimation == null) {
				rotateAnimation = new RotateAnimation(
				0, 360,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f
				);
				rotateAnimation.setDuration(15000);
				rotateAnimation.setRepeatCount(Animation.INFINITE);
				rotateAnimation.setInterpolator(new LinearInterpolator());
			}
			albumArt.startAnimation(rotateAnimation);
		}
	}
	
	protected void stopAlbumArtRotation() {
		ImageView albumArt = findViewById(R.id.player_album_art);
		if (albumArt != null) {
			albumArt.clearAnimation();
		}
	}
	
	protected void startSeekBarUpdate() {
		if (seekRunnable == null) {
			seekRunnable = new Runnable() {
				@Override
				public void run() {
					if (mediaPlayer != null && isPlaying) {
						try {
							int currentPos = mediaPlayer.getCurrentPosition();
							SeekBar sb = findViewById(R.id.player_seekbar);
							sb.setProgress(currentPos);
							TextView timeCur = findViewById(R.id.player_time_current);
							timeCur.setText(formatDuration(currentPos));
						} catch (Exception ignored) {}
					}
					seekHandler.postDelayed(this, 1000);
				}
			};
		}
		seekHandler.postDelayed(seekRunnable, 1000);
	}
	
	protected String formatDuration(long durationMs) {
		long sec = (durationMs / 1000) % 60;
		long min = (durationMs / (1000 * 60)) % 60;
		return String.format("%02d:%02d", min, sec);
	}
	
	protected void showSongOptionsDialog(Song song) {
		String[] options = {"Play", "Rename", "Delete"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(song.title);
		builder.setItems(options, (dialog, which) -> {
			if (which == 0) {
				playSong(song);
				} else if (which == 1) {
				showRenameDialog(song);
				} else if (which == 2) {
				showDeleteConfirmDialog(song);
			}
		});
		builder.show();
	}
	
	protected void showRenameDialog(Song song) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Rename Track");
		final EditText input = new EditText(this);
		input.setText(song.title);
		input.setPadding(40, 40, 40, 40);
		builder.setView(input);
		builder.setPositiveButton("Save", (dialog, which) -> {
			String newName = input.getText().toString().trim();
			if (!newName.isEmpty()) {
				renameMediaFile(song, newName);
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}
	
	protected void showDeleteConfirmDialog(Song song) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Delete Track");
		builder.setMessage("Are you sure you want to permanently delete this audio file?");
		builder.setPositiveButton("Delete", (dialog, which) -> deleteMediaFile(song));
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}
	
	protected void showConvertConfirmDialog(Video video) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Extract Audio");
		builder.setMessage("Do you want to extract audio tracks from this video and convert it to MP3 format?");
		builder.setPositiveButton("Extract", (dialog, which) -> convertVideoToMp3(video));
		builder.setNegativeButton("Cancel", null);
		builder.show();
	}
	
	protected void renameMediaFile(Song song, String newName) {
		try {
			File file = new File(song.path);
			File parent = file.getParentFile();
			String extension = "";
			int lastDotIdx = file.getName().lastIndexOf('.');
			if (lastDotIdx > 0) {
				extension = file.getName().substring(lastDotIdx);
			}
			File newFile = new File(parent, newName + extension);
			if (file.renameTo(newFile)) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Audio.Media.DATA, newFile.getAbsolutePath());
				values.put(MediaStore.Audio.Media.DISPLAY_NAME, newFile.getName());
				values.put(MediaStore.Audio.Media.TITLE, newName);
				getContentResolver().update(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				values,
				MediaStore.Audio.Media.DATA + "=?",
				new String[]{song.path}
				);
				loadMusic();
				Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
			}
			} catch (Exception e) {
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	protected void deleteMediaFile(Song song) {
		try {
			File file = new File(song.path);
			if (file.exists()) {
				file.delete();
			}
			getContentResolver().delete(
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
			MediaStore.Audio.Media.DATA + "=?",
			new String[]{song.path}
			);
			loadMusic();
			Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	protected Dialog createCustomProgressDialog(String message) {
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCancelable(false);
		
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setGravity(android.view.Gravity.CENTER);
		int padding = (int) (32 * getResources().getDisplayMetrics().density);
		container.setPadding(padding, padding, padding, padding);
		
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(isDarkMode ? 0xFF252525 : 0xFFFFFFFF);
		bg.setCornerRadius(40);
		bg.setStroke(2, 0xFFFF4081);
		container.setBackground(bg);
		
		ProgressBar progressBar = new ProgressBar(this);
		progressBar.getIndeterminateDrawable().setColorFilter(0xFFFF4081, PorterDuff.Mode.SRC_IN);
		LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
		(int) (72 * getResources().getDisplayMetrics().density),
		(int) (72 * getResources().getDisplayMetrics().density)
		);
		container.addView(progressBar, progressParams);
		
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setTextColor(isDarkMode ? 0xFFFFFFFF : 0xFF212121);
		tv.setTextSize(18);
		tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.WRAP_CONTENT,
		LinearLayout.LayoutParams.WRAP_CONTENT
		);
		textParams.setMargins(0, (int) (20 * getResources().getDisplayMetrics().density), 0, 0);
		tv.setGravity(android.view.Gravity.CENTER);
		container.addView(tv, textParams);
		
		dialog.setContentView(container);
		return dialog;
	}
	
	protected void convertVideoToMp3(Video video) {
		Dialog progressDialog = createCustomProgressDialog("Converting... Please wait.");
		progressDialog.show();
		
		new Thread(() -> {
			boolean success = false;
			String outputFilePath = null;
			try {
				File outputDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}
				outputFilePath = new File(outputDirectory, video.title + ".mp3").getAbsolutePath();
				
				MediaExtractor extractor = new MediaExtractor();
				extractor.setDataSource(video.path);
				
				int audioTrackIndex = -1;
				MediaFormat format = null;
				for (int i = 0; i < extractor.getTrackCount(); i++) {
					format = extractor.getTrackFormat(i);
					String mime = format.getString(MediaFormat.KEY_MIME);
					if (mime.startsWith("audio/")) {
						audioTrackIndex = i;
						break;
					}
				}
				
				if (audioTrackIndex >= 0) {
					extractor.selectTrack(audioTrackIndex);
					MediaMuxer muxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
					int writeTrackIndex = muxer.addTrack(format);
					muxer.start();
					
					ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);
					MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
					while (true) {
						bufferInfo.offset = 0;
						bufferInfo.size = extractor.readSampleData(buffer, 0);
						if (bufferInfo.size < 0) {
							bufferInfo.size = 0;
							break;
						}
						bufferInfo.presentationTimeUs = extractor.getSampleTime();
						bufferInfo.flags = extractor.getSampleFlags();
						muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo);
						extractor.advance();
					}
					muxer.stop();
					muxer.release();
					success = true;
				}
				extractor.release();
				} catch (Exception e) {
				success = false;
			}
			
			final boolean finalSuccess = success;
			final String finalPath = outputFilePath;
			runOnUiThread(() -> {
				progressDialog.dismiss();
				if (finalSuccess) {
					Toast.makeText(MainActivityV1.this, "Extraction completed!", Toast.LENGTH_LONG).show();
					android.media.MediaScannerConnection.scanFile(MainActivityV1.this, new String[]{finalPath}, null, null);
					loadMusic();
					hideOverlay(findViewById(R.id.video_converter_overlay));
					} else {
					Toast.makeText(MainActivityV1.this, "Conversion failed", Toast.LENGTH_LONG).show();
				}
			});
		}).start();
	}
	
	protected void showOverlay(View overlay) {
		overlay.setVisibility(View.VISIBLE);
		TranslateAnimation animate = new TranslateAnimation(0, 0, overlay.getHeight() > 0 ? overlay.getHeight() : 2000, 0);
		animate.setDuration(350);
		animate.setFillAfter(true);
		overlay.startAnimation(animate);
	}
	
	protected void hideOverlay(View overlay) {
		TranslateAnimation animate = new TranslateAnimation(0, 0, 0, overlay.getHeight() > 0 ? overlay.getHeight() : 2000);
		animate.setDuration(350);
		animate.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}
			@Override public void onAnimationRepeat(Animation animation) {}
			@Override public void onAnimationEnd(Animation animation) {
				overlay.setVisibility(View.GONE);
			}
		});
		overlay.startAnimation(animate);
	}
	
	protected void applyTheme(View view) {
		if (view == null) return;
		
		int bgColor = isDarkMode ? 0xFF121212 : 0xFFF5F5F5;
		int cardBgColor = isDarkMode ? 0xFF1E1E1E : 0xFFFFFFFF;
		int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF212121;
		int secondaryTextColor = isDarkMode ? 0xFFA0A0A0 : 0xFF757575;
		int accentColor = 0xFFFF4081;
		
		if (view.getId() == R.id.main_root_layout ||
		view.getId() == R.id.full_player_overlay ||
		view.getId() == R.id.video_converter_overlay) {
			view.setBackgroundColor(bgColor);
		} else if (view.getId() == R.id.toolbar_layout ||
		view.getId() == R.id.player_header ||
		view.getId() == R.id.converter_header) {
			view.setBackgroundColor(cardBgColor);
			} else if (view.getId() == R.id.mini_player_bar) {
			GradientDrawable gd = new GradientDrawable();
			gd.setColor(cardBgColor);
			gd.setCornerRadii(new float[]{32, 32, 32, 32, 32, 32, 32, 32});
			gd.setStroke(2, isDarkMode ? 0xFF333333 : 0xFFE0E0E0);
			view.setBackground(gd);
		}
		
		if (view instanceof TextView) {
			TextView tv = (TextView) view;
			int id = tv.getId();
			if (id == R.id.toolbar_title || id == R.id.player_header_title ||
			id == R.id.converter_header_title || id == R.id.player_song_title ||
			id == R.id.item_title || id == R.id.mini_title) {
				tv.setTextColor(primaryTextColor);
				} else {
				tv.setTextColor(secondaryTextColor);
			}
			} else if (view instanceof ImageView) {
			ImageView iv = (ImageView) view;
			if (iv.getId() != R.id.item_icon) {
				iv.setColorFilter(primaryTextColor);
			}
			if (iv.getId() == R.id.player_album_art) {
				GradientDrawable gd = new GradientDrawable();
				gd.setColor(isDarkMode ? 0xFF252525 : 0xFFEAEAEA);
				gd.setShape(GradientDrawable.OVAL);
				gd.setStroke(6, 0xFFFF4081);
				iv.setBackground(gd);
			}
			} else if (view instanceof SeekBar) {
			SeekBar sb = (SeekBar) view;
			sb.getProgressDrawable().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
			sb.getThumb().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
		}
		
		if (view instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++) {
				applyTheme(vg.getChildAt(i));
			}
		}
		
		if (musicAdapter != null) musicAdapter.notifyDataSetChanged();
		if (videoAdapter != null) videoAdapter.notifyDataSetChanged();
	}
	
	protected void showCrashScreen(Throwable throwable) {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setBackgroundColor(0xFF000000);
		layout.setPadding(32, 32, 32, 32);
		
		TextView title = new TextView(this);
		title.setText("FATAL EXCEPTION CRASH HANDLER");
		title.setTextColor(0xFF00FF00);
		title.setTextSize(22);
		title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		layout.addView(title);
		
		TextView subTitle = new TextView(this);
		subTitle.setText("An unexpected error occurred in the application process.");
		subTitle.setTextColor(0xFF00FF00);
		subTitle.setTextSize(14);
		subTitle.setTypeface(Typeface.MONOSPACE);
		subTitle.setPadding(0, 16, 0, 16);
		layout.addView(subTitle);
		
		ScrollView scrollView = new ScrollView(this);
		scrollView.setLayoutParams(new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
		
		TextView errorText = new TextView(this);
		errorText.setTextColor(0xFF00FF00);
		errorText.setTextSize(12);
		errorText.setTypeface(Typeface.MONOSPACE);
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		String stackTrace = sw.toString();
		
		errorText.setText(stackTrace);
		scrollView.addView(errorText);
		layout.addView(scrollView);
		
		setContentView(layout);
	}
	
	protected class MusicAdapter extends BaseAdapter {
		private List<Song> list;
		
		public MusicAdapter(List<Song> list) {
			this.list = list;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}
		
		@Override
		public Object getItem(int position) {
			return list.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivityV1.this).inflate(R.layout.item_music, parent, false);
			}
			Song song = list.get(position);
			TextView title = convertView.findViewById(R.id.item_title);
			TextView subtitle = convertView.findViewById(R.id.item_subtitle);
			ImageView icon = convertView.findViewById(R.id.item_icon);
			ImageView options = convertView.findViewById(R.id.item_options);
			View container = convertView.findViewById(R.id.item_container_layout);
			
			GradientDrawable cardBg = new GradientDrawable();
			cardBg.setColor(isDarkMode ? 0xFF1E1E1E : 0xFFFFFFFF);
			cardBg.setCornerRadius(24);
			cardBg.setStroke(2, isDarkMode ? 0xFF333333 : 0xFFE0E0E0);
			container.setBackground(cardBg);
			
			title.setText(song.title);
			subtitle.setText(song.artist + " • " + formatDuration(song.duration));
			
			icon.setImageResource(R.drawable.ic_all);
			icon.setImageLevel(5);
			
			GradientDrawable iconBg = new GradientDrawable();
			iconBg.setColor(isDarkMode ? 0xFF2D2D2D : 0xFFF0F0F0);
			iconBg.setCornerRadius(20);
			icon.setBackground(iconBg);
			
			int padding = (int) (14 * getResources().getDisplayMetrics().density);
			icon.setPadding(padding, padding, padding, padding);
			
			options.setImageResource(R.drawable.ic_all);
			options.setImageLevel(2);
			
			options.setOnClickListener(v -> showSongOptionsDialog(song));
			
			int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF212121;
			int secondaryTextColor = isDarkMode ? 0xFFA0A0A0 : 0xFF757575;
			title.setTextColor(primaryTextColor);
			subtitle.setTextColor(secondaryTextColor);
			icon.setColorFilter(primaryTextColor);
			options.setColorFilter(primaryTextColor);
			
			return convertView;
		}
	}
	
	protected class VideoAdapter extends BaseAdapter {
		private List<Video> list;
		
		public VideoAdapter(List<Video> list) {
			this.list = list;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}
		
		@Override
		public Object getItem(int position) {
			return list.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivityV1.this).inflate(R.layout.item_music, parent, false);
			}
			Video video = list.get(position);
			TextView title = convertView.findViewById(R.id.item_title);
			TextView subtitle = convertView.findViewById(R.id.item_subtitle);
			ImageView icon = convertView.findViewById(R.id.item_icon);
			ImageView options = convertView.findViewById(R.id.item_options);
			View container = convertView.findViewById(R.id.item_container_layout);
			
			GradientDrawable cardBg = new GradientDrawable();
			cardBg.setColor(isDarkMode ? 0xFF1E1E1E : 0xFFFFFFFF);
			cardBg.setCornerRadius(24);
			cardBg.setStroke(2, isDarkMode ? 0xFF333333 : 0xFFE0E0E0);
			container.setBackground(cardBg);
			
			title.setText(video.title);
			subtitle.setText(formatDuration(video.duration));
			
			icon.setImageBitmap(null);
			icon.setBackgroundColor(Color.TRANSPARENT);
			icon.setPadding(0, 0, 0, 0);
			
			long videoId = video.id;
			Bitmap cachedThumb = thumbnailCache.get(videoId);
			if (cachedThumb != null) {
				icon.setImageBitmap(cachedThumb);
				} else {
				icon.setImageResource(R.drawable.ic_all);
				icon.setImageLevel(6);
				int padding = (int) (14 * getResources().getDisplayMetrics().density);
				icon.setPadding(padding, padding, padding, padding);
				GradientDrawable iconBg = new GradientDrawable();
				iconBg.setColor(isDarkMode ? 0xFF2D2D2D : 0xFFF0F0F0);
				iconBg.setCornerRadius(20);
				icon.setBackground(iconBg);
				
				new Thread(() -> {
					Bitmap thumb = null;
					try {
						MediaMetadataRetriever retriever = new MediaMetadataRetriever();
						retriever.setDataSource(video.path);
						byte[] art = retriever.getEmbeddedPicture();
						if (art != null) {
							thumb = BitmapFactory.decodeByteArray(art, 0, art.length);
							} else {
							thumb = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
						}
						retriever.release();
					} catch (Exception ignored) {}
					
					if (thumb != null) {
						final Bitmap finalThumb = thumb;
						thumbnailCache.put(videoId, thumb);
						runOnUiThread(() -> {
							icon.setBackgroundColor(Color.TRANSPARENT);
							icon.setPadding(0, 0, 0, 0);
							icon.setImageBitmap(finalThumb);
						});
					}
				}).start();
			}
			
			options.setVisibility(View.GONE);
			
			int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF212121;
			int secondaryTextColor = isDarkMode ? 0xFFA0A0A0 : 0xFF757575;
			title.setTextColor(primaryTextColor);
			subtitle.setTextColor(secondaryTextColor);
			if (icon.getDrawable() != null && icon.getBackground() != null) {
				icon.setColorFilter(primaryTextColor);
				} else {
				icon.clearColorFilter();
			}
			
			return convertView;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		seekHandler.removeCallbacks(seekRunnable);
	}
}