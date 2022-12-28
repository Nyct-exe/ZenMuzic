package com.example.zenmuzic.playlistRecyclerView;

import java.io.Serializable;

public class Playlist implements Serializable {

    private boolean isChecked = false;
    private String name;

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
}
