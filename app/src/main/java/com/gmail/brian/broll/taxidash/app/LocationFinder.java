package com.gmail.brian.broll.taxidash.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;


public class LocationFinder extends NavigationActivity implements GoogleMap.OnMarkerClickListener {
    private Driver currentDriver = null;
    private Geocoder geocoder;
    private Ride trip = null;
    private GoogleMap map;
    private Card.OnCardClickListener selectLocation = null;
    private List<Card> searchResults = new ArrayList<Card>();
    private List<Marker> markers = new ArrayList<Marker>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_location_finder, null, false);
        content.addView(contentView, 0);

        //Get driver
        Intent intent = getIntent();
        if(intent.hasExtra("Driver")){
            this.currentDriver = intent.getParcelableExtra("Driver");
        }

        //Initialize geocoder
        if(geocoder == null){
            geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        }

        //Get the map
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setOnMarkerClickListener(this);

        //Create new trip
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        this.trip = new Ride(latitude, longitude);

        //Create card listener
        selectLocation = new Card.OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                Address endpoint = ((LocationCard) card).getAddress();
                trip.setEndPoint(endpoint.getLatitude(), endpoint.getLongitude());

                //Move to the new activity
                Intent mapTrip = new Intent(getApplicationContext(), FareEstimator.class);
                mapTrip.putExtra("Driver", (Parcelable) currentDriver);
                mapTrip.putExtra("Ride", trip);
                startActivity(mapTrip);
            }
        };

        //Add text changed listener
        EditText search = (EditText) findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    searchLocations(s.toString());
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.location_finder, menu);
        return true;
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
        //Search button pressed
    public void searchLocations(String query){
        //Remove old results
        searchResults.clear();
        Log.i("SEARCHING", "FOR " + query);

        //Populate the card list with results (within a lat long of curr location)
        double left = trip.getStartLongitude() - 1;
        double right = trip.getStartLongitude() + 1;
        double top = trip.getStartLatitude() - 1;
        double bottom = trip.getStartLatitude() + 1;

        try {
            List<Address> results = geocoder.getFromLocationName(query, 7, bottom, left, top, right);
            Log.i("GEOCODER", "FOUND " + results.size() + " RESULTS");
            markers.clear();
            for(Address result : results) {
                //addCard(result);
                addPin(result);
            }
            //Set up the card list
            CardListView list = (CardListView) findViewById(R.id.search_results);
            CardArrayAdapter adapter = new CardArrayAdapter(this, searchResults);
            list.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPin(Address addr){
        markers.add(this.map.addMarker(new MarkerOptions()
                .position(new LatLng(addr.getLatitude(), addr.getLongitude()))
                .title(addr.getFeatureName())));

    }

    private void addCard(Address addr){
        //Create the card
        Card card = new LocationCard(getApplicationContext(), addr);

        //Add click listener
        card.setOnClickListener(selectLocation);
        searchResults.add(card);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        LatLng endpoint = marker.getPosition();
        trip.setEndPoint(endpoint.latitude, endpoint.longitude);

        //Move to the new activity
        Intent mapTrip = new Intent(getApplicationContext(), FareEstimator.class);
        mapTrip.putExtra("Driver", (Parcelable) currentDriver);
        mapTrip.putExtra("Ride", trip);
        startActivity(mapTrip);
        return true;
    }
}
