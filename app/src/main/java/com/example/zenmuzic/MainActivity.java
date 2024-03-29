package com.example.zenmuzic;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.zenmuzic.interfaces.AsyncResponse;
import com.example.zenmuzic.routeRecycleView.AbstractSerializer;
import com.example.zenmuzic.routeRecycleView.Route;
import com.example.zenmuzic.routeRecycleView.RouteRecycleView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.navigation.NavigationView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener, AsyncResponse {

    private Route currentRoute;
    private ArrayList<Route> allRoutes;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private GoogleMap gMap;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_PERMISSIONS = 1;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;
    private static final int DEFAULT_ZOOM = 15;
    private final LatLng defaultLocation = new LatLng(0, 0);

    // Spotify
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "e728ce73ce224bed8731b892dd710540";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    public String AUTH_TOKEN;

    // Permissions
    private boolean locationPermissionGranted;
    private boolean recordingPermissionGranted;
    private boolean writeExternalStoragePermission;
    private boolean readExternalStoragePermission;

    // UI
    private Button songButton;
    private Button spotifyButton;


    private void updateUserConnectionBasedOnSpotifyStatus(boolean isConnected) {
        ImageButton forwardsButton = findViewById(R.id.forwardsButton);
        ImageButton backwardsButton = findViewById(R.id.backButton);
        if (isConnected) {
            forwardsButton.setVisibility(View.VISIBLE);
            backwardsButton.setVisibility(View.VISIBLE);
            songButton.setVisibility(View.VISIBLE);
            spotifyButton.setVisibility(View.GONE);
            getSupportActionBar().show();
        }
        else {
            forwardsButton.setVisibility(View.GONE);
            backwardsButton.setVisibility(View.GONE);
            songButton.setVisibility(View.GONE);
            spotifyButton.setVisibility(View.VISIBLE);
            getSupportActionBar().hide();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setNavigationViewListener();

        // Setting up the navigation drawer
        drawerLayout = findViewById(R.id.activity_drawer);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Google Maps Static implementation
        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Button Initialization
        songButton = findViewById(R.id.songButton);
        spotifyButton = findViewById(R.id.spotifyButton);
        spotifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authSpotify();
            }
        });
        updateUserConnectionBasedOnSpotifyStatus(false);
    }
    //Lifecycle Controls
    @Override
    protected void onStart() {
        super.onStart();
        /**
         *  Currently every time the app is reopened it plays the indie rock music playlist. smth to consider.
         */
        if (SpotifyAppRemote.isSpotifyInstalled(this) && AUTH_TOKEN == null){
            authSpotify();
        }else if(!SpotifyAppRemote.isSpotifyInstalled(this)){ // if the user does not have Spotify installed it opens spotify on google play
            // Dialog Fragment initialisation
            new SpotifyMissingDialogFragment().show(getSupportFragmentManager(),"DialogBox");
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocationHandler locationHandler = new LocationHandler(this);
        locationHandler.delegate = this;
        locationHandler.execute();
        if(gMap != null){
            drawAllRoutesOnMap();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()){
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        }
    }

    private void authSpotify(){
        //Auth
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        // DECLARING SCOPES OF AUTHORIZATION
        builder.setScopes(new String[]{"app-remote-control","user-read-playback-state","user-read-playback-position","user-read-currently-playing","streaming","user-read-private","user-library-read","user-top-read","playlist-read-collaborative","playlist-read-private"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);


    }

    private void ConnectSpotify(){
        NavigationView navigationView = findViewById(R.id.nvView);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);
        //     Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");
                        // Now you can start interacting with App Remote
                        logoutItem.setTitle("Logout From Spotify");
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);
                        logoutItem.setTitle("Login To Spotify");
                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void connected() {

        syncMusicPlayerTrack();
        /**
         * Starts a Foreground Service
         */
        Log.d("MainActivity", (String.valueOf(isForegroundServiceRunning())));
        if(!isForegroundServiceRunning() && AUTH_TOKEN != null && SpotifyAppRemote.isSpotifyInstalled(this) ){
            // Starts a Service in the Foreground
            Intent serviceIntent = new Intent(this,ForegroundService.class);
            serviceIntent.putExtra("AUTH_TOKEN",AUTH_TOKEN);
            serviceIntent.putExtra("RecordingPermission",false);
            serviceIntent.putExtra("writePermission",writeExternalStoragePermission);
            serviceIntent.putExtra("readingPermission",readExternalStoragePermission);
            serviceIntent.putExtra("locationGranted",locationPermissionGranted);
            startForegroundService(serviceIntent);
        }
        updateUserConnectionBasedOnSpotifyStatus(true);
    }

    /**
     * Subscribes to playerState and everytime a player state is changed the button text is updated.
     * Updates text of songButton to show currently playing song
     */
    private void syncMusicPlayerTrack(){
        if(mSpotifyAppRemote != null){
            // Subscribe to PlayerState
            mSpotifyAppRemote.getPlayerApi()
                    .subscribeToPlayerState()
                    .setEventCallback(playerState -> {
                        final Track track = playerState.track;
                        if (track != null) {
                            songButton.setText(track.name + " by " + track.artist.name);
                        }
                    });
        }
    }

    // Spotify Login result handling
    //TODO: FLESH THIS OUT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    ((ZenMusicApplication) this.getApplication()).setAUTH_TOKEN(response.getAccessToken());
                    AUTH_TOKEN = response.getAccessToken();
                    Toast.makeText(this, "Authorised!!", Toast.LENGTH_SHORT).show();
                    ConnectSpotify();
                    break;

                // Auth flow returned an error
                case ERROR:
                    Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    Toast.makeText(this, "The Connection has been terminated", Toast.LENGTH_SHORT).show();
                    // Handle other cases
            }
        }
    }


    public void backMusicButton(View view){
        if(mSpotifyAppRemote != null){
            mSpotifyAppRemote.getPlayerApi().skipPrevious();
        }
    }

    public void forwardMusicButton(View view){
        if(mSpotifyAppRemote != null){
            mSpotifyAppRemote.getPlayerApi().skipNext();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if(actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.gMap = googleMap;
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        updateLocationUI();
        getDeviceLocation();
        drawAllRoutesOnMap();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        try {
            switch (item.getItemId()) {
                case R.id.routes: {
                    Intent intent = new Intent(this, RouteRecycleView.class);
                    startActivity(intent);
                    break;
                }

                case R.id.nav_settings: {
                    Intent intent = new Intent(this,SettingsActivity.class);
                    startActivity(intent);
                    break;
                }
                case R.id.nav_share: {
                    SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
                    String currentPlaylistUri = sharedPreferences.getString("currentPlaylist",null);
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);

                    if(currentPlaylistUri != null){
                        String[] choppedUri = currentPlaylistUri.split(":");
                        String playlistUrl = "https://open.spotify.com/playlist/";
                        playlistUrl += choppedUri[2];
                        ClipData clip = ClipData.newPlainText("Current Playlist", playlistUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Saved to Clipboard", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Currently No Playlist Assigned On Route", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case R.id.nav_logout: {
                    AuthorizationClient.clearCookies(this);
                    SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                    item.setTitle("Login To Spotify");
                    AUTH_TOKEN = null;
                    ((ZenMusicApplication) this.getApplication()).setAUTH_TOKEN(null);
                    Toast.makeText(this, "Disconnected from Spotify", Toast.LENGTH_SHORT).show();
                    updateUserConnectionBasedOnSpotifyStatus(false);
                }
            }
        } catch (Exception e){
            Toast.makeText(this, "Not Connected To Spotify", Toast.LENGTH_SHORT).show();
            authSpotify();
        }

        //close navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

// Listens to Navigation drawer Clicks
    private void setNavigationViewListener() {
        NavigationView navigationView = findViewById(R.id.nvView);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);
    }



    // MAP IMPLEMENTATION REF: https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial

    // Permissions Handling
    private void getPermissions() {
        /*
         * Request permission, so that we can get the location of the
         * device and record enviromental sounds. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationPermissionGranted = true;
                ((ZenMusicApplication) this.getApplication()).setLOCATION_PERMISSION(true);
        }
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
                recordingPermissionGranted = true;
        }
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            writeExternalStoragePermission = true;
        }
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            readExternalStoragePermission = true;
        }
        if(!locationPermissionGranted
                || !recordingPermissionGranted
                || !writeExternalStoragePermission
                || !readExternalStoragePermission){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
        }
        if(locationPermissionGranted){
            updateLocationUI();
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        recordingPermissionGranted = false;
        writeExternalStoragePermission = false;
        readExternalStoragePermission = false;
        ((ZenMusicApplication) this.getApplication()).setLOCATION_PERMISSION(false);
        if (requestCode
                == REQUEST_PERMISSIONS) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                ((ZenMusicApplication) this.getApplication()).setLOCATION_PERMISSION(true);
            }
            if(grantResults.length > 0
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                recordingPermissionGranted = true;
            }
            if(grantResults.length > 0
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED){
                writeExternalStoragePermission = true;
            }
            if(grantResults.length > 0
                    && grantResults[3] == PackageManager.PERMISSION_GRANTED){
                readExternalStoragePermission = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }
    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (gMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                gMap.setMyLocationEnabled(false);
                gMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getPermissions();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            gMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            gMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    public boolean isForegroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo serviceInfo: activityManager.getRunningServices(Integer.MAX_VALUE)){
            if(ForegroundService.class.getName().equals(serviceInfo.service.getClassName()))
                return true;
        }
        return false;
    }

    @Override
    public void processFinish(String result) {
        // Do Nothing
    }

    @Override
    public void getLocation(Location location) {
        // Handle Users Location
        lastKnownLocation = location;
        gMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void drawAllRoutesOnMap(){
        // Draw all Routes on map
        gMap.clear();
        loadRoutes();
        List<Integer> colorList = Arrays.asList(Color.RED, Color.BLUE, Color.BLACK, Color.GREEN, Color.MAGENTA);
        for(Route r:allRoutes){
            if(r != null){
                if(r.getStartingPoint() != null && r.getEndPoint() != null){
                    PolylineOptions lineOptions = new PolylineOptions();
                    lineOptions.addAll(r.getListOfPoints());
                    lineOptions.width(12);
                    Random rand = new Random(r.getName().hashCode());
                    lineOptions.color(colorList.get(rand.nextInt(colorList.size())));
                    lineOptions.geodesic(false);
                    gMap.addPolyline(lineOptions);
                }
            }
        }
    }

    private void loadRoutes(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = sharedPreferences.getString("routes list", null);
        Type type = new TypeToken<ArrayList<Route>>() {}.getType();
        allRoutes = gson.fromJson(json, type);
        if (allRoutes == null) {
            allRoutes = new ArrayList<>();
        }
    }


    /*
    * Meant to be used to only get one current route
    * Currently Disabled
     */

    private void getCurrentRoute(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = sharedPreferences.getString("currentRoute", null);
        Type type = new TypeToken<Route>() {}.getType();
        currentRoute = gson.fromJson(json, type);
    }
}