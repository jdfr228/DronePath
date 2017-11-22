package com.dronepath;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Apply the Preferences xml (in res -> xml)
        PreferenceFragment prefs = new SettingsFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, prefs);
        fragmentTransaction.commit();

        // TODO- Set up Listener (may be a better way to do it?) to update the Telemetry summaries

    }

    // Listener for when any Setting is changed
    // TODO- not set up properly- doesn't trigger
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("myTag", "onSharedPreferenceChanged listener triggered");

        // Perform a sanity check on min and max Altitudes
        if (key.equals("pref_key_min_altitude") || key.equals("pref_key_max_altitude")) {
            Log.d("myTag", "minAltitude or maxAltitude changed");
            double minAltitude = Double.parseDouble(sharedPreferences.getString(
                    "pref_key_min_altitude", ""));
            double maxAltitude = Double.parseDouble(sharedPreferences.getString(
                    "pref_key_max_altitude", ""));

            if (minAltitude > maxAltitude) {
                Log.d("myTag", "minAltitude > maxAltitude");
                // TODO- reverse Setting change
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}