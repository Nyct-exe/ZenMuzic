package com.example.zenmuzic.playlistRecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zenmuzic.R;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.requests.data.library.GetCurrentUsersSavedAlbumsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;


public class PlaylistRecyclerView extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button confirmButton;

    private PlaylistAdapter playlistAdapter;
    private ArrayList<Playlist> playlists = new ArrayList<>();

    // Spotify
    private String AUTH_TOKEN;

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

        // Getting Auth Token
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            AUTH_TOKEN = extras.getString("AUTH_TOKEN");
        }

        GetSpotifyPlaylists_Async();

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
     * Implements Java wrapper for Spotify Web API which is then used to retrieve playlist data
     * https://github.com/spotify-web-api-java/spotify-web-api-java
     */

    public void GetSpotifyPlaylists_Async(){
        /**
         * Setups the api for the spotify and gets the list of user playlists.
         */
        SpotifyApi api = new SpotifyApi.Builder()
                .setAccessToken(AUTH_TOKEN)
                .build();

        GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = api
                .getListOfCurrentUsersPlaylists()
                .build();

        try {
            final CompletableFuture<Paging<PlaylistSimplified>> pagingFuture = getListOfCurrentUsersPlaylistsRequest.executeAsync();
            // Thread free to do other tasks...

            final Paging<PlaylistSimplified> playlistSimplifiedPaging = pagingFuture.join();
            /**
             * Creating Playlist objects and populating the recyclerview with real data
             */
            // Adding user created playlists
            for(PlaylistSimplified p: playlistSimplifiedPaging.getItems()){
                Playlist playlist = new Playlist();
                playlist.setName(p.getName());
                playlist.setUri(p.getUri());
                playlists.add(playlist);
            }
            playlistAdapter.setPlaylists(playlists);

        } catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }

    }

}