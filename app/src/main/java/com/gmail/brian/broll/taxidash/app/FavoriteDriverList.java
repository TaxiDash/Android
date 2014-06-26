package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardArrayMultiChoiceAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;


public class FavoriteDriverList extends NavigationActivity {
    private Menu actionMenu = null;
    private static final int NEW_MENU_ID = Menu.FIRST +1;

    ArrayList<Driver> favoriteDrivers;

    ArrayList<DriverCard> selectedCards = new ArrayList<DriverCard>();
    List<Card> displayedCards = new ArrayList<Card>();

    //Card colors
    ColorDrawable cardColor;
    ColorDrawable selectedColor;

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

    Card.OnLongCardClickListener selectListener = new Card.OnLongCardClickListener() {
        @Override
        public boolean onLongClick(Card card, View view) {
            selectDriverCard((DriverCard) card);
            return true;
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
    String favFileName = CONSTANTS.CURRENT_SERVER.getCity() + CONSTANTS.CURRENT_SERVER.getState() + "-favorites.dat";

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
        Log.i("FAV_DRIVERS_LIST", "resources null? " + (getResources() == null));
        cardColor = new ColorDrawable(getResources().getColor(R.color.cardColor));
        selectedColor = new ColorDrawable(getResources().getColor(R.color.selectedCardColor));

        //Create the card list of favorite drivers
        CardListView list = (CardListView) findViewById(R.id.favorite_driver_list);

        //Create the cards
        for(Driver favoriteDriver : favoriteDrivers){
            addDriver(favoriteDriver);
        }
        Log.i("FAV DRIVER LIST", "1. THERE ARE " + favoriteDrivers.size() + " DRIVERS");

        CardArrayMultiChoiceAdapter adapter;
        adapter = new CardArrayMultiChoiceAdapter(this.getApplicationContext(), displayedCards) {
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.action_call) {
                    Toast.makeText(getContext(), "Share;" , Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (item.getItemId() == R.id.action_delete) {
                    int count = getSelectedCards().size();
                    Toast.makeText(getContext(), "Pressed delete button (" + count + ")" , Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b, CardView cardView, Card card) {
                //updateActionBar(getSelectedCards().size());
            }
        };
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


        //If there are no favorite drivers, display a message
        if(this.favoriteDrivers.size() == 0){
            RelativeLayout main = (RelativeLayout) findViewById(R.id.main_container);
            TextView noDriversMsg = new TextView(getApplicationContext());
            noDriversMsg.setText("You don't have any favorite drivers!");
            noDriversMsg.setTextSize(30);

            RelativeLayout.LayoutParams layoutParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            noDriversMsg.setLayoutParams(layoutParams);
            noDriversMsg.setGravity(Gravity.CENTER);

            main.addView(noDriversMsg);
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
                //actionMenu.getItem(R.id.action_call).setVisible(false);
                //actionMenu.getItem(R.id.action_delete).setVisible(false);
                break;
            case 1:
                //Call option, move up, move down, delete
                //actionMenu.getItem(R.id.action_call).setVisible(true);
                //actionMenu.getItem(R.id.action_delete).setVisible(true);
                break;
            default:
                //delete option
                //actionMenu.getItem(R.id.action_call).setVisible(false);
                //actionMenu.getItem(R.id.action_delete).setVisible(true);
                break;
        }
    }

     @Override
    protected void onDestroy(){
        Utils.saveFavoriteDrivers(this.getApplicationContext(), favoriteDrivers);
        super.onDestroy();
    }

    @Override
    public void onPause(){
        Utils.saveFavoriteDrivers(this.getApplicationContext(), favoriteDrivers);
        super.onPause();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.favorite_driver_list, menu);
        actionMenu = menu;
        updateActionBar();
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_settings:
                //Do stuff
                break;

            case R.id.action_delete:
                for(DriverCard selectedCard : selectedCards){
                    removeDriver(selectedCard);
                }
                break;

            case R.id.action_call:
                callDriver(selectedCards.get(0).getDriver());
                break;

        }
        selectedCards.clear();
        return super.onOptionsItemSelected(item);
    }

    private void addDriver(Driver driver){
        if(!favoriteDrivers.contains(driver)) {
            favoriteDrivers.add(driver);
        }

        DriverCard card = new DriverCard(this.getApplicationContext(), driver);
        card.setBackgroundResource(new ColorDrawable(getResources().getColor(R.color.cardColor)));
        card.setClickListener(clickListener);
        card.setLongClickable(true);
        card.setOnLongClickListener(selectListener);

        card.setSwipeable(false);//TODO swiping should remove driver from list
        card.setOnSwipeListener(swipeListener);
        displayedCards.add(card);
        Utils.saveFavoriteDrivers(this.getApplicationContext(), favoriteDrivers);
    }

    private void removeDriver(DriverCard card){
        Driver driver = card.getDriver();
        displayedCards.remove(card);
        favoriteDrivers.remove(driver);
    };

    private void selectDriverCard(DriverCard driverCard){
        //Add coloring to selected cards
        //driverCard.setBackgroundResource(Draw);
        if(selectedCards.contains(driverCard)) {
            Log.i("SELECTED CARD CHANGE", "DE-SELECTING " + driverCard.getDriver().getName());
            selectedCards.remove(driverCard);
            //Change color
            driverCard.setBackgroundResource(cardColor);
        }else{
            Log.i("SELECTED CARD CHANGE", "SELECTING " + driverCard.getDriver().getName());
            selectedCards.add(driverCard);
            //Change color
            driverCard.setBackgroundResource(selectedColor);
        }
        updateActionBar();
    }

}
