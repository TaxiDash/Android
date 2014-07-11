package com.gmail.brian.broll.taxidash.app;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.io.File;


/**
 * Splash screen for the TaxiDash app. Here we initially load the server info.
 */
public class TaxiDash extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {
    private static int CAN_PROCEED = 2;
    private static long MIN_SPLASH_TIME = 2400;
    private static long MAX_SPLASH_TIME = 6400;
    private int currentStatus = 0;
    private LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        Typeface fontAwesome = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        TextView logo = (TextView) findViewById(R.id.splash_message);
        logo.setTypeface(fontAwesome);
        */

        ActionBar actionBar = getActionBar();

        if(actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.activity_taxi_dash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(++currentStatus == CAN_PROCEED) {
                    startNearbyCabs();
                }
            }
        }, MIN_SPLASH_TIME);

        //Add support for max splash time
        //If it hasn't connected by now, we should
        //see if we have stored a recent TaxiDash
        //server address
        //TODO

        //mLocationClient.connect();
    }

    //Location Client info
    @Override
    public void onStart() {
        super.onStart();
        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = mLocationClient.getLastLocation();
        Log.i("Location Found", location.getLatitude() + ", " + location.getLongitude());
        new InitializeConstants().execute(location);
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onDisconnected() {
        Log.e("Location Client", "Location client disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Location Client", "Location client failed");
    }

    private class InitializeConstants extends Utils.initializeTaxiDashConstants {

        @Override
        protected Void doInBackground(Location... params){
            return super.doInBackground(params);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //Display toast about connection
            if(CONSTANTS.CURRENT_SERVER != null) {
                Toast.makeText(getApplicationContext(), "Connected to "
                        + CONSTANTS.CURRENT_SERVER.getCity() + ", " + CONSTANTS.CURRENT_SERVER.getState(),
                        Toast.LENGTH_SHORT).show();

                Log.i("getFilesDir", getFilesDir() + "");
                CONSTANTS.TEMP = getFilesDir() + "/" + CONSTANTS.CURRENT_SERVER.getCity() + "_"
                        + CONSTANTS.CURRENT_SERVER.getState() + "_TEMP/";
                //Make the file
                File temp = new File(CONSTANTS.TEMP);
                temp.mkdir();

                Utils.debugLogging(getApplicationContext(), "TEMP Dir is set to " + CONSTANTS.TEMP);

            } else {
                Toast.makeText(getApplicationContext(), "Could not contact TaxiDash.",
                        Toast.LENGTH_SHORT).show();
            }

            if(++currentStatus == CAN_PROCEED) {
                startNearbyCabs();
            }else{
                //start scanning for nearby cabs?
                //TODO
            }
        }
    }

    private void startNearbyCabs(){
        // After completing http call
        // will close this activity and lauch main activity
        Intent i = new Intent(TaxiDash.this, NearbyCabList.class);
        startActivity(i);
        // close this activity
        finish();
    }
}

