package com.example.zenmuzic.routeRecycleView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zenmuzic.BuildConfig;
import com.example.zenmuzic.R;
import com.example.zenmuzic.playlistRecyclerView.PlaylistRecyclerView;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.Locale;

public class RouteAdd extends AppCompatActivity {
    private Route route;
    private TextInputEditText routeInput;
    private TextView playlistText;
    private AutocompleteSupportFragment startRoute;
    private AutocompleteSupportFragment endRoute;
    private ActivityResultLauncher<Intent> playlistResultLauncher;
    private int position;

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
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY, Locale.US);
        }
        startRoute = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.start_route_fragment);
        startRoute.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        endRoute = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.end_route_fragment);
        endRoute.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

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
}
