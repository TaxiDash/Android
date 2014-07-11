package com.gmail.brian.broll.taxidash.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FareEstimator extends NavigationActivity implements LocationListener, GoogleMap.OnMarkerClickListener {
    private boolean isInRide = false;
    private Driver currentDriver = null;
    private Ride ride;
    private int passengers = 1;

    //Map details
    private static final int CURRENT_PATH_COLOR = Color.GRAY;
    private static final int CURRENT_PATH_WIDTH = 15;
    private static final int DIRECTIONS_PATH_COLOR = Color.BLUE;
    private static final int DIRECTIONS_PATH_WIDTH = 15;

    private Polyline currentRoute = null;
    private Polyline directionsRoute = null;

    private static final int VIEW_BOX_PADDING = 25;

    private GoogleMap mMap;
    private Geocoder geocoder;
    private List<Marker> markers = new ArrayList<Marker>();

    //Location
    private LocationManager locationManager;
    private long MIN_UPDATE_TIME = 100;
    private long MIN_UPDATE_DISTANCE = 100;

    private long BAR_HIDE_TIME = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout contentView = (RelativeLayout) inflater.inflate(R.layout.activity_fare_estimator, null, false);
        content.addView(contentView, 0);

        //Hiding the action bar
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(false && getActionBar() != null) {//TODO
                    getActionBar().hide();
                }
            }
        }, BAR_HIDE_TIME);

        //Setting up search view
        SearchView searchView = (SearchView) findViewById(R.id.location_query);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    searchLocations(query);
                    return true;
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "There was a problem with your search",
                            Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        Intent intent = getIntent();
        if(intent.hasExtra("Driver")) {//Driver is only set if currently in ride
            currentDriver = intent.getParcelableExtra("Driver");
            ride = intent.getParcelableExtra("Ride");
            isInRide = true;
        }

        //Get location stuff
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME,
                MIN_UPDATE_DISTANCE, this);

        //Get the map
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mMap = mapFragment.getMap();

        Log.i("Map fragment null? ", (((MapFragment) getFragmentManager().findFragmentById(R.id.map) == null) + ""));

        //Map stuff
        Utils.debugLogging(getApplicationContext(), "Map is null?" + (mMap == null));
        if(mMap != null){//Now we can manipulate mMap
            //Map settings
            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(this);
            mMap.setTrafficEnabled(true);

            Utils.debugLogging(getApplicationContext(), "Map settings have been set");

            Location location = mMap.getMyLocation();
            if(location == null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            //location has been set. Update the ride info (this location is more precise)
            ride.setStartLocation(location);

            //Set the mMap to this location
            if(location != null){
                Log.i("LOCATION", "Location is " + location.toString());
                moveCameraTo(location);
                ride = new Ride(location);
            }else{
                Utils.debugLogging(getApplicationContext(), "Location is null...");
            }
            //Begin creating the ride info
            //Get location info
            geocoder = new Geocoder(getApplicationContext());

            //Get passenger count
            promptForPassengerCount();

            if (!isInRide){
                //Remove "End Ride" button
                RelativeLayout layout = (RelativeLayout) findViewById(R.id.container);
                BootstrapButton endButton = (BootstrapButton) findViewById(R.id.end_ride_button);
                layout.removeView(endButton);
            }

        }else{//NO MAP! -- forward to the rating screen
            Toast.makeText(getApplicationContext(), "Fare estimation is not available.", Toast.LENGTH_SHORT);
            if(currentDriver != null){
                Intent forwardToRate = new Intent(this, RateDriver.class);
                startActivity(forwardToRate);
            }else{
                //Set background text to alerting the user that the mMap is not available
                //TODO
            }
        }
     }

    private void promptForPassengerCount() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Passengers");
        alert.setMessage("How many passengers?");
        final EditText passengerCount = new EditText(this);
        passengerCount.setText("1");
        alert.setView(passengerCount);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String text = passengerCount.getText().toString();
                passengers = Integer.parseInt(text);
            }
        });

        alert.show();
    }

    public void onEndRideClicked(View view){
        //Create intent and start rate driver activity
        assert isInRide;
        assert currentDriver != null;
        assert ride != null;

        Intent intent = new Intent(getApplicationContext(), RateDriver.class);
        intent.putExtra("Driver", (android.os.Parcelable) currentDriver);
        intent.putExtra("Ride", ride);
        startActivity(intent);
    }

    public void searchLocations(String query) throws IOException {
        //Remove old results
        Utils.debugLogging(getApplicationContext(), "Searching for " + query);

        //Populate the card list with results (within a lat long of curr location)
        double left = ride.getStartLongitude() - 1;
        double right = ride.getStartLongitude() + 1;
        double top = ride.getStartLatitude() - 1;
        double bottom = ride.getStartLatitude() + 1;


        List<Address> results = geocoder.getFromLocationName(query, 7, bottom, left, top, right);
        Log.i("GEOCODER", "FOUND " + results.size() + " RESULTS");
        Utils.debugLogging(getApplicationContext(), "Found " + results.size() + " results");

        if(results.size() > 0) {
            Location location = mMap.getMyLocation();
            double maxLeft = location.getLongitude(),
                   maxTop = location.getLatitude(),
                   maxRight = location.getLongitude(),
                   maxBottom = location.getLatitude();

            Log.i("LOCATION", location.getLatitude() + ", " + location.getLongitude());

            assert maxBottom < maxTop;

            markers.clear();
            mMap.clear();

            if(CONSTANTS.DEBUG) {
                drawSearchRegion(left, top, right, bottom);
            }

            for (Address result : results) {
                addPin(result);
                Log.i("RESULTS:", result.getLatitude() +", " + result.getLongitude() + "");

                //Find the new viewbox (to move the camera to)
                maxLeft = Math.min(maxLeft, result.getLongitude());
                maxRight = Math.max(maxRight, result.getLongitude());
                maxTop = Math.max(maxTop, result.getLatitude());
                maxBottom = Math.min(maxBottom, result.getLatitude());
            }

            //Don't get a wider view box than a 2x2 (in degrees) window
            Log.d("VIEW BOX", "View box would be " + maxLeft + ", " + maxTop + ", "
            + maxRight + ", " + maxBottom);
            maxLeft = Math.max(maxLeft, left);
            //maxRight = Math.min(maxRight, right);
            //maxTop = Math.min(maxTop, top);
            //maxBottom = Math.max(maxBottom, bottom);

            //If viewbox is not set correctly, move to it
            //TODO
            LatLngBounds viewBox = new LatLngBounds(new LatLng(maxBottom, maxLeft), new LatLng(maxTop, maxRight));
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBox, VIEW_BOX_PADDING));
            Utils.debugLogging(getApplicationContext(), "View Box: " + maxBottom + ", " + maxLeft + "  " + maxTop + ", " + maxRight);
        } else {
            Toast.makeText(getApplicationContext(), "No results found", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawSearchRegion(double left, double top, double right, double bottom){
        PolylineOptions polylineOptions = new PolylineOptions().color(Color.RED).width(10);

        LatLng topLeft = new LatLng(top, left);
        LatLng topRight = new LatLng(top, right);
        LatLng bottomLeft = new LatLng(bottom, left);
        LatLng bottomRight = new LatLng(bottom, right);

        polylineOptions.add(topLeft, topRight, bottomRight, bottomLeft, topLeft);
        mMap.addPolyline(polylineOptions);
        Utils.debugLogging(getApplicationContext(), "Creating Search Region Box");
    }

    private void addPin(Address address){
        LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());
        //Utils.debugLogging(getApplicationContext(), "Adding pin to " + latlng);
        markers.add(this.mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .title(address.getFeatureName())));
    }

    //Location Client info
    @Override
    public void onStart() {
        super.onStart();
    }

    private void moveCameraTo(Location location){
        //Build CameraPosition
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Utils.debugLogging(getApplicationContext(), "Moving camera to " + latLng);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14f);
        GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                Toast.makeText(getBaseContext(), "Animation complete", Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "Animation canceled", Toast.LENGTH_SHORT)
                        .show();
            }
        };

        Log.i("MOVECAMERATO", "Moving camera to " + location);
        mMap.animateCamera(cameraUpdate, callback);
    }

    @Override
    public void onDestroy(){
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fare_estimator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(ride == null){
            ride = new Ride(location);
        }
        //extend the line marking the current path on the mMap
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        moveCameraTo(location);

        //add location to line
        addLocationToRoute(latlng);

        Utils.debugLogging(getApplicationContext(), "Location changed to " + latlng);
    }

    private void addLocationToRoute(LatLng location){
        if(currentRoute == null){
            currentRoute = mMap.addPolyline(new PolylineOptions()
                    .add(location)
                    .color(CURRENT_PATH_COLOR)
                    .width(CURRENT_PATH_WIDTH));

        }else{
            List<LatLng> points = currentRoute.getPoints();
            points.add(location);
            currentRoute.setPoints(points);
        }
    }

    private void createDirectionsPath(List<LatLng> directions){
        if(directionsRoute == null){
            directionsRoute = mMap.addPolyline(new PolylineOptions()
                    .color(DIRECTIONS_PATH_COLOR)
                    .width(DIRECTIONS_PATH_WIDTH));
        }

        directionsRoute.setPoints(directions);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Utils.debugLogging(getApplicationContext(), provider + " now has status " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        //Switch to most accurate provider
        //TODO
        Utils.debugLogging(getApplicationContext(), provider + " has been enabled");

    }

    @Override
    public void onProviderDisabled(String provider) {
        //Switch providers
        //TODO
        Utils.debugLogging(getApplicationContext(), provider + " has been disabled");

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Utils.debugLogging(getApplicationContext(), marker.getTitle() + " has been clicked");
        //Calculate route to this marker
        //Prompt?
        LatLng start = new LatLng(ride.getStartLatitude(), ride.getStartLongitude());
        LatLng end = marker.getPosition();
        new getDirections().execute(start, end);
        return true;
    }

    private void displayEstimate(ArrayList<Double> fares){
        //Display the estimated fares
        //fares.get(n) is the estimate for (n+1) people
        //TODO

        double fare = Math.round(fares.get(passengers-1)*100)/100;

        Toast.makeText(getApplicationContext(),
                passengers + " passengers: $" + fare, Toast.LENGTH_SHORT).show();
        if (ride == null) {
            ride.setEstimateFare(fare);
        }
    }

    private class getDirections extends Utils.directionsCalculator{
        //Get directions and fare estimation from server
        //Send the list of points, the total distance and total time
        //Receive an estimate fare for the trip

        @Override
        protected Void doInBackground(LatLng... params) {
            Log.i("Directions", "From " + params[0] + " to " + params[1]);
            return super.doInBackground(params);
        }

        protected void onPostExecute(Void params){
            //Set the appropriate values with the given protected values
            //double miles = ((double) distanceValue)/1609.34;
            Utils.debugLogging(getApplicationContext(), "distance is " + distanceValue + " miles");
            createDirectionsPath(directions);

            if (fares == null || fares.size() == 0){
                Toast.makeText(getApplicationContext(), "Could not estimate fare.", Toast.LENGTH_SHORT).show();
            } else {
                //If you received a fare estimation, display it
                displayEstimate(fares);
            }
        }
    }
}
