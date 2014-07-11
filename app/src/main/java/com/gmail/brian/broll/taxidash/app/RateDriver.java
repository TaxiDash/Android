package com.gmail.brian.broll.taxidash.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class RateDriver extends NavigationActivity {
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
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_rate_driver, null, false);
        content.addView(contentView, 0);

        //Set USER_ID
        TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        USER_ID = "ANDROID_" + tManager.getDeviceId();

        //Retrieve the driver
        Intent intent = getIntent();
        currentDriver = intent.getParcelableExtra("Driver");
        Log.i("ON RATING CREATE", "currentDriver's image is " + currentDriver.getImageURL());

        assert currentDriver != null;//Handle this error TODO

        rating = new Rating(currentDriver);

        if(intent.hasExtra("Ride")){
            this.ride = intent.getParcelableExtra("Ride");

            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            this.ride.setEndPoint(latitude, longitude);
        }else{
            LinearLayout container = (LinearLayout) findViewById(R.id.submit_ride_container);
            View submitRide = findViewById(R.id.submitRide);
            container.removeView(submitRide);
        }
    }

    private void getRatingInfo(){
        //Populate rating info
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating);
        rating.setRating((int)ratingBar.getRating());

        EditText comments = (EditText) findViewById(R.id.comments);
        rating.setComments(comments.getText().toString());

        CheckBox checkBox = (CheckBox) findViewById(R.id.submitRide);
        if(checkBox == null || !checkBox.isChecked()){
            rating.removeRide();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.rate_driver, menu);
        return true;
    }

    private void submitNotification(int responseCode){
        Log.d("RATING", "SUBMITTING NOTIFICATION with response code " + responseCode);

        if(responseCode == 201) {//Success


            if (rating.getRating() == CONSTANTS.MAX_RATING && !isFavoriteDriver(currentDriver)) {
                //Ask to favorite the driver
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage("Would you like to add " + currentDriver.getName()
                        + " to your favorite drivers?");

                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Create intent to go to favorite driver page
                        Intent nextActivity = new Intent(getApplicationContext(), FavoriteDriverList.class);
                        nextActivity.putExtra("Driver", (android.os.Parcelable) currentDriver);
                        startActivity(nextActivity);
                    }
                });

                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        Intent nextActivity = new Intent(getApplicationContext(), NearbyCabList.class);
                        Toast.makeText(getApplicationContext(), "Your rating has been submitted!", Toast.LENGTH_SHORT).show();

                        startActivity(nextActivity);
                    }
                });

                alert.show();
            }else{

                Intent nextActivity = new Intent(getApplicationContext(), NearbyCabList.class);
                Toast.makeText(getApplicationContext(), "Your rating has been submitted!", Toast.LENGTH_SHORT).show();

                startActivity(nextActivity);
            }

        } else {
            //Error occurred
            //More detail? TODO
            Toast.makeText(getApplicationContext(), "Your rating failed to save", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSubmitRatingClicked(View view){
        /*
         * Submit rating!
         */

        Log.d("SUBMIT RATING", "ABOUT TO SUBMIT RATING");
        getRatingInfo();
        new submitRating().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private boolean isFavoriteDriver(Driver driver){
        //This should check if a driver is already "favorited"
        Log.i("CHECKING FAVORITES", driver.getName() + " is fav? " + Utils.loadFavoriteDrivers(this.getApplicationContext()).contains(driver));
        ArrayList<Driver> favoriteDrivers =  Utils.loadFavoriteDrivers(this.getApplicationContext());
        for(Driver fav : favoriteDrivers){
            if(fav.getBeaconId() == driver.getBeaconId()){
                return true;
            }
        }

        return false;
    }

        private class submitRating extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            String endpoint = CONSTANTS.CURRENT_SERVER.getAddress() + "/ratings.json";
            int responseCode = -1;

            try {
                //Create rating JSON
                JSONObject jsonRating = new JSONObject();
                jsonRating.put("driver_id", currentDriver.getId());
                jsonRating.put("rating", rating.getRating());
                jsonRating.put("comments", rating.getComments());
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

                //Submit the rating
                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                Log.d("JSON CONTENT", jsonRating.toString());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("rating", jsonRating);
                StringEntity json = new StringEntity(jsonObject.toString());
                json.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                connection.setRequestProperty("Content-Type", "application/json");
                OutputStream out = connection.getOutputStream();

                try {
                    json.writeTo(out);
                }finally {
                    out.close();
                }

                responseCode = connection.getResponseCode();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return responseCode;
        }

        protected void onPostExecute(Integer v){
            submitNotification(v);
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
