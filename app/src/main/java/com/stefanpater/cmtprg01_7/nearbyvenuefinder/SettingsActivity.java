package com.stefanpater.cmtprg01_7.nearbyvenuefinder;

import android.os.Bundle;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String KEY_PREF_RADIUS = "pref_radius";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the settings fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
