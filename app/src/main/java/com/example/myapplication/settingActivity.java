package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private String uid;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance("gs://project-221c4.firebasestorage.app");
        storageRef = storage.getReference();

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

    // ================= Load User Info =================
    private void loadUserInfo() {
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String username = doc.getString("username");
                    String imageUrl = doc.getString("profileImageUrl");

                    if (username != null) {
                        etUsername.setText(username);
                    }

                    if (imageUrl != null) {
                        Glide.with(this)
                                .load(imageUrl)
                                .into(imgProfile);
                    }
                });
    }

    // ================= Listeners =================
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
                            toast("Update failed"));
        });

        // Pick Image
        btnChangeIcon.setOnClickListener(v -> openGallery());

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // ================= Open Gallery =================
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    // ================= Handle Image Result =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE &&
                resultCode == Activity.RESULT_OK &&
                data != null) {

            selectedImageUri = data.getData();

            imgProfile.setImageURI(selectedImageUri);

            uploadProfileImage(); // Upload immediately
        }
    }

    // ================= Upload Image =================
    private void uploadProfileImage() {

        if (selectedImageUri == null || uid == null) return;

        StorageReference profileRef =
                storageRef.child("profile_images/" + uid + ".jpg");

        profileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        profileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {

                                    String imageUrl = uri.toString();

                                    Map<String, Object> data = new HashMap<>();
                                    data.put("profileImageUrl", imageUrl);

                                    db.collection("users")
                                            .document(uid)
                                            .update(data)
                                            .addOnSuccessListener(unused ->
                                                    toast("Profile image updated"));
                                }))
                .addOnFailureListener(e ->
                        toast("Upload failed"));
    }

    // ================= Country =================
    private void displayCountry() {
        String country = Locale.getDefault().getDisplayCountry();
        tvCountry.setText(country);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}