package com.example.zenmuzic.playlistRecyclerView;

import android.os.Parcel;
import android.os.Parcelable;

public class Playlist implements Parcelable {

    private boolean isChecked = false;
    private String name;
    private String uri;

    protected Playlist() {}
    protected Playlist(Parcel in) {
        isChecked = in.readByte() != 0;
        name = in.readString();
        uri = in.readString();
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (isChecked ? 1 : 0));
        parcel.writeString(name);
        parcel.writeString(uri);
    }
}
