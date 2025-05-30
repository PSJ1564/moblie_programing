package com.example.pineapplegame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> rankingList = new ArrayList<>();

    static class RankingEntry {
        String nickname;
        int score;

        RankingEntry(String nickname, int score) {
            this.nickname = nickname;
            this.score = score;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        Button btnBack = findViewById(R.id.btnBackToMain);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(RankingActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // 현재 액티비티 종료
        });

        listView = findViewById(R.id.rankingListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rankingList);
        listView.setAdapter(adapter);

        loadRankings();
    }

    private void loadRankings() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("rankings");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RankingEntry> entries = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("nickname").getValue(String.class);
                    Integer score = child.child("score").getValue(Integer.class);

                    if (name != null && score != null) {
                        entries.add(new RankingEntry(name, score));
                    }
                }

                // 점수 내림차순 정렬
                Collections.sort(entries, (a, b) -> b.score - a.score);

                rankingList.clear();
                for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                    RankingEntry entry = entries.get(i);
                    rankingList.add((i + 1) + ". " + entry.nickname + " - " + entry.score);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RankingActivity.this, "랭킹 로딩 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
