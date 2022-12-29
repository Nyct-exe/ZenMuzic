package com.example.zenmuzic.routeRecycleView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;

import java.util.ArrayList;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteListHolder> {
    private Context context;
    private ArrayList<Route> routes = new ArrayList<>();

    public RouteAdapter(Context context, ArrayList<Route> routes) {
        this.context = context;
        this.routes = routes;
    }

    public void setRoutes(ArrayList<Route> routes) {
        this.routes = routes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_route,parent,false);
        return new RouteListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteListHolder holder, int position) {
        holder.bind(routes.get(position));
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    class RouteListHolder extends RecyclerView.ViewHolder{
        private TextView routeName;

        public RouteListHolder(@NonNull View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.route_name_textView);

        }

        void bind(final Route route) {
            routeName.setText(route.getName());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }


    }
}