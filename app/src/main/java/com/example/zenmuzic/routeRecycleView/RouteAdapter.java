package com.example.zenmuzic.routeRecycleView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RoutelistHolder> {
    private Context context;
    private ArrayList<Route> routes = new ArrayList<>();

    public RouteAdapter(Context context, ArrayList<Route> routes) {
        this.context = context;
        this.routes = routes;
    }

    @NonNull
    @Override
    public RouteAdapter.RoutelistHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RouteAdapter.RoutelistHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class RoutelistHolder extends RecyclerView.ViewHolder{
        public RoutelistHolder(@NonNull View itemView) {
            super(itemView);
        }


    }
}