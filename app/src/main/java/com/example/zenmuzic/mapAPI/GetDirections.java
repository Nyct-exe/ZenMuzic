package com.example.zenmuzic.mapAPI;

import android.os.AsyncTask;
import android.util.Log;

import com.example.zenmuzic.BuildConfig;
import com.example.zenmuzic.interfaces.AsyncResponse;
import com.example.zenmuzic.routeRecycleView.Route;
import com.example.zenmuzic.routeRecycleView.RouteRecycleView;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GetDirections extends AsyncTask<URL, String, String> {
    public AsyncResponse delegate = null;
    private RouteRecycleView routeRecycleView;
    private Route route;

    public GetDirections(Route route, RouteRecycleView routeRecycleView) {
        this.route = route;
        this.routeRecycleView = routeRecycleView;
    }

    public static URL createURL(LatLng origin, LatLng destination) {
        String stringOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDestination = "destination=" + destination.latitude + "," + destination.longitude;
        String key = "key=" + BuildConfig.MAPS_API_KEY;
        String parameters = stringOrigin + "&" + strDestination + "&" + key;
        String output = "json";
        URL url = null;
        try {
            url = new URL("https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters);
        } catch (MalformedURLException e) {
            Log.d("Exception on URL creation", e.toString());
        }
        return url;
    }

    private String downloadURL(URL url) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Downloading Route Didn't Work", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    protected String doInBackground(URL... urls) {
        // For storing data from web service
        String data = "";
        try{
            // Fetching the data from web service
            data = downloadURL(urls[0]);
            Log.d("DownloadTask","DownloadTask : " + data);
        }catch(Exception e){
            Log.d("Background Task",e.toString());
        }
        return data;
    }

    private LatLng createLatLng(JSONObject step) throws JSONException {
        double latitude = step.getDouble("lat");
        double longitude = step.getDouble("lng");
        return new LatLng(latitude, longitude);
    }

    private ArrayList<LatLng> parseRouteJSON(String result) {
        ArrayList<LatLng> listOfLocations = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(result);
            JSONObject routeContent = root.getJSONArray("routes").getJSONObject(0);
            JSONArray steps = routeContent.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
            listOfLocations.add(createLatLng(steps.getJSONObject(0).getJSONObject("start_location")));
            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);
                listOfLocations.addAll(PolyUtil.decode(step.getJSONObject("polyline").getString("points")));
            }
        }
        catch(JSONException e) {
            System.out.println(e.getMessage());
        }
        return listOfLocations;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        ArrayList<LatLng> listOfLocations = parseRouteJSON(result);
        route.setListOfPoints(listOfLocations);
        // This allows this async to be used without a recyclerView
        if(routeRecycleView != null){
            routeRecycleView.saveData();
        } else {
            delegate.processFinish("Finished");
        }

    }
}