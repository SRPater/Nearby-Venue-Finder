package com.stefanpater.cmtprg01_7.nearbyvenuefinder;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class FindVenuesActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOG_TAG = FindVenuesActivity.class.getSimpleName();
    private static final String FOURSQUARE_API_ID = "FCPGJYKWQDF1I3XFLFWVVOXF3FRZXTA4PAP5R24J3ERH1RB5";
    private static final String FOURSQUARE_API_SECRET = "AN5XYBSZZBTK4AQ2UOLSMZHBX31ATPYOLGPIGGF204UPQEON";
    private static final String FOURSQUARE_API_URL = "https://api.foursquare.com/v2/venues/search?";
    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/geocode/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyAH9vp-16bvFbn4iZ5iQ-qMeZDYTaxwxj4";
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient apiClient;
    private Location location;
    private LocationRequest locationRequest;
    private String latitude;
    private String longitude;
    private ArrayList<HashMap<String, String>> venues;
    private ListView lstVenues;
    private int locationUpdateCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_venues);

        // Setup the toolbar
        Toolbar findVenuesToolbar = (Toolbar)findViewById(R.id.findVenuesToolbar);
        setSupportActionBar(findVenuesToolbar);

        // Get the ListView and create and ArrayList to hold the venues
        lstVenues = (ListView)findViewById(R.id.lstVenues);
        venues = new ArrayList<>();

        // Get the "Back" button and set the OnClickListeners
        Button btnBack = (Button)findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

        // Build the GoogleApiClient
        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest (intervals in milliseconds)
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(1000);
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
        // Start the SettingsActivity if the settings item in the menu is clicked
        if (item.getItemId() == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Connect the GoogleApiClient
        apiClient.connect();
        venues = new ArrayList<>();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates and disconnect the GoogleApiClient if it is connected
        if (apiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
            apiClient.disconnect();
        }
    }

    @Override
    public void onClick(View v) {
        // Finish the Activity when the back button is clicked
        if (v.getId() == R.id.btnBack) {
            finish();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Try to get the last known location and log a message if a SecurityException is thrown
        try {
            location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        } catch (SecurityException e) {
            Log.d(LOG_TAG, e.getMessage());
        }

        // Check if the last known location was found and get location updates if it's not
        if (location == null) {
            // Try to request location updates and log a message if a SecurityException is thrown
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
            } catch (SecurityException e) {
                Log.d(LOG_TAG, e.getMessage());
            }
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Send a toast message and try to reconnect
        Context toastContext = getApplicationContext();
        CharSequence toastText = getString(R.string.txtConnectionToast);
        int toastDuration = Toast.LENGTH_SHORT;

        Toast connectionToast = Toast.makeText(toastContext, toastText, toastDuration);
        connectionToast.show();

        apiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         *
         * Copied this method from tutorial's GitHub.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.d(LOG_TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationUpdateCounter == 0) {
            locationUpdateCounter++;
            handleNewLocation(location);
        } else {
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
        }
    }

    private void handleNewLocation(Location location) {
        // Get the latitude and longitude to use in the API URL
        latitude     = String.valueOf(location.getLatitude());
        longitude    = String.valueOf(location.getLongitude());

        new RetrieveVenuesTask().execute();
        new RetrieveLocationTask().execute();
    }

    private class RetrieveVenuesTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Get the radius preference to use in the Foursquare URL
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(FindVenuesActivity.this);
            String radius = sharedPref.getString(SettingsActivity.KEY_PREF_RADIUS, "100");

            // Try to retrieve information and log a message if an exception is caught
            try {
                // Build the URL using the latitude, longitude, radius setting and the Foursquare ID and key
                URL foursquareUrl = new URL(
                        FOURSQUARE_API_URL
                        + "&ll=" + latitude
                        + "," + longitude
                        + "&intent=browse&radius=" + radius
                        + "&client_id=" + FOURSQUARE_API_ID
                        + "&client_secret=" + FOURSQUARE_API_SECRET
                        + "&v=20170410"
                );

                // Open the connection
                HttpURLConnection connection = (HttpURLConnection)foursquareUrl.openConnection();

                // Try to read the input from the connection's input stream and disconnect when ready
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    // Add every line of the input to the string builder
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }

                    // Close the input reader and return the built string
                    reader.close();
                    return builder.toString();
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            // Log a message if there was no response or build a list of venues if there is
            if (response == null) {
                Log.d(LOG_TAG, "There was an error");

                // Set an error message as the only item in the venues array
                HashMap<String, String> venue = new HashMap<>();
                venue.put("name", "There was an error");
                venues.add(venue);
            } else {
                // Try to parse the JSON and log a message if a JSONException is caught
                try {
                    JSONObject jsonObject = new JSONObject(response);

                    // Get the venues array
                    JSONObject res = jsonObject.getJSONObject("response");
                    JSONArray jsonVenues = res.getJSONArray("venues");

                    if (jsonVenues.length() > 0) {
                        // Loop through the venues array to get each venue's name
                        for (int i = 0; i < jsonVenues.length(); i++) {
                            JSONObject jsonVenue = jsonVenues.getJSONObject(i);
                            String name = jsonVenue.getString("name");

                            // Put the data in a HashMap and store it in the venues array
                            HashMap<String, String> venue = new HashMap<>();
                            venue.put("name", name);
                            venues.add(venue);
                        }
                    } else {
                        // Set a warning message as the only item in the venues array
                        HashMap<String, String> venue = new HashMap<>();
                        venue.put("name", "No venues found within radius");
                        venues.add(venue);
                    }
                } catch (JSONException e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
            }

            // Showing parsed JSON in ListView
            ListAdapter adapter = new SimpleAdapter(
                    FindVenuesActivity.this,
                    venues,
                    R.layout.list_item,
                    new String[]{"name"},
                    new int[]{R.id.name}
            );

            lstVenues.setAdapter(adapter);
        }
    }

    private class RetrieveLocationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Try to retrieve information and log a message if an exception is caught
            try {
                URL googleUrl = new URL(
                        // Build the URL using the latitude, longitude and API key
                        GOOGLE_API_URL
                        + "latlng=" + latitude
                        + "," + longitude
                        + "&key=" + GOOGLE_API_KEY
                        + "&result_type=locality"
                );

                // Open the connection
                HttpURLConnection connection = (HttpURLConnection)googleUrl.openConnection();

                // Try to read the input from the connection's input stream and disconnect when ready
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    // Add every line of the input to the string builder
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }

                    // Close the input reader and return the built string
                    reader.close();
                    return builder.toString();
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            String locationText = getString(R.string.txtLocation);

            // Log a message if there was no response or set the location text if there is
            if (response == null) {
                Log.d(LOG_TAG, "There was an error!");

                // Set a "not found" message as location in the view
                locationText += " " + getString(R.string.location_not_found);
            } else {
                // Try to parse the JSON and log a message if a JSONException is caught
                try {
                    JSONObject jsonObject = new JSONObject(response);

                    // Get the formatted address from the first item in the results array
                    JSONArray results = jsonObject.getJSONArray("results");
                    JSONObject jsonLocation = results.getJSONObject(0);
                    locationText += " " + jsonLocation.getString("formatted_address");
                } catch (JSONException e) {
                    Log.d(LOG_TAG, e.getMessage());
                    locationText += " " + getString(R.string.location_not_found);
                }
            }

            TextView txtLocation = (TextView)findViewById(R.id.txtLocation);
            txtLocation.setText(locationText);
        }
    }
}
