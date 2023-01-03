package com.example.zenmuzic.routeRecycleView;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.zenmuzic.playlistRecyclerView.Playlist;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;

public class Route implements Parcelable {
    private String name = "";

    private Playlist playlist;

    private Place startingPoint;

    private Place endPoint;

    private ArrayList<LatLng> listOfPoints;

    protected Route() {
        listOfPoints = new ArrayList<LatLng>();
    }

    protected Route(Parcel in) {
        name = in.readString();
        playlist = in.readParcelable(Playlist.class.getClassLoader());
        startingPoint = in.readParcelable(Place.class.getClassLoader());
        endPoint = in.readParcelable(Place.class.getClassLoader());
        listOfPoints = new ArrayList<LatLng>();
        in.readParcelableList(listOfPoints, LatLng.class.getClassLoader());
    }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        @Override
        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

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

    public ArrayList<LatLng> getListOfPoints() {return listOfPoints;}

    public void setListOfPoints(ArrayList<LatLng> listOfPoints) {this.listOfPoints = listOfPoints;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeParcelable(playlist, i);
        parcel.writeParcelable(startingPoint, i);
        parcel.writeParcelable(endPoint, i);
        parcel.writeParcelableList(listOfPoints, i);
    }
}
