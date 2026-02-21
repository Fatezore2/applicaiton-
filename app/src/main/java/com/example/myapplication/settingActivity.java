package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class settingActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;

    private ImageView imgProfile;
    private EditText etUsername;
    private Button btnSaveUsername, btnChangeIcon, btnLogout;
    private TextView tvCountry;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = auth.getUid();

        bindViews();
        loadUserInfo();
        setupListeners();
        displayCountry();
    }

    // ================= Bind UI =================
    private void bindViews() {
        imgProfile = findViewById(R.id.imgProfile);
        etUsername = findViewById(R.id.etUsername);
        btnSaveUsername = findViewById(R.id.btnChangeUsername);
        btnChangeIcon = findViewById(R.id.btnChangeIcon);
        btnLogout = findViewById(R.id.btnLogout);
        tvCountry = findViewById(R.id.tvCountry);
    }

    // ================= Load Current User Data =================
    private void loadUserInfo() {
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String username = doc.getString("username");
                    if (username != null) {
                        etUsername.setText(username);
                    }
                });
    }

    // ================= Setup Click Listeners =================
    private void setupListeners() {

        // Save username
        btnSaveUsername.setOnClickListener(v -> {
            String newName = etUsername.getText().toString().trim();
            if (newName.isEmpty()) {
                toast("Username cannot be empty");
                return;
            }

            if (uid == null) return;

            Map<String, Object> data = new HashMap<>();
            data.put("username", newName);

            db.collection("users")
                    .document(uid)
                    .update(data)
                    .addOnSuccessListener(unused ->
                            toast("Username updated"))
                    .addOnFailureListener(e ->
                            toast("Update failed: " + e.getMessage()));
        });

        // Change profile icon
        btnChangeIcon.setOnClickListener(v -> openGallery());

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(settingActivity.this, MainActivity.class));
            finish();
        });
    }

    // ================= Display Country =================
    private void displayCountry() {
        String country = Locale.getDefault().getDisplayCountry();
        tvCountry.setText(country);
    }

    // ================= Open Gallery =================
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    // ================= Handle Selected Image =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {

            Uri imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(),
                        imageUri
                );

                imgProfile.setImageBitmap(bitmap);

                // NOTE:
                // This only changes local UI.
                // If you want to save image to Firebase Storage,
                // tell me and I’ll add full upload code.

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ================= Toast Helper =================
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}