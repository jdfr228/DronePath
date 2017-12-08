package com.dronepath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;


public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Toast toast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the xml Preferences layout
        addPreferencesFromResource(R.xml.preferences);

        toast = Toast.makeText(getContext(), "", Toast.LENGTH_LONG);

        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
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

    public void alertUser(String message) {
        toast.setText(message);
        toast.show();
    }
}
