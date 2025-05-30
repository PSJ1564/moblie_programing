package com.example.pineapplegame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText editNickname, editId, editPassword;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editNickname = findViewById(R.id.editNickname);
        editId = findViewById(R.id.editId);
        editPassword = findViewById(R.id.editPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnBack = findViewById(R.id.btnBack);

        dbRef = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(v -> registerUser());

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String nickname = editNickname.getText().toString().trim();
        String id = editId.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (nickname.isEmpty() || id.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean nicknameExists = false;
                boolean idExists = false;

                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (user.nickname.equals(nickname)) nicknameExists = true;
                        if (user.id.equals(id)) idExists = true;
                    }
                }

                if (nicknameExists) {
                    Toast.makeText(this, "닉네임이 이미 존재합니다.", Toast.LENGTH_SHORT).show();
                } else if (idExists) {
                    Toast.makeText(this, "아이디가 이미 존재합니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String userKey = dbRef.push().getKey(); // 고유 키 생성
                    dbRef.child(userKey).setValue(new User(nickname, id, password, 0));
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class User {
        public String nickname;
        public String id;
        public String password;
        public int score;

        public User(String nickname, String id, String password, int score) {
            this.nickname = nickname;
            this.id = id;
            this.password = password;
            this.score = score;
        }
    }
}
