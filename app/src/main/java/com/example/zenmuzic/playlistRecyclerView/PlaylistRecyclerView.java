package com.example.zenmuzic.playlistRecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zenmuzic.MainActivity;
import com.example.zenmuzic.R;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;

public class PlaylistRecyclerView extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button confirmButton;

    private PlaylistAdapter playlistAdapter;
    private ArrayList<Playlist> playlists = new ArrayList<>();

    // Spotify
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "e728ce73ce224bed8731b892dd710540";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_recycler_view);
        recyclerView = findViewById(R.id.playlistRecyclerView);
        confirmButton = findViewById(R.id.playlistConfirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        playlistAdapter = new PlaylistAdapter(this,playlists);
        recyclerView.setAdapter(playlistAdapter);

        GetSpotify();

        PopulateData();

        /**
         * Currently displays just the name of the selected playlist
         */
        //TODO: Implement actual setting of the playlist to the route once routes are created
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistAdapter.getSelectedPlaylist() != null){
                    Toast.makeText(PlaylistRecyclerView.this, playlistAdapter.getSelectedPlaylist().getName(), Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(PlaylistRecyclerView.this, "Playlist Not Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * This current implementation seems silly but I can't find anything about on how to pass SpotifyAppRemote between
     */
    private void GetSpotify() {
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

                        // Now you can start interacting with App Remote
//                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("PlaylistRecyclerView", throwable.getMessage(), throwable);
                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });

    }

    private void PopulateData() {

        for(int i = 0; i < 10; i++){
            Playlist playlist = new Playlist();
            playlist.setName("Cool Music "+ i);
            playlists.add(playlist);
        }

        playlistAdapter.setPlaylists(playlists);
    }
}