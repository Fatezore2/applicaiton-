package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText etName, etEmail, etPassword;
    private TextView tvError;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        tvError = findViewById(R.id.tvError);
        btnCreate = findViewById(R.id.btncreate);

        tvError.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnCreate.setOnClickListener(v -> handleRegister());
    }

    // =============================
    // Register Logic
    // =============================
    private void handleRegister() {

        String username = etName.getText() != null
                ? etName.getText().toString().trim() : "";

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username)) {
            showError("請輸入使用者名稱");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            showError("請輸入電子郵件");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("電子郵件格式不正確");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            showError("密碼至少需要 6 個字元");
            return;
        }

        tvError.setVisibility(View.GONE);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        showError("註冊失敗，帳號可能已存在");
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    // 🔥 Save username to Firestore
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", username);
                    user.put("email", email);
                    user.put("createdAt", System.currentTimeMillis());

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    showError("使用者資料儲存失敗")
                            );
                });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
