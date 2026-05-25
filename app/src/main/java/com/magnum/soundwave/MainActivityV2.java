package com.magnum.soundwave;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivityV2 extends MainActivityV1 {
	
	private final int[] avatarColors = {
		0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1,
		0xFF3949AB, 0xFF1E88E5, 0xFF00897B, 0xFF43A047,
		0xFFFF8F00, 0xFFF4511E, 0xFF6D4C41, 0xFF546E7A
	};
	
	@Override
	protected void initViews() {
		super.initViews();
		
		ListView musicListView = findViewById(R.id.music_list_view);
		musicAdapter = new MusicAdapterV2();
		musicListView.setAdapter(musicAdapter);
		
		ListView videoListView = findViewById(R.id.video_list_view);
		videoAdapter = new VideoAdapterV2();
		videoListView.setAdapter(videoAdapter);
	}
	
	@Override
	protected void applyTheme(View view) {
		if (view == null) return;
		
		int bgColor = isDarkMode ? 0xFF0F111A : 0xFFF4F7F9;
		int cardBgColor = isDarkMode ? 0xFF1A1D29 : 0xFFFFFFFF;
		int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF1A1A1A;
		int secondaryTextColor = isDarkMode ? 0xFF8A94A6 : 0xFF757575;
		int accentColor = 0xFF00E676;
		
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
			gd.setCornerRadii(new float[]{36, 36, 36, 36, 36, 36, 36, 36});
			gd.setStroke(2, isDarkMode ? 0xFF2A2E3D : 0xFFE2E8F0);
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
				gd.setColor(isDarkMode ? 0xFF1A1D29 : 0xFFEAEAEA);
				gd.setShape(GradientDrawable.OVAL);
				gd.setStroke(8, accentColor);
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
	
	private class MusicAdapterV2 extends MusicAdapter {
		public MusicAdapterV2() {
			super(songsList);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivityV2.this).inflate(R.layout.item_music, parent, false);
			}
			Song song = songsList.get(position);
			TextView title = convertView.findViewById(R.id.item_title);
			TextView subtitle = convertView.findViewById(R.id.item_subtitle);
			ImageView icon = convertView.findViewById(R.id.item_icon);
			ImageView options = convertView.findViewById(R.id.item_options);
			View container = convertView.findViewById(R.id.item_container_layout);
			
			GradientDrawable cardBg = new GradientDrawable();
			cardBg.setColor(isDarkMode ? 0xFF1A1D29 : 0xFFFFFFFF);
			cardBg.setCornerRadius(28);
			cardBg.setStroke(2, isDarkMode ? 0xFF2A2E3D : 0xFFE2E8F0);
			container.setBackground(cardBg);
			
			title.setText(song.title);
			subtitle.setText(song.artist + " • " + formatDuration(song.duration));
			
			int colorIndex = position % avatarColors.length;
			Bitmap numberBitmap = createNumberBitmap(position + 1, avatarColors[colorIndex]);
			icon.setImageBitmap(numberBitmap);
			icon.setBackgroundColor(Color.TRANSPARENT);
			icon.setPadding(0, 0, 0, 0);
			icon.clearColorFilter();
			
			options.setImageResource(R.drawable.ic_all);
			options.setImageLevel(2);
			options.setOnClickListener(v -> showSongOptionsDialog(song));
			
			int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF1A1A1A;
			int secondaryTextColor = isDarkMode ? 0xFF8A94A6 : 0xFF757575;
			title.setTextColor(primaryTextColor);
			subtitle.setTextColor(secondaryTextColor);
			options.setColorFilter(primaryTextColor);
			
			return convertView;
		}
	}
	
	private class VideoAdapterV2 extends VideoAdapter {
		public VideoAdapterV2() {
			super(videosList);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivityV2.this).inflate(R.layout.item_music, parent, false);
			}
			Video video = videosList.get(position);
			TextView title = convertView.findViewById(R.id.item_title);
			TextView subtitle = convertView.findViewById(R.id.item_subtitle);
			ImageView icon = convertView.findViewById(R.id.item_icon);
			ImageView options = convertView.findViewById(R.id.item_options);
			View container = convertView.findViewById(R.id.item_container_layout);
			
			GradientDrawable cardBg = new GradientDrawable();
			cardBg.setColor(isDarkMode ? 0xFF1A1D29 : 0xFFFFFFFF);
			cardBg.setCornerRadius(28);
			cardBg.setStroke(2, isDarkMode ? 0xFF2A2E3D : 0xFFE2E8F0);
			container.setBackground(cardBg);
			
			title.setText(video.title);
			subtitle.setText(formatDuration(video.duration));
			
			long videoId = video.id;
			Bitmap cachedThumb = thumbnailCache.get(videoId);
			
			if (cachedThumb != null) {
				icon.setPadding(0, 0, 0, 0);
				icon.setBackgroundColor(Color.TRANSPARENT);
				icon.setImageBitmap(cachedThumb);
				icon.clearColorFilter();
				} else {
				icon.setImageResource(R.drawable.ic_all);
				icon.setImageLevel(6);
				int padding = (int) (16 * getResources().getDisplayMetrics().density);
				icon.setPadding(padding, padding, padding, padding);
				
				GradientDrawable iconBg = new GradientDrawable();
				iconBg.setColor(isDarkMode ? 0xFF2A2E3D : 0xFFE2E8F0);
				iconBg.setCornerRadius(20);
				icon.setBackground(iconBg);
				
				int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF1A1A1A;
				icon.setColorFilter(primaryTextColor);
				
				new Thread(() -> {
					Bitmap thumb = null;
					try {
						thumb = ThumbnailUtils.createVideoThumbnail(video.path, MediaStore.Video.Thumbnails.MINI_KIND);
					} catch (Exception ignored) {}
					
					if (thumb != null) {
						final Bitmap finalThumb = thumb;
						thumbnailCache.put(videoId, thumb);
						runOnUiThread(() -> {
							icon.setPadding(0, 0, 0, 0);
							icon.setBackgroundColor(Color.TRANSPARENT);
							icon.setImageBitmap(finalThumb);
							icon.clearColorFilter();
						});
					}
				}).start();
			}
			
			options.setVisibility(View.GONE);
			
			int primaryTextColor = isDarkMode ? 0xFFFFFFFF : 0xFF1A1A1A;
			int secondaryTextColor = isDarkMode ? 0xFF8A94A6 : 0xFF757575;
			title.setTextColor(primaryTextColor);
			subtitle.setTextColor(secondaryTextColor);
			
			return convertView;
		}
	}
}