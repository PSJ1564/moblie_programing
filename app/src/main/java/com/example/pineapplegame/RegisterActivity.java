package com.example.pineapplegame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText editNickname, editId, editPassword;
    private FirebaseAuth mAuth;
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

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(v -> registerUser());

        // 뒤로가기 버튼 클릭 시
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

        mAuth.createUserWithEmailAndPassword(id, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        dbRef.child(uid).setValue(new User(nickname, id, 0)); // 초기 점수 0
                        Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        finish(); // 회원가입 후 로그인 화면 등으로 이동
                    } else {
                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class User {
        public String nickname;
        public String id;
        public int score;

        public User() {} // Firebase용 빈 생성자

        public User(String nickname, String id, int score) {
            this.nickname = nickname;
            this.id = id;
            this.score = score;
        }
    }
}

