package com.example.zenmuzic;

import android.app.Application;

import com.example.zenmuzic.routeRecycleView.Route;

public class ZenMusicApplication extends Application {
    private String AUTH_TOKEN;
    private boolean LOCATION_PERMISSION;

    private Route route;

    public String getAUTH_TOKEN() {
        return AUTH_TOKEN;
    }

    public void setAUTH_TOKEN(String AUTH_TOKEN) {
        this.AUTH_TOKEN = AUTH_TOKEN;
    }

    public boolean isLOCATION_PERMISSION() {
        return LOCATION_PERMISSION;
    }

    public void setLOCATION_PERMISSION(boolean LOCATION_PERMISSION) {
        this.LOCATION_PERMISSION = LOCATION_PERMISSION;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
