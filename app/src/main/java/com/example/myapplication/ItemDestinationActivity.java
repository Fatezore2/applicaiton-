package com.example.myapplication;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.*;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ItemDestinationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;
    private static final int LOCATION_PERMISSION_CODE = 200;

    private EditText etCity;
    private TextView tvDateTime;
    private Button btnSave, btnDelete;

    private GoogleMap mMap;
    private Marker marker;

    private double pickedLat;
    private double pickedLng;
    private boolean locationSelected = false;

    private Calendar selectedDateTime;

    private String destinationId;
    private String groupId;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private FusedLocationProviderClient fusedLocationClient;

    private SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_destination);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        selectedDateTime = Calendar.getInstance();
        destinationId = getIntent().getStringExtra("destinationId");

        initViews();
        loadGroupId();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.pickMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {

        etCity = findViewById(R.id.etCity);
        tvDateTime = findViewById(R.id.tvDateTime);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        etCity.setFocusable(false);
        etCity.setClickable(true);
        etCity.setCursorVisible(false);

        etCity.setOnClickListener(v -> startAutocomplete());

        tvDateTime.setText(dateFormat.format(selectedDateTime.getTime()));

        setupDatePicker();

        btnSave.setOnClickListener(v -> saveDestination());

        btnDelete.setOnClickListener(v -> deleteDestination());
    }

    private void loadGroupId() {
        String uid = auth.getUid();
        if (uid == null) return;

        Log.d("DEBUG", "Checking membership for UID: " + uid);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    groupId = doc.getString("currentGroupId");
                    Log.d("DEBUG", "Loaded GroupID: " + groupId);

                    // 確認成員路徑是否真的存在
                    db.collection("groups").document(groupId)
                            .collection("members").document(uid).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult().exists()) {
                                    Log.d("DEBUG", "Membership confirmed.");
                                    if (destinationId != null) loadDestination();
                                } else {
                                    Log.e("DEBUG", "User is NOT a member of this group!");
                                }
                            });
                });
    }

    private void loadDestination() {

        if (groupId == null) return;

        btnDelete.setVisibility(View.VISIBLE);

        db.collection("groups")
                .document(groupId)
                .collection("destinations")
                .document(destinationId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    etCity.setText(doc.getString("city"));

                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");

                    if (lat != null && lng != null) {

                        pickedLat = lat;
                        pickedLng = lng;
                        locationSelected = true;

                        if (mMap != null) {
                            updateMarker(new LatLng(pickedLat, pickedLng));
                        }
                    }

                    Date date = doc.getDate("dateTime");

                    if (date != null) {
                        selectedDateTime.setTime(date);
                        tvDateTime.setText(dateFormat.format(date));
                    }
                });
    }

    private void saveDestination() {
        String city = etCity.getText().toString().trim();

        // 1. 確保使用者已登入
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupId == null || city.isEmpty() || !locationSelected) {
            Toast.makeText(this, "請選擇正確的地點與資訊", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("city", city);
        data.put("lat", pickedLat);
        data.put("lng", pickedLng);
        data.put("dateTime", selectedDateTime.getTime());
        data.put("createdBy", auth.getCurrentUser().getUid());

        // 2. 使用寫入批次或直接寫入
        db.collection("groups").document(groupId)
                .collection("destinations")
                .document(destinationId == null ? UUID.randomUUID().toString() : destinationId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "儲存成功", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("SAVE_ERROR", "寫入失敗: " + e.getMessage());
                    Toast.makeText(this, "權限不足或發生錯誤", Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteDestination() {

        if (destinationId == null || groupId == null) return;

        db.collection("groups")
                .document(groupId)
                .collection("destinations")
                .document(destinationId)
                .delete();

        finish();
    }

    private void setupDatePicker() {

        tvDateTime.setOnClickListener(v -> {

            Calendar now = Calendar.getInstance();

            new DatePickerDialog(this,
                    (view, year, month, day) -> {

                        new TimePickerDialog(this,
                                (timeView, hour, minute) -> {

                                    selectedDateTime.set(year, month, day, hour, minute);

                                    tvDateTime.setText(
                                            dateFormat.format(selectedDateTime.getTime()));

                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true).show();

                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void startAutocomplete() {

        List<Place.Field> fields =
                Arrays.asList(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG
                );

        Intent intent =
                new Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY, fields)
                        .setHint("Search destination...")
                        .build(this);

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {

                Place place = Autocomplete.getPlaceFromIntent(data);

                LatLng latLng = place.getLatLng();

                if (latLng != null) {

                    pickedLat = latLng.latitude;
                    pickedLng = latLng.longitude;
                    locationSelected = true;

                    updateMarker(latLng);

                    // ⭐ 用 Geocoder 取得真正的城市名稱
                    fillCityFromLocation(pickedLat, pickedLng);
                }

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {

                Status status = Autocomplete.getStatusFromIntent(data);

                Toast.makeText(
                        this,
                        status.getStatusMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
    private void updateMarker(LatLng latLng) {

        if (mMap == null) return;

        if (marker != null) marker.remove();

        marker = mMap.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title(etCity.getText().toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE)));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void fillCityFromLocation(double lat, double lng) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {

            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                String city = address.getLocality();

                if (city == null) city = address.getFeatureName();

                etCity.setText(city);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return;
        }

        enableLocation();
    }

    private void enableLocation() {

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null && destinationId == null) {

                        LatLng current =
                                new LatLng(location.getLatitude(),
                                        location.getLongitude());

                        mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(current, 14));
                    }
                });

        mMap.setOnMapClickListener(latLng -> {

            pickedLat = latLng.latitude;
            pickedLng = latLng.longitude;
            locationSelected = true;

            updateMarker(latLng);

            fillCityFromLocation(pickedLat, pickedLng);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enableLocation();
        }
    }
}