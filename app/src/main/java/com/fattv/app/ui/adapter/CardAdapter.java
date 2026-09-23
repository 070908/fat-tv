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
import com.fattv.app.ui.MvPlayerActivity;

import java.util.List;

/**
 * CardAdapter - TV 网格卡片适配器（专辑 / MV 列表）
 * 竖版卡片：封面 + 标题 + 艺人，支持焦点动效与点击回调
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    /** 与 SongAdapter 的 viewType 区分，避免同一 RecyclerView 切换适配器时 RecycledViewPool 复用错 ViewHolder */
    private static final int VIEW_TYPE_CARD = 1;

    public interface OnItemClickListener {
        void onItemClick(Song item);
    }

    private final List<Song> items;
    private final OnItemClickListener listener;

    public CardAdapter(List<Song> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_CARD;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvArtist.setText(item.artist != null ? item.artist : "");

        if (item.coverUrl != null && !item.coverUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(item.coverUrl)
                .placeholder(R.drawable.placeholder_cover)
                .error(R.drawable.placeholder_cover)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.placeholder_cover);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1.0f;
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
        }
    }
}