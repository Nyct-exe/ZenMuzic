package com.example.zenmuzic.playlistRecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {


    private Context context;
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private int checkedPosition = -1; // Default selected position, -1: no default selected

    public PlaylistAdapter(Context context, ArrayList<Playlist> playlists) {
        this.context = context;
        this.playlists = playlists;
    }

    public void setPlaylists(ArrayList<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist,parent,false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        holder.bind(playlists.get(position));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder{

        private TextView textView;
        private ImageView imageView;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.spotify_playlist_textView);
            imageView = itemView.findViewById(R.id.playlist_imageview);
        }

        void bind(final Playlist playlist){
            if (checkedPosition == -1){ // If nothing is selected the check icon is hidden
                imageView.setVisibility(View.GONE);
            } else {
                if(checkedPosition == getAdapterPosition()){ // Shows a check icon on selected playlist
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    imageView.setVisibility(View.GONE);
                }
            }

            textView.setText(playlist.getName());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageView.setVisibility(View.VISIBLE);
                    if(checkedPosition != getAdapterPosition()){
                        notifyItemChanged(checkedPosition);
                        checkedPosition = getAdapterPosition();
                    }
                }
            });

        }
    }

    public Playlist getSelectedPlaylist(){
        if(checkedPosition != -1){
            return playlists.get(checkedPosition);
        }
        return null;
    }

}
