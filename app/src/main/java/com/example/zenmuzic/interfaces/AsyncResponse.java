package com.example.zenmuzic.interfaces;


import android.location.Location;

public interface AsyncResponse {
    void processFinish(String result);
    void getLocation(Location location);
}
