package com.example.zenmuzic;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.zenmuzic.routeRecycleView.AbstractSerializer;
import com.example.zenmuzic.routeRecycleView.Route;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import se.michaelthelin.spotify.requests.data.player.SkipUsersPlaybackToNextTrackRequest;

public class ForegroundService extends Service {

    public ArrayList<Route> routes;

    // Spotify
    private String AUTH_TOKEN;
    // For handling web-api
    private SpotifyApi spotifyApi;
    // For handling IRC spotify communication
    private SpotifyAppRemote mSpotifyAppRemote;
    private static final String CLIENT_ID = "e728ce73ce224bed8731b892dd710540";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";


    // The entry point to the Fused Location Provider.
    // GPS
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private double currentUserSpeed;

    private final float routeTolerance = 50;
    private EnvironmentalAudioRecorder environmentalAudioRecorder;
    private List<Integer> baseAmplitudesList = new ArrayList<>();
    private AudioManager audioManager;
    private boolean recordingPermission;
    private String currentPlaylist;


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

        /*
        * Gets Sample Of 5 Seconds environment audio to use as basis for volume control
         */
//        baseAmplitudesList = environmentalAudioRecorder.getAmplitudesList(getBaseContext());

        // Gives controls of the phone's volume
        audioManager = (AudioManager) getApplicationContext().getSystemService(getBaseContext().AUDIO_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // GETTING THE AUTH TOKEN
        if(intent != null){
            Bundle extras = intent.getExtras();
            if (extras != null) {
                AUTH_TOKEN = extras.getString("AUTH_TOKEN");
                recordingPermission = extras.getBoolean("RecordingPermission");
                locationPermissionGranted = extras.getBoolean("locationGranted");
            }
        }

        if(recordingPermission){
            environmentalAudioRecorder = new EnvironmentalAudioRecorder();

            /*
             * Gets Sample Of 5 Seconds environment audio to use as basis for volume control
             */
            baseAmplitudesList = environmentalAudioRecorder.getAmplitudesList(getBaseContext());

            // Gives controls of the phone's volume
            audioManager = (AudioManager) getApplicationContext().getSystemService(getBaseContext().AUDIO_SERVICE);
        }


        spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(AUTH_TOKEN)
                .build();
        linkSpotifyAndStartAThread();
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

    private Route getRouteBasedOnSpeedAndDistance() {
        Route closestRoute = null;
        float shortestDistance = (float) routeTolerance;
        for (Route route: routes){
            if(route.getListOfPoints().size() != 0) {
                float shortestDistanceForRoute = Float.MAX_VALUE;
                for(LatLng point : route.getListOfPoints()) {
                    float[] routeResult = new float[3];
                    Location.distanceBetween(point.latitude, point.longitude, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), routeResult);
                    if(routeResult[0] < shortestDistanceForRoute) {
                        shortestDistanceForRoute = routeResult[0];
                    }
                }
                if(shortestDistanceForRoute < shortestDistance && currentUserSpeed < route.getSpeed()) {
                    closestRoute = route;
                    shortestDistance = shortestDistanceForRoute;
                }
            }
        }
        return closestRoute;
    }

    private void serviceThread() {
        new Thread(
                new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        while(true){
                            loadData();
                            if(isSongFinishing(5000)){
                                Route route = getRouteBasedOnSpeedAndDistance();
                                if(route != null && route.getPlaylist() != null && !route.getPlaylist().getUri().equals(currentPlaylist)) {
                                    Log.d("Foreground","Playlist: "+ route.getPlaylist().getName());
                                    mSpotifyAppRemote.getPlayerApi().setShuffle(true);
                                    mSpotifyAppRemote.getPlayerApi().play(route.getPlaylist().getUri());
                                }
                                // The service is too fast and sometimes manages to change playlist twice.
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            getCurrentSpeed();
                            Log.d("Foreground","Speed: "+currentUserSpeed);
                            if(recordingPermission){
                                adjustVolumeBasedOnEnvironment();
                            }

                        }
                    }
                }
        ).start();

    }
    /*
    *   Loads Data from sharedPreferences:
    * Routes - All saved routes of the current user
    * Permissions - currently only Record Audio
     */

    private void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = sharedPreferences.getString("routes list", null);
        Type type = new TypeToken<ArrayList<Route>>() {}.getType();
        routes = gson.fromJson(json, type);
        if (routes == null) {
            routes = new ArrayList<>();
        }
        recordingPermission = sharedPreferences.getBoolean("RECORD_AUDIO",false);

    }
    /*
    *   Saves The latest playlist to preferences so it could be uses to generate a uri for sharing a playlist.
     */
    private void savePlaylistUri(String playlistUri){
        SharedPreferences sharedPref = getBaseContext().getSharedPreferences("shared preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("currentPlaylist",playlistUri);
        editor.apply();
    }

    /*
    * Note to developers, this method crashes if you're listening to a PodCast.
     */

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

        /*
         * Left it here in case it keeps killing the app on the actual phone.
         */
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

    public void linkSpotifyAndStartAThread(){
        // Connect Spotify Remote
        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(false)
                        .build();


        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("ForegroundService", "Connected! Yay!");
                        serviceThread();
                        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerContext()
                                .setEventCallback(playerContext -> {
                                    currentPlaylist = playerContext.uri;
                                })
                                .setErrorCallback(throwable -> {
                                });

                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("ForegroundService", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void adjustVolumeBasedOnEnvironment(){
        List<Integer> currentAmplitudes = environmentalAudioRecorder.getAmplitudesList(getBaseContext());
        int response = environmentalAudioRecorder.isEnvironmentLoud(baseAmplitudesList,currentAmplitudes);
        switch (response){
            case -1:
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER,0);
                break;
            case 0:
                audioManager.adjustVolume(AudioManager.ADJUST_SAME,0);
                break;
            case 1:
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE,0);
                break;

        }
        // updates amplitudes list with the previous amplitude data
//        baseAmplitudesList = currentAmplitudes;
    }





}
