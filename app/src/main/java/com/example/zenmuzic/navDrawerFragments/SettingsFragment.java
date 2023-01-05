package com.example.zenmuzic.navDrawerFragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.zenmuzic.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Preference recordingPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        recordingPreference = findPreference("RecordAudio");

        /*
          * Listens for the switch and disables or enables recording of environment based on it.
         */

        recordingPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                if( ((SwitchPreferenceCompat) preference).isChecked()){
                    savePreference(true);
                } else {
                    savePreference(false);
                }
                return false;
            }
        });
    }

    private void savePreference(boolean state){
            SharedPreferences sharedPref = getActivity().getSharedPreferences("shared preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("RECORD_AUDIO", state);
            editor.apply();
    }
}