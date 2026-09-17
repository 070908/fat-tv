package com.fattv.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;

import java.util.List;

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
        holder.itemView.setOnClickListener(v -> listener.onItemClick(song));
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackgroundColor(hasFocus ? 0x44FFFFFF : 0x00000000);
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
        }
    }
}
