package com.example.zenmuzic.playlistRecyclerView;

import java.io.Serializable;

public class Playlist implements Serializable {

    private boolean isChecked = false;
    private String name;
    private String uri;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
