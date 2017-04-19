package com.stefanpater.cmtprg01_7.nearbyvenuefinder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup the ActionBar
        Toolbar mainToolbar = (Toolbar)findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        // Get the "Find Venues" button and add an OnClickListener
        Button btnFindVenues = (Button)findViewById(R.id.btnFindVenues);
        btnFindVenues.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Create an Intent to start the FindVenuesActivity and start it
        Intent findVenuesIntent = new Intent(this, FindVenuesActivity.class);
        startActivity(findVenuesIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Open the SettingsActivity if the settings item in the menu is clicked
        if (item.getItemId() == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return true;
    }
}
