package com.example.zenmuzic.routeRecycleView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zenmuzic.BuildConfig;
import com.example.zenmuzic.LocationHandler;
import com.example.zenmuzic.R;
import com.example.zenmuzic.mapAPI.GetDirections;
import com.example.zenmuzic.playlistRecyclerView.PlaylistRecyclerView;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

public class RouteAdd extends AppCompatActivity implements OnMapReadyCallback {
    private Route route;
    private TextInputEditText routeInput;
    private TextView playlistText;
    private Spinner spinnerTransport;
    private AutocompleteSupportFragment startRoute;
    private AutocompleteSupportFragment endRoute;
    private ActivityResultLauncher<Intent> playlistResultLauncher;
    private int position;
    private String spinnerValue;
    private GoogleMap mMap;
    private LocationHandler locationHandler;
    private Location currentLocation;
    // SPEEDS FOR TRANSPORT
    private static final double WALKING = 3;
    private static final double CAR = 9;



    private void handleIntent(Intent intent) {
        Route extraRoute = intent.getParcelableExtra("ROUTE_OBJECT");
        if (extraRoute != null) {
            route = extraRoute;
            routeInput.setText(route.getName());
            if(route.getPlaylist() != null ) {
                playlistText.setText(route.getPlaylist().getName());
            }
            if(route.getStartingPoint() != null) {
                startRoute.setText(route.getStartingPoint().getName());
            }
            if(route.getEndPoint() != null) {
                endRoute.setText(route.getEndPoint().getName());
            }

        }
        if(intent.getIntExtra("POSITION", -1) != -1) {
            position = intent.getIntExtra("POSITION", -1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);
        routeInput = findViewById(R.id.routeNameInput);
        playlistText = findViewById(R.id.playlistText);
        spinnerTransport = findViewById(R.id.spinner_transport);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY, Locale.US);
        }
        startRoute = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.start_route_fragment);
        startRoute.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        startRoute.setHint("Starting Location");

        endRoute = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.end_route_fragment);
        endRoute.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        endRoute.setHint("Destination");

        startRoute.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                route.setStartingPoint(place);
            }

            @Override
            public void onError(@NonNull Status status) {
            }
        });

        endRoute.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                route.setEndPoint(place);
            }

            @Override
            public void onError(@NonNull Status status) {
            }
        });

        handleIntent(getIntent());

        playlistResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            handleIntent(data);
                        }
                    }
                });

        findViewById(R.id.playlistInput).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                route.setName(routeInput.getText().toString());
                switch (spinnerValue){
                    case "Walking":
                        route.setSpeed(WALKING);
                        break;
                    case "Car":
                        route.setSpeed(CAR);
                        break;
                    default:
                        route.setSpeed(WALKING);
                        break;
                }


                Intent intent = new Intent(RouteAdd.this, PlaylistRecyclerView.class);
                intent.putExtra("ROUTE_OBJECT", route);
                openPlaylistForResult(intent);
            }
        });

        findViewById(R.id.saveRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishRoute(false);
            }
        });

        findViewById(R.id.deleteRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishRoute(true);
            }
        });

        // Spinner
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this,R.array.Trasnport, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerTransport.setAdapter(arrayAdapter);
        spinnerTransport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinnerValue = adapterView.getSelectedItem().toString();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                    // Do Nothing
            }
        });
        // Does not work as intended since it needs to wait for success
        // LocationData
        locationHandler = new LocationHandler(this);
        currentLocation = locationHandler.getLastKnownLocation();

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.addRouteMap);
        mapFragment.getMapAsync(this);


    }



    public void openPlaylistForResult(Intent intent) {
        playlistResultLauncher.launch(intent);
    }

    public void finishRoute(boolean delete) {
        Intent intent = new Intent();
        intent.putExtra("ROUTE_OBJECT", route);
        intent.putExtra("DELETE", delete);
        intent.putExtra("POSITION", position);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.clear();
        // Existing Route
        if(route.getStartingPoint() != null && route.getEndPoint() != null){
            LatLng startingPoint = route.getStartingPoint().getLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 14));
            MarkerOptions options = new MarkerOptions();
            options.position(route.getStartingPoint().getLatLng());
            mMap.addMarker(options);

            PolylineOptions lineOptions = new PolylineOptions();
            lineOptions.addAll(route.getListOfPoints());
            lineOptions.width(12);
            lineOptions.color(Color.RED);
            lineOptions.geodesic(false);
            mMap.addPolyline(lineOptions);
        } else { // Route is being created
            if(currentLocation != null){
                LatLng startingPoint = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 14));
            }

        }

    }

    private void updateUI(){
        mMap.clear();
        if(route.getStartingPoint() != null && route.getEndPoint() != null){
            LatLng startingPoint = route.getStartingPoint().getLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 14));
            MarkerOptions options = new MarkerOptions();
            options.position(route.getStartingPoint().getLatLng());
            mMap.addMarker(options);

            PolylineOptions lineOptions = new PolylineOptions();
            lineOptions.addAll(route.getListOfPoints());
            lineOptions.width(12);
            lineOptions.color(Color.RED);
            lineOptions.geodesic(false);
            mMap.addPolyline(lineOptions);
        }
    }

}
