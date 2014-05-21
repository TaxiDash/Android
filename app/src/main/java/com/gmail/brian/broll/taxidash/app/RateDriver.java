package com.gmail.brian.broll.taxidash.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class RateDriver extends Activity {
    /*
     * This activity is the rating screen for the app.
     * It will need to receive the selected driver object
     * and be able to submit ratings to the server.
     */

    Driver currentDriver = null;
    Rating rating;
    Ride ride;
    String USER_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_driver);

        //Set USER_ID
        TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        USER_ID = "ANDROID_" + tManager.getDeviceId();
        //Retrieve the driver
        Intent intent = getIntent();
        currentDriver = intent.getParcelableExtra("Driver");

        assert currentDriver != null;//Handle this error TODO

        rating = new Rating(currentDriver);
    }

    private void getRatingInfo(){
        //Populate rating info
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating);
        rating.setRating(ratingBar.getNumStars());

        EditText comments = (EditText) findViewById(R.id.comments);
        rating.setComments(comments.getText().toString());

        boolean checked = ((CheckBox) findViewById(R.id.submitRide)).isChecked();
        if(!checked){
            rating.removeRide();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.rate_driver, menu);
        return true;
    }

    private void submitNotification(){
        if(rating.getRating() == 5){
            //Ask to favorite the driver
            //TODO

        }else{
            //Simply display a toast
            Toast.makeText(getApplicationContext(), "Your rating has been submitted!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onSubmitRatingClicked(View view){
        /*
         * Submit rating!
         */

        Log.i("SUBMIT RATING", "ABOUT TO SUBMIT RATING");
        getRatingInfo();
        new submitRating().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

        private class submitRating extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            //create a JSON
            //Send a POST message to the server
            Company[] companies = null;
            //Get the contact info for the companies
            String endpoint = CONSTANTS.SERVER_ADDRESS + "/ratings.json";

            try {
                HttpClient http = new DefaultHttpClient();
                http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

                //Create rating JSON
                //TODO
                JSONObject jsonRating = new JSONObject();
                jsonRating.put("driver_id", currentDriver.getId());
                jsonRating.put("rating", rating.getRating());
                jsonRating.put("rider_id", USER_ID);

                if(rating.isSendingRide()) {
                    JSONObject jsonRide = new JSONObject();
                    jsonRide.put("driver_id", currentDriver.getId());
                    jsonRide.put("rider_id", USER_ID);

                    jsonRide.put("start_latitude", ride.getStartLatitude());
                    jsonRide.put("start_longitude", ride.getStartLongitude());
                    jsonRide.put("end_latitude", ride.getEndLatitude());
                    jsonRide.put("end_longitude", ride.getEndLongitude());

                    //TODO Add the following support
                    //jsonRide.put("estimated_fare", ride.getEstimatedFare());
                    //jsonRide.put("actual_fare", ride.getActualFare());

                    jsonRating.put("ride", jsonRide);
                }

                HttpPost httpPost = new HttpPost(endpoint);
                //httpPost.setHeader("Accept", "application/json");
                //httpPost.setHeader("Content-type", "application/json");
                StringEntity json = new StringEntity(jsonRating.toString());
                Log.i("RATING REQ", jsonRating.toString());

                json.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                //sets the post request as the resulting string
                httpPost.setEntity(json);

                ResponseHandler responseHandler = new BasicResponseHandler();
                http.execute(httpPost, responseHandler);
                Log.i("RATING RESPONSE", responseHandler.toString());

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void v){
            submitNotification();
        }

            /*
        private void saveImageToCache(String filename, Bitmap image) {
            Log.i("IMAGE SAVING", "SAVING IMAGE TO " + filename);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                image.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try{
                    out.close();
                } catch(Throwable ignore) {}
            }
        }
        */
    }
}
