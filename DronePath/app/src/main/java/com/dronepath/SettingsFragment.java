package com.dronepath;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Dylan on 11/12/2017.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the xml Preferences layout
        addPreferencesFromResource(R.xml.preferences);
    }
}
