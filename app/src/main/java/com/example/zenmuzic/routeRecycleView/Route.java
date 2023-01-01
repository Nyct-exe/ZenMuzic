package com.example.zenmuzic.routeRecycleView;

import com.example.zenmuzic.playlistRecyclerView.Playlist;
import com.google.android.libraries.places.api.model.Place;

import java.io.Serializable;

public class Route implements Serializable {
    private String name = "";

    private Playlist playlist;

    private Place startingPoint;

    private Place endPoint;

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

    public Place getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(Place startingPoint) {
        this.startingPoint = startingPoint;
    }

    public Place getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Place endPoint) {
        this.endPoint = endPoint;
    }


}
