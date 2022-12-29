package com.example.zenmuzic.routeRecycleView;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RouteRecycleView extends AppCompatActivity {
    public ArrayList<Route> routes;

    private RecyclerView recyclerView;
    private RouteAdapter routeAdapter;
    private FloatingActionButton addRouteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_recycler_view);

        recyclerView = findViewById(R.id.routeRecycleView);
        addRouteButton = findViewById(R.id.routeAddButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        routeAdapter = new RouteAdapter(this, routes);
        recyclerView.setAdapter(routeAdapter);

        loadData();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("routes list", null);
        Type type = new TypeToken<ArrayList<Route>>() {}.getType();
        routes = gson.fromJson(json, type);
        if (routes == null) {
            routes = new ArrayList<>();
            Route route = new Route();
            route.setName("Study Playlist");
            routes.add(route);
            Route route2 = new Route();
            route.setName("Running Playlist");
            routes.add(route2);
        }
        routeAdapter.setRoutes(routes);
    }
}