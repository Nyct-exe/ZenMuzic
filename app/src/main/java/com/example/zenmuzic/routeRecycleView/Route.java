package com.example.zenmuzic.routeRecycleView;

import com.example.zenmuzic.playlistRecyclerView.Playlist;

import java.io.Serializable;

public class Route implements Serializable {
    private String name;

    private Playlist playlist;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }


}
