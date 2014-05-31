package com.gmail.brian.broll.taxidash.app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.overflowanimation.BaseCardOverlayAnimation;
import it.gmariotti.cardslib.library.view.CardListView;


public class FareEstimator extends NavigationActivity {
    private Driver currentDriver = null;
    private Ride trip = null;
    private GoogleMap map;
    private Geocoder geocoder;
    private Card.OnCardClickListener selectLocation = null;
    private List<Card> searchResults = new ArrayList<Card>();
    private View mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout contentView = (FrameLayout) inflater.inflate(R.layout.activity_fare_estimator, null, false);
        View searchView = inflater.inflate(R.layout.fragment_search, null, false);
        mapView = inflater.inflate(R.layout.fragment_map, null, false);

        contentView.addView(searchView);
        content.addView(contentView, 0);

        /*
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SearchFragment())
                    .commit();
        }
        */

        Intent intent = getIntent();
        if(intent.hasExtra("Driver")) {
            currentDriver = intent.getParcelableExtra("Driver");
        }

        //Get the map
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        Log.i("Map fragment null? ", (((MapFragment) getFragmentManager().findFragmentById(R.id.map) == null)+""));

        if(map != null){//Now we can manipulate the map
            map.setMyLocationEnabled(true);

            //Begin creating the ride info
            trip = new Ride(map.getMyLocation().getLatitude(),
                            map.getMyLocation().getLatitude());

            geocoder = new Geocoder(getApplicationContext());
        }else{//NO MAP! -- forward to the rating screen
            if(currentDriver != null){
                Intent forwardToRate = new Intent(this, RateDriver.class);
                startActivity(forwardToRate);
            }else{
                //Set background text to alerting the user that the map is not available
                //TODO
            }
        }

        selectLocation = new Card.OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                Address endpoint = ((LocationCard) card).getAddress();
                trip.setEndPoint(endpoint.getLatitude(), endpoint.getLongitude());

                //Move the new fragment in
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new MyMapFragment())
                        .commit();
            }
        };

        CardListView list = (CardListView) findViewById(R.id.search_results);
        CardArrayAdapter adapter = new CardArrayAdapter(this, searchResults);
        list.setAdapter(adapter);
    }


    public void locationSelected(View view){

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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MyMapFragment extends Fragment {

        public MyMapFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_map, container, false);
            return rootView;
        }
    }

    public static class SearchFragment extends Fragment {

        public SearchFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_search, container, false);
            return rootView;
        }
    }
}
