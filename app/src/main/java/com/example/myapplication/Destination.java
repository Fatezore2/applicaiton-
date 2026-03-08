package com.example.myapplication;

import java.util.Date;

public class Destination {

    public String city;
    public double lat;
    public double lng;
    public Date dateTime;
    public String createdBy;

    public Destination() {
        // Firestore 必须要空构造
    }

    public Destination(String city, double lat, double lng, Date dateTime, String createdBy) {
        this.city = city;
        this.lat = lat;
        this.lng = lng;
        this.dateTime = dateTime;
        this.createdBy = createdBy;
    }
}