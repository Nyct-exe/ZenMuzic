package com.example.zenmuzic;

import static android.content.ContentValues.TAG;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zenmuzic.routeRecycleView.AbstractSerializer;
import com.example.zenmuzic.routeRecycleView.Route;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.maps.model.LatLng;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import se.michaelthelin.spotify.requests.data.player.SkipUsersPlaybackToNextTrackRequest;

public class ForegroundService extends Service {

    public ArrayList<Route> routes;

    // Spotify
    private String AUTH_TOKEN;
    private SpotifyApi spotifyApi;

    // The entry point to the Fused Location Provider.
    // GPS
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private double currentUserSpeed;


    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };
        createLocationRequest();
        getCurrentLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // GETTING THE AUTH TOKEN
        Bundle extras = intent.getExtras();
        if (extras != null) {
             AUTH_TOKEN = extras.getString("AUTH_TOKEN");
             locationPermissionGranted = extras.getBoolean("locationGranted");
        }

        spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(AUTH_TOKEN)
                .build();

        new Thread(
                new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        while(true){
                            // TODO: WRITE LOGIC WHAT HAPPENS IN THE FOREGROUND
//                            Log.d("Foreground","Foreground Service is Running");
                            loadData();
//                            if(isSongFinishing(5000)){
//                                Log.d("Foreground","Song is Finishing");
//                                if(getCurrentSpeed() > 0)
//                                    Log.d("Foregorund","Speed:" + getCurrentSpeed());
//                                for(Route r: routes){
//                                    getPolyPath(r);
//                                    if(PolyUtil.isLocationOnPath(getCurrentLocation())){
//
//                                    }
//                                    r.getStartingPoint().get
//                                    PolyUtil.isLocationOnPath()
//                                }
//                            }
                            getCurrentSpeed();
                            Log.d("Foreground","Speed: "+currentUserSpeed);
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

    private boolean isSongFinishing(int milesecondsBeforeEnd){
        GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest =
                spotifyApi.getInformationAboutUsersCurrentPlayback()
                        .build();
        try {
            final CurrentlyPlayingContext currentlyPlayingContext = getInformationAboutUsersCurrentPlaybackRequest.execute();
            if(currentlyPlayingContext != null) {
                Log.d("ForegroundService","Progress: " + currentlyPlayingContext.getProgress_ms());
                if(currentlyPlayingContext.getItem().getDurationMs() - currentlyPlayingContext.getProgress_ms() < milesecondsBeforeEnd)
                    return true;
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(){
        requestLocationUpdate();
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this.getMainExecutor(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
//                    currentLocation[0] = new LatLng(location.getLatitude(),location.getLongitude());
//                    Log.d("Foreground", "CurrentLocation: " + currentLocation[0]);
                    lastKnownLocation = location;

                }
            }
        });
    }



    @SuppressLint("MissingPermission")
    public void getCurrentSpeed(){
        requestLocationUpdate();
            if(locationPermissionGranted){
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this.getMainExecutor(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && lastKnownLocation != null) {
                            currentUserSpeed = location.getSpeed();
//                            currentUserSpeed = (sqrt(pow(location.getLongitude() - lastKnownLocation.getLongitude(), 2)) + pow((location.getLatitude() - lastKnownLocation.getLatitude()),2)) ;
                        }

                    }
                });
            }
    }

    public void skipUsersPlaybackToNextTrack() {
        SkipUsersPlaybackToNextTrackRequest skipUsersPlaybackToNextTrackRequest = spotifyApi
                .skipUsersPlaybackToNextTrack()
                .build();
        try {
            final String string = skipUsersPlaybackToNextTrackRequest.execute();

            System.out.println("Null: " + string);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /*
    * Methods Below create and handle updates to the location
     */

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    // TODO: Change or Remove what happens on new Location

    private void onNewLocation(Location location) {
        lastKnownLocation = location;
        Log.d("Foreground", "Location: " + location);

        // Notify anyone listening for broadcasts about the new location.
//        Intent intent = new Intent(ACTION_BROADCAST);
//        intent.putExtra(EXTRA_LOCATION, location);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /*
    * Sends a request to update the location
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdate(){
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

        } catch (Exception e){

        }
    }








}
