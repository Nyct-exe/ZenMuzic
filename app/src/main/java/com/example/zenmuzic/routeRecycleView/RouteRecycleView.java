package com.example.zenmuzic.routeRecycleView;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;

import java.util.ArrayList;

public class RouteRecycleView extends AppCompatActivity {

    private RecyclerView recyclerView;

    private RouteAdapter routeAdapter;
    private ArrayList<Route> routes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_recycler_view);
        recyclerView = findViewById(R.id.playlistRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        routeAdapter = new RouteAdapter(this, routes);
        recyclerView.setAdapter(routeAdapter);
    }
}