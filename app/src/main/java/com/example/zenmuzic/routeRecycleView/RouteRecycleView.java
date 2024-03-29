package com.example.zenmuzic.routeRecycleView;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;
import com.example.zenmuzic.ZenMusicApplication;
import com.example.zenmuzic.mapAPI.GetDirections;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;

public class RouteRecycleView extends AppCompatActivity {
    public ArrayList<Route> routes;

    private RecyclerView recyclerView;
    private RouteAdapter routeAdapter;
    private FloatingActionButton addRouteButton;
    private ActivityResultLauncher<Intent> routeResultLauncher;


    private void getListOfLocationsForRoute(Route route) {
        if(route.getStartingPoint() != null && route.getEndPoint() != null) {
            LatLng origin = route.getStartingPoint().getLatLng();
            LatLng destination = route.getEndPoint().getLatLng();
            URL url = GetDirections.createURL(origin, destination);
            new GetDirections(route, this).execute(url);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_recycler_view);
        recyclerView = findViewById(R.id.routeRecycleView);
        addRouteButton = findViewById(R.id.routeAddButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        routeAdapter = new RouteAdapter(this, routes);
        routeAdapter.setOnItemClickListener(new RouteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                Route route = routes.get(position);
                openRouteAdd(route, position);
            }
        });
        recyclerView.setAdapter(routeAdapter);

        addRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Route route = new Route();
                routes.add(route);
                openRouteAdd(route, routes.size()-1);
            }
        });
        routeResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        int position = data.getIntExtra("POSITION", -1);
                        if(data.getBooleanExtra("DELETE", false)) {
                            routes.remove(position);
                        }
                        else {
                            Route dataRoute = ((ZenMusicApplication) this.getApplication()).getRoute();
                            routes.set(position, dataRoute);
                            getListOfLocationsForRoute(routes.get(position));
                        }
                        routeAdapter.notifyDataSetChanged();
                        saveData();
                    }
                });

        loadData();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = sharedPreferences.getString("routes list", null);
        Type type = new TypeToken<ArrayList<Route>>() {}.getType();
        routes = gson.fromJson(json, type);
        if (routes == null) {
            routes = new ArrayList<>();
        }
        routeAdapter.setRoutes(routes);
    }

    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new GsonBuilder().registerTypeAdapter(Place.class, new AbstractSerializer()).create();
        String json = gson.toJson(routes);
        editor.putString("routes list", json);
        editor.apply();
    }

    public void openRouteAdd(Route route, int position) {
        Intent intent = new Intent(RouteRecycleView.this, RouteAdd.class);
        ((ZenMusicApplication) this.getApplication()).setRoute(route);
        intent.putExtra("POSITION", position);
        routeResultLauncher.launch(intent);
    }
}