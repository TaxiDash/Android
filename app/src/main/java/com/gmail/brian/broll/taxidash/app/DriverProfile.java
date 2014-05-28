package com.gmail.brian.broll.taxidash.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Brian Broll on 5/15/14.
 */
public class DriverProfile extends Activity{
    /*
     * This activity will show the driver information in a full panel.
     * It will show a "Start Ride" button which will allow the user
     * to transition to another activity (fare estimator).
     */

    private String DRIVER_PROFILE = "DRIVER PROFILE";
    private Driver driver;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);
        //Get the intent
        Intent intent = getIntent();
        driver = intent.getParcelableExtra("Driver");

        Log.i(DRIVER_PROFILE, "ABOUT TO CREATE PROFILE FOR DRIVER");
        if(driver == null){
            Log.i(DRIVER_PROFILE, "DRIVER IS NULL" );
        }else{
            Log.i(DRIVER_PROFILE, "DRIVER IS " + driver.getName());
        }

        if(driver == null){
            //Fail gracefully-ish
            driver = new Driver(-1, -1, "NO DRIVER RECEIVED", null, -1, "", false);
        }

        //Set the driver attributes in the view
        TextView name = (TextView) findViewById(R.id.name);
        name.setText(driver.getName());
        name.setTextSize(30);

        TextView company = (TextView) findViewById(R.id.company_name);
        company.setText(driver.getCompanyName());


        RatingBar rating = (RatingBar) findViewById(R.id.driverRating);
        rating.setNumStars(CONSTANTS.MAX_RATING);
        rating.setRating(driver.getRating());

        //TextView valid = (TextView) findViewById(R.id.valid);
        //String validTag = "Valid License";
        if(!driver.hasValidLicense()) {
            //Consider switching this out so the background
            //Changes to alert that the license is invalid
            //OW, assume license valid TODO
            //validTag = "License invalid";
        }
        //valid.setText(validTag);
        if(driver.getImage() == null) {
            new setDriverImage().execute(driver);
        }else{
            updateDriverImage();
        }
    }

    private void updateDriverImage() {
        //Set the image
        ImageView image = (ImageView) findViewById(R.id.image);

        Bitmap bitmap = driver.getImage();
        if (bitmap != null && image != null) {
            image.setImageBitmap(bitmap);
        }
    }

    public void onStartRideClicked(View v){
        //Send intent to the rate driver activity
        Intent viewDriverIntent = new Intent(v.getContext(), RateDriver.class);
        //Intent viewDriverIntent = new Intent(v.getContext(), FareEstimator.class);
        viewDriverIntent.putExtra("Driver", (android.os.Parcelable) driver);
        Log.i("ON PROFILE EXIT", "Driver image is " + driver.getImageURL());
        startActivity(viewDriverIntent);
    }

    private class setDriverImage extends AsyncTask<Driver, Void, Void> {
        /*
         * This should probably be moved to the main screen and
         * only done if the driver's image is null
         */

        @Override
        protected Void doInBackground(Driver... params) {
            //Given the beaconIds, get the driver info
            for (int i = 0; i < params.length; i++) {
                Driver drvr = params[i];
                int beaconId = driver.getBeaconId();
                String endpoint = CONSTANTS.SERVER_ADDRESS + "/mobile/" + beaconId + ".json";

                //Get the image for the driver
                try {
                    URL url = new URL(CONSTANTS.SERVER_ADDRESS + "/mobile/images/drivers/" + beaconId + ".json");
                    HttpURLConnection connection = (HttpURLConnection) url
                            .openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap img = BitmapFactory.decodeStream(input);

                    File path = new File(getCacheDir(), beaconId + ".png");
                    saveImageToCache(path.getAbsolutePath(), img);
                    drvr.setImage(path.getAbsolutePath());
                    Log.i("DRIVER IMAGE", "SAVING IMAGE TO " + path.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("getBmpFromUrl error: ", e.getMessage().toString());
                    return null;
                }
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            updateDriverImage();
        }

        private void saveImageToCache(String filename, Bitmap image) {
            Log.i("IMAGE SAVING", "SAVING IMAGE TO " + filename);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                image.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

}
