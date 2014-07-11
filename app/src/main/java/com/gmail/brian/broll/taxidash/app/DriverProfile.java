package com.gmail.brian.broll.taxidash.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This activity will show the driver information in a full panel.
 * It will show a "Start Ride" button which will allow the user
 * to transition to another activity (fare estimator).
 *
 * Created by Brian Broll on 5/15/14.
 */
public class DriverProfile extends NavigationActivity implements LocationListener{
    private static final long MIN_TIME_BW_UPDATES = 10000;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.2f;

    private String DRIVER_PROFILE = "DRIVER PROFILE";
    private Driver driver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_driver_profile, null, false);
        content.addView(contentView, 0);

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
        //rating.setNumStars(CONSTANTS.MAX_RATING);
        rating.setRating(driver.getRating());
        rating.setOnClickListener(null);//nop on click

        if(!driver.hasValidLicense()) {
            BootstrapButton button = (BootstrapButton) findViewById(R.id.rideButton);
            button.setBootstrapType("danger");
            button.setBootstrapButtonEnabled(true);
            button.setText("Invalid License");
            button.setOnClickListener(null);
        }

        if(driver.getImage() == null) {
            new setDriverImage().execute();
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
        //Create a ride
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = -181;
        double latitude = -181;

        Log.i("GPS", "LocationManager is null? " + (lm == null));
        Log.i("GPS", "Location is null? " + (location == null));

        if(location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        final Ride ride = new Ride(latitude, longitude);

        //Send intent to the rate driver activity
        //Intent viewDriverIntent = new Intent(v.getContext(), FareEstimator.class);
        Log.i("ON PROFILE EXIT", "Driver image is " + driver.getImageURL());

        //Prompt to estimate fare
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Estimate Fare?");
        alert.setMessage("Would you like to estimate your fare?");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Start fare estimator
                Intent intent = new Intent(getApplicationContext(), FareEstimator.class);
                intent.putExtra("Driver", (android.os.Parcelable) driver);
                if (ride.getStartLatitude() != -181 && ride.getStartLongitude() != -181){
                    //Then we could get the ride info
                    intent.putExtra("Ride", ride);
                }
                startActivity(intent);

            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Start rating page
                Intent intent = new Intent(getApplicationContext(), RateDriver.class);
                intent.putExtra("Driver", (android.os.Parcelable) driver);
                if (ride.getStartLatitude() != -181 && ride.getStartLongitude() != -181){
                    //Then we could get the ride info
                    intent.putExtra("Ride", ride);
                }
                startActivity(intent);
            }
        });

        alert.show();


    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class setDriverImage extends AsyncTask<Void, Void, Void> {
        /*
         * This should probably be moved to the main screen and
         * only done if the driver's image is null
         */

        @Override
        protected Void doInBackground(Void... params) {
            //Get the current driver's image
            int beaconId = driver.getBeaconId();
            String endpoint = CONSTANTS.CURRENT_SERVER.getAddress() + "/mobile/images/drivers/"
                    + beaconId + ".json";

            //Get the image for the driver
            try {
                Bitmap image = Utils.getImageFromServer(endpoint);

                File path = new File(CONSTANTS.TEMP + beaconId + ".png");//Change this to CONSTANTS.TEMP?
                saveImageToCache(path.getAbsolutePath(), image);
                driver.setImage(path.getAbsolutePath());
                Log.i("DRIVER IMAGE", "SAVING IMAGE TO " + path.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("getBmpFromUrl error: ", e.getMessage().toString());
                //Utils.debugLogging(getApplicationContext(), "Could not get ");
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

    public Location getLocation() {
        LocationManager locationManager;
        boolean isGPSEnabled, isNetworkEnabled;
        double latitude, longitude;
        Location location = null;

        try {
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                //this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

}
