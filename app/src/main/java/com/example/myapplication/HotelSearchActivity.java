// HotelSearchActivity.java
package com.example.myapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;

public class HotelSearchActivity extends AppCompatActivity {

    private EditText etDestination;
    private DatePicker datePickerCheckIn, datePickerCheckOut;
    private Button btnSearch;

    // 人數相關變數
    private TextView tvAdultCount, tvChildCount, tvRoomCount;
    private Button btnAdultMinus, btnAdultPlus, btnChildMinus, btnChildPlus;
    private Button btnRoomMinus, btnRoomPlus;
    private int adultCount = 2;
    private int childCount = 0;
    private int roomCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_search);

        initViews();
        setupDefaultDates();
        setupGuestControls();

        btnSearch.setOnClickListener(v -> performHotelSearch());
    }

    private void initViews() {
        etDestination = findViewById(R.id.etDestination);
        datePickerCheckIn = findViewById(R.id.datePickerCheckIn);
        datePickerCheckOut = findViewById(R.id.datePickerCheckOut);
        btnSearch = findViewById(R.id.btnSearch);

        // 人數顯示
        tvAdultCount = findViewById(R.id.tvAdultCount);
        tvChildCount = findViewById(R.id.tvChildCount);
        tvRoomCount = findViewById(R.id.tvRoomCount);

        // 人數控制按鈕
        btnAdultMinus = findViewById(R.id.btnAdultMinus);
        btnAdultPlus = findViewById(R.id.btnAdultPlus);
        btnChildMinus = findViewById(R.id.btnChildMinus);
        btnChildPlus = findViewById(R.id.btnChildPlus);
        btnRoomMinus = findViewById(R.id.btnRoomMinus);
        btnRoomPlus = findViewById(R.id.btnRoomPlus);

        // 確保可以輸入中文
        etDestination.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    }

    private void setupGuestControls() {
        btnAdultMinus.setOnClickListener(v -> {
            if (adultCount > 1) {
                adultCount--;
                tvAdultCount.setText(String.valueOf(adultCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        btnAdultPlus.setOnClickListener(v -> {
            if (adultCount < 10) {
                adultCount++;
                tvAdultCount.setText(String.valueOf(adultCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        btnChildMinus.setOnClickListener(v -> {
            if (childCount > 0) {
                childCount--;
                tvChildCount.setText(String.valueOf(childCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        btnChildPlus.setOnClickListener(v -> {
            if (childCount < 10) {
                childCount++;
                tvChildCount.setText(String.valueOf(childCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        btnRoomMinus.setOnClickListener(v -> {
            if (roomCount > 1) {
                roomCount--;
                tvRoomCount.setText(String.valueOf(roomCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        btnRoomPlus.setOnClickListener(v -> {
            if (roomCount < 5) {
                roomCount++;
                tvRoomCount.setText(String.valueOf(roomCount));
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                updateButtonStates();
            }
        });

        updateButtonStates();
    }

    private void updateButtonStates() {
        btnAdultMinus.setEnabled(adultCount > 1);
        btnAdultPlus.setEnabled(adultCount < 10);
        btnChildMinus.setEnabled(childCount > 0);
        btnChildPlus.setEnabled(childCount < 10);
        btnRoomMinus.setEnabled(roomCount > 1);
        btnRoomPlus.setEnabled(roomCount < 5);

        float enabledAlpha = 1.0f;
        float disabledAlpha = 0.5f;

        btnAdultMinus.setAlpha(btnAdultMinus.isEnabled() ? enabledAlpha : disabledAlpha);
        btnAdultPlus.setAlpha(btnAdultPlus.isEnabled() ? enabledAlpha : disabledAlpha);
        btnChildMinus.setAlpha(btnChildMinus.isEnabled() ? enabledAlpha : disabledAlpha);
        btnChildPlus.setAlpha(btnChildPlus.isEnabled() ? enabledAlpha : disabledAlpha);
        btnRoomMinus.setAlpha(btnRoomMinus.isEnabled() ? enabledAlpha : disabledAlpha);
        btnRoomPlus.setAlpha(btnRoomPlus.isEnabled() ? enabledAlpha : disabledAlpha);
    }

    private void setupDefaultDates() {
        Calendar today = Calendar.getInstance();
        datePickerCheckIn.updateDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        datePickerCheckOut.updateDate(
                tomorrow.get(Calendar.YEAR),
                tomorrow.get(Calendar.MONTH),
                tomorrow.get(Calendar.DAY_OF_MONTH)
        );
    }

    private void performHotelSearch() {
        String destination = etDestination.getText().toString().trim();

        if (TextUtils.isEmpty(destination)) {
            Toast.makeText(this, "請輸入目的地", Toast.LENGTH_SHORT).show();
            return;
        }

        String checkIn = getDateFromPicker(datePickerCheckIn);
        String checkOut = getDateFromPicker(datePickerCheckOut);

        if (checkOut.compareTo(checkIn) <= 0) {
            Toast.makeText(this, "退房日期必須晚於入住日期", Toast.LENGTH_SHORT).show();
            return;
        }

        openBooking(destination, checkIn, checkOut);
    }

    private String getDateFromPicker(DatePicker datePicker) {
        int year = datePicker.getYear();
        int month = datePicker.getMonth() + 1;
        int day = datePicker.getDayOfMonth();
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    private void openBooking(String destination, String checkIn, String checkOut) {
        try {
            String encodedDestination = URLEncoder.encode(destination, "UTF-8");

            String bookingUri = "booking://search?" +
                    "destination=" + encodedDestination +
                    "&checkin=" + checkIn +
                    "&checkout=" + checkOut +
                    "&lang=zh-tw" +
                    "&group_adults=" + adultCount +
                    "&group_children=" + childCount +
                    "&no_rooms=" + roomCount;

            Log.d("HotelSearch", "Booking URI: " + bookingUri);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookingUri));
            startActivity(intent);
            logSearchEvent(destination, true);

        } catch (UnsupportedEncodingException e) {
            Log.e("HotelSearch", "編碼錯誤", e);
            showError("無法處理目的地名稱");
        } catch (ActivityNotFoundException e) {
            handleBookingWebFallback(destination, checkIn, checkOut);
        }
    }

    private void handleBookingWebFallback(String destination, String checkIn, String checkOut) {
        try {
            String encodedDestination = URLEncoder.encode(destination, "UTF-8");

            String webUrl = "https://www.booking.com/searchresults.zh-tw.html?" +
                    "ss=" + encodedDestination +
                    "&checkin=" + checkIn +
                    "&checkout=" + checkOut +
                    "&group_adults=" + adultCount +
                    "&group_children=" + childCount +
                    "&no_rooms=" + roomCount;

            Log.d("HotelSearch", "Booking Web Fallback: " + webUrl);

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            startActivity(browserIntent);
            logSearchEvent(destination, false);

        } catch (UnsupportedEncodingException e) {
            Log.e("HotelSearch", "編碼錯誤", e);
            showError("無法處理目的地名稱");
        }
    }

    private void logSearchEvent(String destination, boolean isApp) {
        Log.i("HotelSearch", "搜尋 - 目的地: " + destination +
                ", 人數: " + adultCount + "成人 " + childCount + "兒童" +
                ", 房間: " + roomCount +
                ", 開啟方式: " + (isApp ? "App" : "網頁"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void setDestinationFromMap(LatLng latLng) {
        Toast.makeText(this, "請手動輸入目的地", Toast.LENGTH_SHORT).show();
    }
}