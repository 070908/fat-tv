package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.SourceManager;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchActivity - TV 搜索页：DPad 导航 + 搜索结果列表
 */
public class SearchActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView rvResults;
    private SongAdapter adapter;
    private List<Song> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(results, this::onSongClick);
        rvResults.setAdapter(adapter);

        // 搜索框焦点动画：金色边框已通过 input_focusable drawable 处理
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etSearch.setTextColor(getResources().getColor(R.color.accent));
            } else {
                etSearch.setTextColor(getResources().getColor(R.color.text_primary));
            }
        });

        etSearch.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                doSearch();
                // 搜索后自动将焦点移到列表第一项
                rvResults.post(() -> {
                    if (adapter.getItemCount() > 0) {
                        RecyclerView.ViewHolder holder = rvResults.findViewHolderForAdapterPosition(0);
                        if (holder != null) holder.itemView.requestFocus();
                    }
                });
                return true;
            }
            return false;
        });

        etSearch.requestFocus();
    }

    private void doSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) return;

        new Thread(() -> {
            try {
                List<Song> songs = SourceManager.getInstance().getCurrentProvider().search(keyword, 1);
                runOnUiThread(() -> {
                    results.clear();
                    results.addAll(songs);
                    adapter.notifyDataSetChanged();
                    if (!SourceManager.getInstance().isCurrentHealthy()) {
                        // 源失效时自动降级
                        if (SourceManager.getInstance().autoFallback()) {
                            Toast.makeText(this, "\u5df2\u5207\u6362\u5230\u5907\u7528\u97f3\u6e90", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "\u641c\u7d22\u5931\u8d25: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // 弹窗询问是否切换音源（简化版用 Toast）
                });
            }
        }).start();
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }
}
