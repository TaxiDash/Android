package com.gmail.brian.broll.taxidash.app;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


/**
 * Splash screen for the TaxiDash app. Here we initially load the server info.
 */
public class TaxiDash extends Activity {
    private static int CAN_PROCEED = 2;
    private static long MIN_SPLASH_TIME = 2400;
    private int currentStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        new InitializeConstants().execute();
    }

    private class InitializeConstants extends Utils.setTaxiDashConstants {

        @Override
        protected Void doInBackground(Void... params){
            return super.doInBackground(params);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(++currentStatus == CAN_PROCEED) {
                startNearbyCabs();
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

