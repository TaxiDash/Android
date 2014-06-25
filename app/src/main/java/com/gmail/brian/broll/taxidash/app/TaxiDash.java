package com.gmail.brian.broll.taxidash.app;

import com.gmail.brian.broll.taxidash.app.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * Splash screen for the TaxiDash app. Here we initially load the server info.
 */
public class TaxiDash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();

        setContentView(R.layout.activity_taxi_dash);

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
            // After completing http call
            // will close this activity and lauch main activity
            Intent i = new Intent(TaxiDash.this, NearbyCabList.class);
            startActivity(i);
            // close this activity
            finish();
        }
    }
}

