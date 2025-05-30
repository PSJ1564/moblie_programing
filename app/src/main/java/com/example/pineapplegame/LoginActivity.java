package com.example.pineapplegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etId, etPassword;
    private Button btnLogin, btnRegister, btnBack;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etId = findViewById(R.id.etId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        dbRef = FirebaseDatabase.getInstance().getReference("users");

        btnLogin.setOnClickListener(v -> {
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(id, password);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

    }

    private void loginUser(String id, String password) {
        // users 경로 아래 모든 유저 데이터를 읽어서 id, password 일치 여부 확인
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean foundUser = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String dbId = userSnapshot.child("id").getValue(String.class);
                    String dbPassword = userSnapshot.child("password").getValue(String.class);

                    if (dbId != null && dbPassword != null) {
                        if (dbId.equals(id) && dbPassword.equals(password)) {
                            foundUser = true;
                            // 로그인 성공 시, 닉네임도 받아오기
                            String nickname = userSnapshot.child("nickname").getValue(String.class);

                            Toast.makeText(LoginActivity.this, nickname + "님, 환영합니다!", Toast.LENGTH_SHORT).show();

                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", nickname);
                            editor.putString("nickname", nickname);// 로그인한 닉네임 넣음
                            editor.apply();

                            // 로그인 성공 후 MainActivity로 이동
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("nickname", nickname);  // 필요하면 닉네임 전달
                            startActivity(intent);
                            finish();
                            break;
                        }
                    }
                }

                if (!foundUser) {
                    Toast.makeText(LoginActivity.this, "아이디 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "로그인 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
