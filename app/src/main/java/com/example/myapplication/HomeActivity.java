package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvLogo, tvTripTitle, tvDestinationTitle;
    private Button btnOpenGroup, btnJoinGroup, btnMoreOptions, btnMyLocation;
    private ImageView setting;
    private GoogleMap mMap;
    private String currentGroupId;
    private String uid;

    private static final int REQ_LOCATION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = auth.getUid();

        bindViews();
        setupListeners();
        checkLocationPermission();
        loadCurrentGroup();
    }

    private void bindViews() {
        tvLogo = findViewById(R.id.tvLogo);
        tvTripTitle = findViewById(R.id.tvTripTitle);
        tvDestinationTitle = findViewById(R.id.tvDestinationTitle);
        btnOpenGroup = findViewById(R.id.btnOpenGroup);
        btnJoinGroup = findViewById(R.id.getgrounp);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        setting = findViewById(R.id.setting);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupListeners() {

        btnOpenGroup.setOnClickListener(v ->
                startActivity(new Intent(this, GroupActivity.class)));

        btnJoinGroup.setOnClickListener(v -> showJoinDialog());
        setting.setOnClickListener(v -> {
            startActivity(new Intent(this, settingActivity.class));
        });
        btnMoreOptions.setOnClickListener(v -> {
            if (currentGroupId == null) {
                toast("請先加入群組");
                return;
            }
            startActivity(new Intent(this, ItemDestinationActivity.class));
        });
    }

    private ListenerRegistration userListener;
    private ListenerRegistration groupListener;

    private void loadCurrentGroup() {

        if (uid == null) return;

        userListener = db.collection("users")
                .document(uid)
                .addSnapshotListener((doc, e) -> {

                    if (e != null || doc == null) return;

                    String newGroupId = doc.getString("currentGroupId");

                    if (TextUtils.isEmpty(newGroupId)) {
                        currentGroupId = null;
                        tvTripTitle.setText("尚未加入群組");

                        // remove old group listener
                        if (groupListener != null) groupListener.remove();
                        return;
                    }

                    if (!newGroupId.equals(currentGroupId)) {
                        currentGroupId = newGroupId;
                        listenToGroupName(newGroupId);
                    }
                });
    }
    private void listenToGroupName(String groupId) {

        if (groupListener != null) groupListener.remove();

        groupListener = db.collection("groups")
                .document(groupId)
                .addSnapshotListener((doc, e) -> {

                    if (e != null || doc == null || !doc.exists()) {
                        tvTripTitle.setText("尚未加入群組");
                        return;
                    }

                    String groupName = doc.getString("name");

                    if (!TextUtils.isEmpty(groupName)) {
                        tvTripTitle.setText(groupName);
                    } else {
                        tvTripTitle.setText("未命名群組");
                    }
                });
    }
    private void showJoinDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("輸入群組加入碼");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("加入", (d, w) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!TextUtils.isEmpty(code)) {
                joinGroup(code);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void joinGroup(String code) {

        db.collection("groups")
                .whereEqualTo("joinCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        toast("Group not found");
                        return;
                    }

                    String groupId = snapshot.getDocuments().get(0).getId();

                    DocumentReference groupRef =
                            db.collection("groups").document(groupId);

                    DocumentReference userRef =
                            db.collection("users").document(uid);

                    db.runBatch(batch -> {
                                if (code.equals(currentGroupId)) {
                                    toast("你已經在這個群組");
                                    return;
                                }
                                batch.update(groupRef,
                                        "members",
                                        FieldValue.arrayUnion(uid));

                                Map<String, Object> data = new HashMap<>();
                                data.put("currentGroupId", groupId);

                                // IMPORTANT FIX
                                batch.set(userRef, data, SetOptions.merge());

                            }).addOnSuccessListener(v ->
                                    toast("Joined successfully"))
                            .addOnFailureListener(e ->
                                    toast("Join failed: " + e.getMessage()));
                });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng tokyo = new LatLng(35.681236, 139.767125);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 15f));

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (groupListener != null) groupListener.remove();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                toast("需要定位權限才能顯示你的位置");
            }
        }
    }
}