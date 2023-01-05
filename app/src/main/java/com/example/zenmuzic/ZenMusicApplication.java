package com.example.zenmuzic;

import android.app.Application;

public class ZenMusicApplication extends Application {
    private String AUTH_TOKEN;

    public String getAUTH_TOKEN() {
        return AUTH_TOKEN;
    }

    public void setAUTH_TOKEN(String AUTH_TOKEN) {
        this.AUTH_TOKEN = AUTH_TOKEN;
    }

}
