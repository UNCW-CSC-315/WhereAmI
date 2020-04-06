package edu.uncw.whereami;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class LocationRecord {

    private Date timestamp;
    // the Firestore has a built-in "GeoPoint" type which contains latitude and longitude
    private GeoPoint location;
    private double acc;

    // No-argument constructor is required to support conversion of Firestore document to POJO
    public LocationRecord() {
    }

    // All-argument constructor is required to support conversion of Firestore document to POJO
    public LocationRecord(Date timestamp, GeoPoint location, double acc) {
        this.timestamp = timestamp;
        this.location = location;
        this.acc = acc;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public double getAcc() {
        return acc;
    }

    public void setAcc(double acc) {
        this.acc = acc;
    }
}

