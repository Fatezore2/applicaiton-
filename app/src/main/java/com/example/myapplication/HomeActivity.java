package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.view.MotionEvent;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;


import org.osmdroid.config.Configuration;

import java.util.*;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI
    private TextView tvLogo, tvTripTitle, tvDestinationTitle;
    private Button btnOpenGroup, btnJoinGroup, btnMoreOptions, btnMyLocation;

    private GoogleMap mMap;
    private View mapOverlay;
    private Marker destinationMarker;
    private final Map<String, Marker> userMarkers = new HashMap<>();

    // Location
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    // Firestore listeners
    private ListenerRegistration destListener, locListener;

    // User & Group
    private String currentGroupId;
    private String username;
    private String uid;


    // Destination coords
    private double destLat = 0, destLng = 0;
    private static final int REQ_LOCATION = 1001;

    // ================= ACTIVITY =================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();

        setupListeners();
        loadUserName();
        checkLocationPermission();
        loadCurrentGroup();
    }

    // ================= BIND UI =================
    private void bindViews() {
        tvLogo = findViewById(R.id.tvLogo);
        tvTripTitle = findViewById(R.id.tvTripTitle);
        tvDestinationTitle = findViewById(R.id.tvDestinationTitle);
        btnOpenGroup = findViewById(R.id.btnOpenGroup);
        btnJoinGroup = findViewById(R.id.getgrounp);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        mapOverlay = findViewById(R.id.map_transparent_overlay);

        // Initialize Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLoc = new LatLng(35.681236, 139.767125);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 15f));
        mapOverlay.setOnTouchListener((v, event) -> {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
            }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }

                    return false;
                });
    }


    // ================= MAP =================

    // ================= USER NAME =================
    private void loadUserName() {
        uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    username = doc.getString("username");
                    tvLogo.setText(username != null ? username.substring(0, 1).toUpperCase() : "U");
                });
    }

    // ================= LOAD GROUP =================
    private void loadCurrentGroup() {
        uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc == null) return;
                    String newGroup = doc.getString("currentGroupId");

                    if (!TextUtils.equals(currentGroupId, newGroup)) {
                        switchGroup(newGroup);
                    }
                });
    }

    private void switchGroup(String newGroupId) {

        if (destListener != null) destListener.remove();
        if (locListener != null) locListener.remove();
        if (locationCallback != null)
            locationClient.removeLocationUpdates(locationCallback);

        currentGroupId = newGroupId;

        // 清除地圖 marker
        if (destinationMarker != null) {
            destinationMarker.remove();
            destinationMarker = null;
        }

        for (Marker m : userMarkers.values()) {
            m.remove();
        }
        userMarkers.clear();

        destLat = 0;
        destLng = 0;

        if (currentGroupId == null) {
            tvTripTitle.setText("尚未加入群組");
            tvDestinationTitle.setText("📍 尚未設定目的地");
            return;
        }

        loadGroupName();
        listenDestinations();
        listenLocations();
        setupLocationUpdates();
    }



    private void loadGroupName() {
        db.collection("groups").document(currentGroupId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    tvTripTitle.setText(name == null ? "群組" : name);
                });
    }

    // ================= BUTTONS =================
    private void setupListeners() {

        btnOpenGroup.setOnClickListener(v ->
                startActivity(new Intent(this, GroupActivity.class)));

        btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());

        btnMoreOptions.setOnClickListener(v -> {
            if (currentGroupId == null) {
                toast("請先加入群組");
                return;
            }
            startActivity(new Intent(this, ItemDestinationActivity.class));
        });

        btnMyLocation.setOnClickListener(v -> moveToMyLocation());
    }

    // ================= JOIN GROUP =================
    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("輸入群組加入碼");

        final EditText input = new EditText(this);
        input.setHint("加入碼");
        builder.setView(input);

        builder.setPositiveButton("加入", (d, w) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!TextUtils.isEmpty(code)) joinGroup(code);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void joinGroup(String code) {
        if (uid == null) return;

        db.collection("groups")
                .whereEqualTo("joinCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        toast("❌ Group not found");
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String groupId = doc.getId();

                    db.runTransaction(t -> {
                                t.update(db.collection("groups").document(groupId),
                                        "members", FieldValue.arrayUnion(uid));
                                t.update(db.collection("users").document(uid),
                                        "currentGroupId", groupId);
                                return null;
                            }).addOnSuccessListener(v -> toast("✅ Joined group"))
                            .addOnFailureListener(e -> toast("❌ Join failed"));
                });
    }

    // ================= DESTINATION =================
    private void listenDestinations() {
        destListener = db.collection("groups")
                .document(currentGroupId)
                .collection("destinations")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || snap.isEmpty()) {
                        tvDestinationTitle.setText("📍 尚未設定目的地");
                        return;
                    }

                    DocumentSnapshot d = snap.getDocuments().get(0);
                    String name = d.getString("name");
                    String time = d.getString("time");
                    destLat = d.getDouble("lat");
                    destLng = d.getDouble("lng");

                    tvDestinationTitle.setText("📍 " + name + " (" + time + ")");

                    if (destinationMarker != null)
                        destinationMarker.remove();

                    LatLng dest = new LatLng(destLat, destLng);

                    destinationMarker = mMap.addMarker(
                            new MarkerOptions()
                                    .position(dest)
                                    .title("🎯 " + name)
                    );

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f));

                });
    }

    // ================= LIVE USER LOCATIONS =================
    private void listenLocations() {
        locListener = db.collection("groups")
                .document(currentGroupId)
                .collection("locations")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    for (com.google.android.gms.maps.model.Marker m : userMarkers.values()) {
                        m.remove();
                    }
                    userMarkers.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        String name = d.getString("senderName");
                        String senderId = d.getString("senderId");

                        if (lat == null || lng == null || senderId == null) continue;

                        LatLng pos = new LatLng(lat, lng);

                        String title = name;

                        if (destLat != 0) {
                            double dist = getDistanceKm(lat, lng, destLat, destLng);
                            String eta = getEta(dist);
                            title = name + " | " +
                                    String.format("%.2f km", dist) +
                                    " | " + eta;
                        }

                        com.google.android.gms.maps.model.Marker marker =
                                mMap.addMarker(
                                        new MarkerOptions()
                                                .position(pos)
                                                .title(title)
                                );

                        userMarkers.put(senderId, marker);
                    }

                });
    }

    // ================= DISTANCE & ETA =================
    private double getDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        return result[0] / 1000.0;
    }

    private String getEta(double distanceKm) {
        double speed = 5.0; // walking km/h
        int min = (int) ((distanceKm / speed) * 60);
        return min + " min";
    }

    // ================= LOCATION PERMISSION =================
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocationUpdates();
        }
    }

    // ================= LOCATION UPDATES =================
    private void setupLocationUpdates() {
        if (currentGroupId == null) return;

        LocationRequest req = LocationRequest.create()
                .setInterval(15000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult res) {
                if (res == null || currentGroupId == null) return;
                Location loc = res.getLastLocation();
                if (loc == null) return;

                Map<String, Object> data = new HashMap<>();
                data.put("senderId", uid);
                data.put("senderName", username == null ? "User" : username);
                data.put("lat", loc.getLatitude());
                data.put("lng", loc.getLongitude());
                data.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("groups")
                        .document(currentGroupId)
                        .collection("locations")
                        .document(uid)
                        .set(data);
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationClient.requestLocationUpdates(req, locationCallback, getMainLooper());
    }

    // ================= MY LOCATION BUTTON =================
    private void moveToMyLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(loc -> {

            if (loc == null || mMap == null) {
                toast("無法取得定位");
                return;
            }

            LatLng p = new LatLng(
                    loc.getLatitude(),
                    loc.getLongitude()
            );

            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(p, 17f)
            );
        });
    }


    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ================= CLEANUP =================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (destListener != null) destListener.remove();
        if (locListener != null) locListener.remove();
        if (locationCallback != null)
            locationClient.removeLocationUpdates(locationCallback);
    }
}
