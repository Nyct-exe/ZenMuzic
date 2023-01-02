package com.example.zenmuzic.playlistRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zenmuzic.R;
import com.example.zenmuzic.ZenMusicApplication;
import com.example.zenmuzic.routeRecycleView.Route;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;


public class PlaylistRecyclerView extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button confirmButton;

    private PlaylistAdapter playlistAdapter;
    private ArrayList<Playlist> playlists = new ArrayList<>();

    // Spotify
    private String AUTH_TOKEN;
    private Route route;
    private Intent intent;

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
        AUTH_TOKEN = ((ZenMusicApplication) this.getApplication()).getAUTH_TOKEN();
        route = (Route) getIntent().getSerializableExtra("ROUTE_OBJECT");

        GetSpotifyPlaylists_Async();
        intent = new Intent();
        intent.putExtra("ROUTE_OBJECT", route);
        intent.putExtra("AUTH_TOKEN", AUTH_TOKEN);
        /**
         * Currently displays just the name of the selected playlist
         */
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistAdapter.getSelectedPlaylist() != null){
                    route.setPlaylist(playlistAdapter.getSelectedPlaylist());
                    setResult(RESULT_OK, intent);
                    finish();
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