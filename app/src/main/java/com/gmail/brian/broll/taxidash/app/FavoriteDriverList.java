package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;


public class FavoriteDriverList extends NavigationActivity {
    ArrayList<Driver> favoriteDrivers;

    ArrayList<DriverCard> selectedCards = new ArrayList<DriverCard>();
    List<Card> displayedCards = new ArrayList<Card>();

    Card.OnCardClickListener clickListener = new Card.OnCardClickListener() {
        @Override
        public void onClick(Card card, View v) {
            //What should happen when driver panel is pressed
            //selectDriverCard((DriverCard) card);
            Driver driver;
            driver = ((DriverCard) card).getDriver();
            callDriver(driver);
        }
    };

    Card.OnSwipeListener swipeListener = new Card.OnSwipeListener() {
        @Override
        public void onSwipe(Card card) {
            Driver driver;
            driver = ((DriverCard) card).getDriver();
            callDriver(driver);
        }
    };
    String favFileName = CONSTANTS.CITY_NAME + "-favorites.dat";
    /*
     * This activity will retrieve the user's list of favorite
     * drivers for the given city from a saved file. These
     * drivers will be serialized FavoriteDrivers that can be
     * swiped to call from this list.
     *
     * Touching the cards will select them and allow them to
     * be moved up/down in the list, called, or deleted
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_favorite_driver_list, null, false);
        content.addView(contentView, 0);

        favoriteDrivers = Utils.loadFavoriteDrivers(this.getApplicationContext());

        //Create the card list of favorite drivers
        CardListView list = (CardListView) findViewById(R.id.favorite_driver_list);

        //Create the cards
        for(Driver favoriteDriver : favoriteDrivers){
            addDriver(favoriteDriver);
        }
        Log.i("FAV DRIVER LIST", "1. THERE ARE " + favoriteDrivers.size() + " DRIVERS");

        CardArrayAdapter adapter;
        adapter = new CardArrayAdapter(this.getApplicationContext(), displayedCards);
        list.setAdapter(adapter);

        Intent intent = getIntent();

        if (intent.hasExtra("Driver")) {//Create a new favorite driver

            Driver fav = intent.getParcelableExtra("Driver");
            addDriver(fav);
            //Next, copy the driver's image from cache to permanent location

            if(fav.getImage() != null){//Save it to a more permanent location
                Bitmap favImage = fav.getImage();
                File newImage = new File(getFilesDir(), fav.getBeaconId()+".dat");
                try {
                    FileOutputStream out = new FileOutputStream(newImage.getAbsolutePath());
                    favImage.compress(Bitmap.CompressFormat.PNG, 90, out);
                    fav.setImage(newImage.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            Log.i("FAV DRIVER LIST", "THERE ARE " + favoriteDrivers.size() + " DRIVERS");
        }


    }

    private void callDriver(Driver driver){
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+driver.getPhoneNumber()));
        startActivity(callIntent);
    }

    private void updateActionBar(){
        switch(selectedCards.size()){
            case 0:
                //Regular icons
                //TODO
                break;
            case 1:
                //Call option, move up, move down, delete
                //TODO
                break;
            default:
                //delete option
                //TODO
                break;
        }
    }

     @Override
    protected void onDestroy(){
        saveFavoriteDrivers();
        super.onDestroy();
    }

    @Override
    public void onPause(){
        saveFavoriteDrivers();
        super.onPause();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.favorite_driver_list, menu);
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

    private void saveFavoriteDrivers() {
        //Write the favorite drivers array to file
        File favoriteDriverFile = new File(getFilesDir(), favFileName);
        try {

            Log.i("Favorite Driver List", "About to save drivers to " + favoriteDriverFile.getPath());
            FileOutputStream fileOutputStream = new FileOutputStream(favoriteDriverFile.getPath());
            ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
            out.writeObject(favoriteDrivers);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDriver(Driver driver){
        if(!favoriteDrivers.contains(driver)) {
            favoriteDrivers.add(driver);
        }

        DriverCard card = new DriverCard(this.getApplicationContext(), driver);
        card.setBackgroundResource(new ColorDrawable(getResources().getColor(R.color.cardColor)));
        card.setClickListener(clickListener);
        card.setSwipeable(false);//TODO swiping should remove driver from list
        card.setOnSwipeListener(swipeListener);
        displayedCards.add(card);
        saveFavoriteDrivers();
    }

    private void selectDriverCard(DriverCard driverCard){
        //Add coloring to selected cards
        //driverCard.setBackgroundResource(Draw);
        if(selectedCards.contains(driverCard)) {
            Log.i("SELECTED CARD CHANGE", "DE-SELECTING " + driverCard.getDriver().getName());
            selectedCards.remove(driverCard);
            //Change color
            //TODO
        }else{
            Log.i("SELECTED CARD CHANGE", "SELECTING " + driverCard.getDriver().getName());
            selectedCards.add(driverCard);
            //Change color
            //TODO
        }
        updateActionBar();
    }

}
