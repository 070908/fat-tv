package com.fattv.app.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;
import com.fattv.app.ui.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFragment - 首页多行聚合
 * <p>TV 大屏聚合入口：最近播放（横向）+ 每日推荐（横幅）+ 热门歌单（横向卡片）+ 排行榜快捷（横向行）。</p>
 */
public class HomeFragment extends TranslucentFragment {

    // ===== 最近播放 =====
    private RecyclerView rvRecent;
    private TextView tvRecentEmpty;
    private HomeSongAdapter recentAdapter;
    private final List<Song> recentSongs = new ArrayList<>();

    // ===== 每日推荐 =====
    private View bannerDaily;

    // ===== 热门歌单 =====
    private RecyclerView rvPlaylists;
    private TextView tvPlaylistEmpty;
    private HomeCardAdapter playlistAdapter;
    private final List<Song> playlistItems = new ArrayList<>();

    // ===== 排行榜快捷 =====
    private RecyclerView rvRankQuick;
    private TextView tvRankEmpty;
    private HomeSongAdapter rankAdapter;
    private final List<Song> rankSongs = new ArrayList<>();

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // --- 最近播放 ---
        rvRecent = view.findViewById(R.id.rv_recent);
        tvRecentEmpty = view.findViewById(R.id.tv_recent_empty);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new HomeSongAdapter(recentSongs, this::onRecentSongClick);
        rvRecent.setAdapter(recentAdapter);

        // --- 每日推荐 ---
        bannerDaily = view.findViewById(R.id.banner_daily);
        bannerDaily.setOnClickListener(v -> onDailyRecommendClick());

        // --- 热门歌单 ---
        rvPlaylists = view.findViewById(R.id.rv_playlists);
        tvPlaylistEmpty = view.findViewById(R.id.tv_playlist_empty);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        playlistAdapter = new HomeCardAdapter(playlistItems, this::onPlaylistClick);
        rvPlaylists.setAdapter(playlistAdapter);

        // --- 排行榜快捷 ---
        rvRankQuick = view.findViewById(R.id.rv_rank_quick);
        tvRankEmpty = view.findViewById(R.id.tv_rank_empty);
        rvRankQuick.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rankAdapter = new HomeSongAdapter(rankSongs, this::onRankSongClick);
        rvRankQuick.setAdapter(rankAdapter);

        loadAllData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRecentOnly();
    }

    /** 仅刷新最近播放（onResume 时调用，避免重复加载其他行） */
    private void refreshRecentOnly() {
        recentSongs.clear();
        List<Song> history = MusicPlaybackService.getPlayHistory();
        if (history != null) {
            int limit = Math.min(10, history.size());
            for (int i = 0; i < limit; i++) {
                recentSongs.add(history.get(i));
            }
        }
        recentAdapter.notifyDataSetChanged();
        boolean empty = recentSongs.isEmpty();
        tvRecentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvRecent.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** 后台线程加载全部首页数据 */
    private void loadAllData() {
        refreshRecentOnly();

        new Thread(() -> {
            // 等待音源异步初始化完成（MainActivity 后台线程 init 与首页加载存在竞态），最多等 8 秒
            SourceProvider provider = null;
            for (int i = 0; i < 40; i++) {
                SourceProvider p = SourceManager.getInstance()
                        .getProviderByType(SourceProvider.SourceType.PUBLIC);
                if (p instanceof PublicSourceAdapter) {
                    provider = p;
                    break;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
            PublicSourceAdapter publicAdapter = (provider instanceof PublicSourceAdapter)
                    ? (PublicSourceAdapter) provider : null;

            // 1) 热门歌单
            final List<Song> finalPlaylists;
            try {
                finalPlaylists = publicAdapter != null ? publicAdapter.getTopPlaylists(8) : null;
            } catch (Exception ignored) {
                return; // 源异常直接放弃本轮加载
            }

            // 2) 排行榜快捷（取热歌榜前10首）
            final List<Song> finalRank;
            try {
                finalRank = publicAdapter != null ? publicAdapter.getTopSongs(10) : null;
            } catch (Exception ignored) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                // 热门歌单
                playlistItems.clear();
                if (finalPlaylists != null) playlistItems.addAll(finalPlaylists);
                playlistAdapter.notifyDataSetChanged();
                boolean plEmpty = playlistItems.isEmpty();
                tvPlaylistEmpty.setVisibility(plEmpty ? View.VISIBLE : View.GONE);
                rvPlaylists.setVisibility(plEmpty ? View.GONE : View.VISIBLE);

                // 排行榜快捷
                rankSongs.clear();
                if (finalRank != null) rankSongs.addAll(finalRank);
                rankAdapter.notifyDataSetChanged();
                boolean rkEmpty = rankSongs.isEmpty();
                tvRankEmpty.setVisibility(rkEmpty ? View.VISIBLE : View.GONE);
                rvRankQuick.setVisibility(rkEmpty ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    /** 最近播放歌曲点击 */
    private void onRecentSongClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("queue", new ArrayList<>(recentSongs));
        startActivity(intent);
    }

    /** 每日推荐点击：跳转播放排行榜第1首作为推荐 */
    private void onDailyRecommendClick() {
        if (!rankSongs.isEmpty()) {
            Song song = rankSongs.get(0);
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra("song", song);
            intent.putExtra("queue", new ArrayList<>(rankSongs));
            startActivity(intent);
        }
    }

    /** 热门歌单点击：进入歌单详情页 */
    private void onPlaylistClick(Song item) {
        Intent intent = new Intent(requireContext(), com.fattv.app.ui.PlaylistActivity.class);
        intent.putExtra("playlist_id", item.id);
        intent.putExtra("playlist_name", item.title);
        startActivity(intent);
    }

    /** 排行榜快捷歌曲点击 */
    private void onRankSongClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("queue", new ArrayList<>(rankSongs));
        startActivity(intent);
    }

    /** 歌曲横向行适配器（item_home_song） */
    private static class HomeSongAdapter extends RecyclerView.Adapter<HomeSongAdapter.VH> {
        private final List<Song> data;
        private final OnItemClick listener;

        interface OnItemClick {
            void onClick(Song song);
        }

        HomeSongAdapter(List<Song> data, OnItemClick listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_song, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Song s = data.get(position);
            holder.tvTitle.setText(s.title);
            holder.tvArtist.setText(s.artist != null ? s.artist : "");
            if (s.coverUrl != null && !s.coverUrl.isEmpty()) {
                Glide.with(holder.ivCover.getContext())
                        .load(s.coverUrl)
                        .placeholder(R.drawable.placeholder_cover)
                        .error(R.drawable.placeholder_cover)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .centerCrop()
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.placeholder_cover);
            }
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos >= 0 && pos < data.size()) listener.onClick(data.get(pos));
            });
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.08f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView ivCover;
            final TextView tvTitle;
            final TextView tvArtist;

            VH(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvArtist = itemView.findViewById(R.id.tv_artist);
            }
        }
    }

    /** 歌单/专辑竖版卡片适配器（item_home_card） */
    private static class HomeCardAdapter extends RecyclerView.Adapter<HomeCardAdapter.VH> {
        private final List<Song> data;
        private final OnItemClick listener;

        interface OnItemClick {
            void onClick(Song song);
        }

        HomeCardAdapter(List<Song> data, OnItemClick listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Song s = data.get(position);
            holder.tvTitle.setText(s.title);
            holder.tvArtist.setText(s.artist != null ? s.artist : "");
            if (s.coverUrl != null && !s.coverUrl.isEmpty()) {
                Glide.with(holder.ivCover.getContext())
                        .load(s.coverUrl)
                        .placeholder(R.drawable.placeholder_cover)
                        .error(R.drawable.placeholder_cover)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .centerCrop()
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.placeholder_cover);
            }
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos >= 0 && pos < data.size()) listener.onClick(data.get(pos));
            });
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.1f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView ivCover;
            final TextView tvTitle;
            final TextView tvArtist;

            VH(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvArtist = itemView.findViewById(R.id.tv_artist);
            }
        }
    }
}