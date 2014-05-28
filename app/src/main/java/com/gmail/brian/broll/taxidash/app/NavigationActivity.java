package com.gmail.brian.broll.taxidash.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

/*
 * Created by Brian Broll on 5/27/14.
 *
 * This is an activity that contains the navigation drawer.
 */
public class NavigationActivity extends Activity {
    protected DrawerLayout drawerLayout;
    protected ListView drawerList;
    protected String[] layers;
    protected FrameLayout content;
    private ActionBarDrawerToggle drawerToggle;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // R.id.drawer_layout should be in every activity with exactly the same id.
        //setContentView(R.layout.activity_taxi__list);
        setContentView(R.layout.base_drawer_layout);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        layers = getResources().getStringArray(R.array.activities);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        content = (FrameLayout) findViewById(R.id.content_frame);

        //Creating the drawer with items..
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, layers));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                //Create intent and go to respective activity
                //TODO don't go unless it is a different class
                //map.drawerClickEvent(pos);
                Log.i("Navigation Activity", "ITEM CLICK DETECTED");
                Intent nextActivity = null;

                switch(pos){
                    case 0: //Nearby Cabs
                        nextActivity = new Intent(getApplicationContext(), NearbyCabList.class);
                        break;
                    case 1: //Cab Companies
                        nextActivity = new Intent(getApplicationContext(), LocalCompanyList.class);
                        break;
                    case 2: //Favorite Drivers
                        nextActivity = new Intent(getApplicationContext(), FavoriteDriverList.class);
                        break;
                    case 3: //Fare Estimation
                        nextActivity = new Intent(getApplicationContext(), FareEstimator.class);
                        break;
                }

                if(nextActivity != null) {
                    startActivity(nextActivity);
                }
            }
        });

        //set the ActionBar app icon to open drawer
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blue)));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        drawerToggle = new ActionBarDrawerToggle((Activity) this, drawerLayout, R.drawable.ic_drawer, 0, 0) {
            public void onDrawerClosed(View view) {
                Log.i("DRAWER", "onDrawerClosed...");
                //getActionBar().setTitle(R.string.app_name);
            }

            public void onDrawerOpened(View drawerView)
            {
                getActionBar().setTitle(R.string.app_name);
                Log.i("DRAWER", "onDrawerOpened...");
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);


        //View header = getLayoutInflater().inflate(R.layout.drawer_list_header, null);
        //drawerList.addHeaderView(header, null, false);
        //View footerView = ((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                //R.layout.drawer_list_footer, null, false);
        //drawerList.addFooterView(footerView);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("DRAWER", "onOptionsItemSelected running...");
        // Handle item selection
        if(item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                Log.i("DRAWER", "Closing drawer..");
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                Log.i("DRAWER", "Opening drawer..");
                drawerLayout.openDrawer(GravityCompat.START);
            }
        }
        /*
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        */
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

}
