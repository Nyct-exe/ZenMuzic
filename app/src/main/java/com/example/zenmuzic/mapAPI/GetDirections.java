package com.example.zenmuzic.mapAPI;

import android.os.AsyncTask;
import android.util.Log;

import com.example.zenmuzic.BuildConfig;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GetDirections extends AsyncTask<URL, String, String> {
    public static URL createURL(LatLng origin, LatLng destination) {
        String stringOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDestination = "destination=" + destination.latitude + "," + destination.longitude;
        String key = "key=" + BuildConfig.MAPS_API_KEY;
        String parameters = stringOrigin + "&amp;" + strDestination + "&amp;" + key;
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

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        System.out.println(result);
    }
}