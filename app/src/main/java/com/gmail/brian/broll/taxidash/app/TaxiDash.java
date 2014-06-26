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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;


/**
 * Splash screen for the TaxiDash app. Here we initially load the server info.
 */
public class TaxiDash extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {
    private static int CAN_PROCEED = 2;
    private static long MIN_SPLASH_TIME = 2400;
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

        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
    }

    //Location Client info
    @Override
    public void onStart() {
        super.onStart();
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
            if(++currentStatus == CAN_PROCEED) {
                startNearbyCabs();
            }else{
                //start scanning for nearby cabs
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

