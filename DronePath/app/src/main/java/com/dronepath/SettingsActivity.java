package com.dronepath;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Toast toast;

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

        // Register setting change listener
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

        // TODO- Set up Listener (may be a better way to do it?) to update the Telemetry summaries

    }

    // Listener for when any Setting is changed
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        double minAltitude = Float.parseFloat(sharedPreferences.getString(
                "pref_key_min_altitude", ""));
        double maxAltitude = Float.parseFloat(sharedPreferences.getString(
                "pref_key_max_altitude", ""));

        // Fix an incorrect min/max configuration
        if (key.equals("pref_key_min_altitude") || key.equals("pref_key_max_altitude")) {
            if (minAltitude > maxAltitude) {
                alertUser("Minimum altitude cannot be greater than Maximum altitude");

                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (key.equals("pref_key_min_altitude")) {
                    editor.putString(key, Double.toString(maxAltitude));
                } else { editor.putString(key, Double.toString(minAltitude)); }
                editor.apply();

                // TODO- editor change not immediately reflected in EditText
            }
        }

        // Maintain a safe minimum altitude setting
        if (key.equals("pref_key_min_altitude")) {
            if (minAltitude < 10.0) {
                alertUser("Unsafe minimum altitude setting- resetting to 10 meters");

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key, "10.0");
                editor.apply();
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
        // Unregister setting change listener to be safe
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void alertUser(String message) {
        toast.setText(message);
        toast.show();
    }
}