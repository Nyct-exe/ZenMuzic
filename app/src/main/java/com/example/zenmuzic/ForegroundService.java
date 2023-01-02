package com.example.zenmuzic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.zenmuzic.routeRecycleView.AbstractSerializer;
import com.example.zenmuzic.routeRecycleView.Route;
import com.google.android.libraries.places.api.model.Place;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;

public class ForegroundService extends Service {

    public ArrayList<Route> routes;

    // Spotify
    private String AUTH_TOKEN;
    private SpotifyApi spotifyApi;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // GETTING THE AUTH TOKEN
        Bundle extras = intent.getExtras();
        if (extras != null) {
             AUTH_TOKEN = extras.getString("AUTH_TOKEN");
        }

        spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(AUTH_TOKEN)
                .build();

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while(true){
                            Log.d("Foreground","Foreground Service is Running");
                            loadData();
                            if(isSongFinishing(5000)){
                                Log.d("Foreground","Song is Finishing");
                            }
                            // TODO: WRITE LOGIC WHAT HAPPENS IN THE FOREGROUND
                        }
                    }
                }
        ).start();

        final String CHANNEL_ID = "Foreground Service";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this,CHANNEL_ID)
                .setContentText("ZenMuzic is Running")
                .setContentTitle("ZenMuzic");
        startForeground(1001,notification.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = sharedPreferences.getString("routes list", null);
        Type type = new TypeToken<ArrayList<Route>>() {}.getType();
        routes = gson.fromJson(json, type);
        if (routes == null) {
            routes = new ArrayList<>();
        }

    }

    //TODO: Implement this
    private boolean IsUserMoving(){
        return false;
    }

    private boolean isSongFinishing(int milesecondsBeforeEnd){
        GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest =
                spotifyApi.getInformationAboutUsersCurrentPlayback()
                        .build();
        try {
            final CurrentlyPlayingContext currentlyPlayingContext = getInformationAboutUsersCurrentPlaybackRequest.execute();
            Log.d("ForegroundService","Progress: " + currentlyPlayingContext.getProgress_ms());
            if(currentlyPlayingContext.getItem().getDurationMs() - currentlyPlayingContext.getProgress_ms() < milesecondsBeforeEnd)
                return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }

}
