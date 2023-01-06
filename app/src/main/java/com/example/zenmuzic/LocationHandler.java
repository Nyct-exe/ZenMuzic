package com.example.zenmuzic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.zenmuzic.interfaces.AsyncResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.net.URL;

public class LocationHandler extends AsyncTask<URL, String, String>  {

    // The entry point to the Fused Location Provider.
    // GPS
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted;
    public Location lastKnownLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean LOCATION_PERMISSION_GRANTED;

    public AsyncResponse delegate = null;




    public LocationHandler(Activity activity) {
        LOCATION_PERMISSION_GRANTED = ((ZenMusicApplication) activity.getApplication()).isLOCATION_PERMISSION();
        if(LOCATION_PERMISSION_GRANTED){
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    onNewLocation(locationResult.getLastLocation());
                }
            };

            createLocationRequest();
            getCurrentLocation(activity);
        } else {
            Toast.makeText(activity, "Location Permissions Missing", Toast.LENGTH_SHORT).show();
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
    public void requestLocationUpdate(){
        if(LOCATION_PERMISSION_GRANTED){
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        }
    }
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(Activity activity){
        requestLocationUpdate();
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lastKnownLocation = location;
                    delegate.getLocation(location);

                }
            }
        });
    }

    @Override
    protected String doInBackground(URL... urls) {
        return null;
    }
}
