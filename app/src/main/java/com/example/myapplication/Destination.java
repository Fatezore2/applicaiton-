package com.example.myapplication;

public class Destination {

    public String name;
    public double lat;
    public double lng;
    public String time;
    public String createdBy;

    public Destination() {}

    public Destination(String name, double lat, double lng, String time, String createdBy) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.createdBy = createdBy;
    }
}
