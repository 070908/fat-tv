package com.fattv.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fattv.app.R;
import com.fattv.app.model.Song;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * SongAdapter - TV 歌曲列表适配器（支持封面图、焦点动效、金色主题）
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private final List<Song> songs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public SongAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.title);
        holder.tvArtist.setText(song.artist);
        holder.tvAlbum.setText(song.album != null ? song.album : "");
        holder.tvDuration.setText(formatDuration(song.duration));

        // 加载封面图
        if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(song.coverUrl)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_logo);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(song));

        // 焦点动效：放大 1.1x + 动画过渡
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1.0f;
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start();
            // 背景色变化由 card_item_focusable drawable 处理
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    /**
     * 格式化毫秒为 mm:ss
     */
    private String formatDuration(long ms) {
        if (ms <= 0) return "00:00";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist, tvAlbum, tvDuration;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvAlbum = itemView.findViewById(R.id.tv_album);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
