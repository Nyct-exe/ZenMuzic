package com.example.zenmuzic;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import linc.com.amplituda.Amplituda;
import linc.com.amplituda.AmplitudaProcessingOutput;

public class EnvironmentalAudioRecorder {

    private MediaRecorder mRecorder;
    private MediaPlayer mediaPlayer;
    private static String mFileName = null;
    private Amplituda amplituda;
    private List<Integer> lastAmplitudesList = new ArrayList<>();

    // Constants
    private final static int RECORDING_TIME = 3; // Seconds
    private final static int CHANGE_VOLUME_BASELINE = 2; // Db

    private void startRecording(){
        if (mFileName != null){
            mRecorder = new MediaRecorder();

            // Setting up the recorder
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(mFileName);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("Recorder","Recorder Preparation failed");
            }

            mRecorder.start();
            Log.d("Recorder","Recording has started");
        } else {
            Log.d("Recorder","Missing Recording PATH");
        }

    }

    private void stopRecording(){
        mRecorder.stop();

        mRecorder.release();
        mRecorder = null;
    }

    public List<Integer> getAmplitudesList(Context context){
        setUpStorage(context);
        amplituda = new Amplituda(context);
        AmplitudaProcessingOutput<String> processingOutput;

        startRecording();
        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME);
        } catch (InterruptedException e) {
            Log.d("Recorder", e.getMessage());
        }
        stopRecording();
        try {
            processingOutput = amplituda.processAudio(mFileName);
            Log.d("Recorder","Amplitudes: " + processingOutput.get().amplitudesAsList().toString());
            lastAmplitudesList = processingOutput.get().amplitudesAsList();
            return lastAmplitudesList;
        } catch (Exception e) {
            Log.d("Recorder",e.getMessage());
        }
        return null;
    }

    /*
    * Gets Amplitudes list without recording new audio sample
     */
    public List<Integer> getLastAmplitudesList(){
        return lastAmplitudesList;
    }

    /*
    *   Currently just uses a base value to check if its loud.
    * Implemented a number system instead of a boolean to account for cases where no change is needed
    * 1 - Increase volume
    * 0 - Keep volume at the same level
    * -1 - Decrease Volume
     */

    public int isEnvironmentLoud(List<Integer> previousAmplitudes, List<Integer> currentAmplitudes){

        float previousAmplitudesAvg = getAvg(previousAmplitudes);
        float currentAmplitudesAvg = getAvg(currentAmplitudes);

        Log.d("EnvironmentalAudioRecorder","CurrentAmpAvg: " + previousAmplitudesAvg);
        Log.d("EnvironmentalAudioRecorder","PreviousAmpAvg: " + currentAmplitudesAvg);

        if(Math.abs(currentAmplitudesAvg - previousAmplitudesAvg ) <= CHANGE_VOLUME_BASELINE ){
            return 0;
        }
        if(currentAmplitudesAvg - previousAmplitudesAvg >= 0){
            return 1;
        }
        return -1;
    }


   private void setUpStorage(Context context){
       mFileName = context.getExternalCacheDir().getAbsolutePath();
       mFileName += "/EnvironmentalRecording.3gp";
   }


    /*
    * Gets an average from a list of integers
     */

    private float getAvg(List<Integer> list){
        int sum = 0;
        for(Integer i: list){
            sum += i;
        }
        return sum/list.size();
    }


}
